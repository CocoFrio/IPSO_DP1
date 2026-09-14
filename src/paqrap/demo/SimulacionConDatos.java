package paqrap.demo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import paqrap.io.LectorArchivos;
import paqrap.model.Almacen;
import paqrap.model.AlmacenCentral;
import paqrap.model.AlmacenIntermedio;
import paqrap.model.Bloqueo;
import paqrap.model.MantenimientoPreventivo;
import paqrap.model.Pedido;
import paqrap.model.Ruta;
import paqrap.model.TipoVehiculo;
import paqrap.model.Vehiculo;
import paqrap.planificador.PlanificadorIPSO;
import paqrap.planificador.ipso.IPSOConfig;

/**
 * Simula el avance del reloj y libera los datos cuando llega su instante.
 *
 * <p>Uso:
 * java -cp out paqrap.demo.SimulacionConDatos anio mes dia hora horas</p>
 */
public final class SimulacionConDatos {

    private static final int PASO_MINUTOS = 15;

    private SimulacionConDatos() {
    }

    public static void main(String[] args) throws Exception {
        int anio = args.length > 0 ? Integer.parseInt(args[0]) : 2026;
        int mes = args.length > 1 ? Integer.parseInt(args[1]) : 9;
        int dia = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int hora = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int horasSimulacion = args.length > 4 ? Integer.parseInt(args[4]) : 24;
        LocalDateTime inicio = LocalDateTime.of(anio, mes, dia, hora, 0);
        LocalDateTime fin = inicio.plusHours(horasSimulacion);

        String mm = String.format("%02d", mes);
        String aamm = String.format("%02d%02d", anio % 100, mes);
        Path data = Paths.get("data");
        List<Pedido> pedidos = new ArrayList<>(LectorArchivos.leerVentas(
                data.resolve("ventas").resolve("ventas." + anio + mm + ".txt"), anio, mes));
        List<Bloqueo> bloqueos = new ArrayList<>(LectorArchivos.leerBloqueos(
                data.resolve("bloqueos").resolve("bloqueo." + aamm + ".txt"), anio, mes));
        List<MantenimientoPreventivo> mantenimientos = LectorArchivos.leerMantenimientos(
                data.resolve("mant.preventivo.09.10.txt"));

        pedidos.sort(Comparator.comparing(Pedido::getFechaIngreso));
        bloqueos.sort(Comparator.comparing(Bloqueo::getFechaInicio));
        PlanificadorIPSO planificador = crearPlanificador(inicio, mantenimientos);
        int siguientePedido = primerPedidoDesde(pedidos, inicio);
        int siguienteBloqueo = primerBloqueoDesde(bloqueos, inicio);
        int pedidosHistoricos = siguientePedido;
        int bloqueosHistoricos = siguienteBloqueo;

        System.out.println("=== Simulacion temporal ===");
        System.out.println("Inicio: " + inicio);
        System.out.println("Fin: " + fin);
        System.out.println("Paso: " + PASO_MINUTOS + " minutos");
        System.out.println("Pedidos historicos ignorados: " + pedidosHistoricos);
        System.out.println("Bloqueos historicos ignorados: " + bloqueosHistoricos);

        LocalDateTime instante = inicio;
        for (; !instante.isAfter(fin);
                instante = instante.plusMinutes(PASO_MINUTOS)) {
            planificador.avanzarTiempo(instante);
            List<Pedido> pedidosLlegados = new ArrayList<>();
            int bloqueosNuevos = 0;

            while (siguientePedido < pedidos.size()
                    && !pedidos.get(siguientePedido).getFechaIngreso().isAfter(instante)) {
                Pedido pedido = pedidos.get(siguientePedido++);
                planificador.registrarPedido(pedido);
                pedidosLlegados.add(pedido);
            }
            while (siguienteBloqueo < bloqueos.size()
                    && !bloqueos.get(siguienteBloqueo).getFechaInicio().isAfter(instante)) {
                planificador.registrarBloqueo(bloqueos.get(siguienteBloqueo++));
                bloqueosNuevos++;
            }

            if (!pedidosLlegados.isEmpty()) {
                List<Ruta> rutas = planificador.planificarRutas();
                System.out.printf("%n[%s] llegaron %d pedidos, %d bloqueos; rutas nuevas: %d%n",
                        instante, pedidosLlegados.size(), bloqueosNuevos, rutas.size());
                imprimirPedidosLlegados(pedidosLlegados);
                marcarEnRuta(rutas);
                imprimirRutas(rutas);
                planificador.imprimirPedidosPendientes();
                imprimirPrioridad(planificador.getPedidosPendientes(), instante);
            } else if (bloqueosNuevos > 0) {
                List<Ruta> rutas = planificador.replanificar();
                System.out.printf("%n[%s] llegaron %d bloqueos; rutas replanificadas: %d%n",
                        instante, bloqueosNuevos, rutas.size());
                marcarEnRuta(rutas);
                imprimirRutas(rutas);
                planificador.imprimirPedidosPendientes();
            }

            Pedido pedidoVencido = primerPedidoVencido(pedidos, inicio, instante);
            if (pedidoVencido != null) {
                long minutosRetraso = Duration.between(
                        pedidoVencido.calcularFechaLimite(), instante).toMinutes();
                System.out.printf("%n=== COLAPSO DE LA OPERACION ===%n"
                                + "Pedido vencido: %s%n"
                                + "Fecha limite: %s%n"
                                + "Instante detectado: %s%n"
                                + "Retraso: %dh %02dm%n",
                        pedidoVencido.getIdPedido(),
                        pedidoVencido.calcularFechaLimite(),
                        instante,
                        minutosRetraso / 60,
                        minutosRetraso % 60);
                break;
            }
        }

        System.out.println("\n=== Resumen ===");
        System.out.println("Fin real de la simulacion: " + instante);
        System.out.println("Pedidos posteriores aun no incorporados: "
                + (pedidos.size() - siguientePedido));
        System.out.println("Pedidos pendientes de planificacion: "
                + planificador.getPedidosPendientes().size());
    }

    private static int primerPedidoDesde(List<Pedido> pedidos, LocalDateTime inicio) {
        int indice = 0;
        while (indice < pedidos.size()
                && pedidos.get(indice).getFechaIngreso().isBefore(inicio)) {
            indice++;
        }
        return indice;
    }

    private static int primerBloqueoDesde(List<Bloqueo> bloqueos, LocalDateTime inicio) {
        int indice = 0;
        while (indice < bloqueos.size()
                && bloqueos.get(indice).getFechaInicio().isBefore(inicio)) {
            indice++;
        }
        return indice;
    }

    private static Pedido primerPedidoVencido(List<Pedido> pedidos,
                                               LocalDateTime inicio,
                                               LocalDateTime instante) {
        return pedidos.stream()
                .filter(pedido -> !pedido.getFechaIngreso().isBefore(inicio))
                .filter(pedido -> !Pedido.ESTADO_ENTREGADO.equals(pedido.getEstado()))
                .filter(pedido -> pedido.calcularFechaLimite().isBefore(instante))
                .min(Comparator.comparing(Pedido::calcularFechaLimite))
                .orElse(null);
    }

    private static PlanificadorIPSO crearPlanificador(
            LocalDateTime inicio, List<MantenimientoPreventivo> mantenimientos) {
        List<Vehiculo> flota = new ArrayList<>();
        agregarVehiculos(flota, "TA", TipoVehiculo.AUTO, 10);
        agregarVehiculos(flota, "TB", TipoVehiculo.BICICLETA, 12);
        agregarVehiculos(flota, "TM", TipoVehiculo.MOTO, 15);
        List<Almacen> almacenes = new ArrayList<>();
        almacenes.add(new AlmacenCentral("ALM-CENTRAL", 27, 14));
        almacenes.add(new AlmacenIntermedio("ALM-NOR-OESTE", 12, 38));
        almacenes.add(new AlmacenIntermedio("ALM-ESTE", 57, 27));
        PlanificadorIPSO planificador = new PlanificadorIPSO(
                flota, almacenes, new IPSOConfig()
                        .setTamanoPoblacionN(10)
                        .setIteracionesMaximasT(30)
                        .setSemillaAleatoria(42L),
                inicio);
        for (MantenimientoPreventivo mantenimiento : mantenimientos) {
            planificador.registrarMantenimiento(mantenimiento);
        }
        return planificador;
    }

    private static void agregarVehiculos(List<Vehiculo> flota, String prefijo,
                                         TipoVehiculo tipo, int cantidad) {
        for (int numero = 1; numero <= cantidad; numero++) {
            flota.add(new Vehiculo(prefijo + String.format("%02d", numero), tipo, 27, 14));
        }
    }

    private static void imprimirPrioridad(List<Pedido> pendientes, LocalDateTime instante) {
        pendientes.stream()
                .sorted(Comparator.comparing(Pedido::calcularFechaLimite))
                .limit(5)
                .forEach(pedido -> {
                    long minutos = Duration.between(instante, pedido.calcularFechaLimite()).toMinutes();
                    System.out.printf("  prioridad: %s, tiempo restante: %dh %02dm%n",
                            pedido.getIdPedido(), minutos / 60, Math.abs(minutos % 60));
                });
    }

    private static void imprimirPedidosLlegados(List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            System.out.printf("  llego: %s | cliente: %s | posicion: (%d,%d)"
                            + " | limite: %s | estado: %s%n",
                    pedido.getIdPedido(),
                    pedido.getIdCliente(),
                    pedido.getPosX(),
                    pedido.getPosY(),
                    pedido.calcularFechaLimite(),
                    pedido.getEstado());
        }
    }

    private static void marcarEnRuta(List<Ruta> rutas) {
        for (Ruta ruta : rutas) {
            ruta.setEstado(Ruta.ESTADO_EN_EJECUCION);
            for (Pedido pedido : ruta.getSecuenciaEntrega()) {
                pedido.setEstado(Pedido.ESTADO_EN_RUTA);
            }
        }
    }

    private static void imprimirRutas(List<Ruta> rutas) {
        for (Ruta ruta : rutas) {
            System.out.printf("  Ruta %s | vehiculo: %s (%s) | estado: %s | pedidos: ",
                    ruta.getIdRuta(),
                    ruta.getVehiculoAsignado().getIdVehiculo(),
                    ruta.getVehiculoAsignado().getTipo(),
                    ruta.getEstado());
            for (int i = 0; i < ruta.getSecuenciaEntrega().size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                Pedido pedido = ruta.getSecuenciaEntrega().get(i);
                System.out.print(pedido.getIdPedido() + " [" + pedido.getEstado() + "]");
            }
            System.out.println();
        }
    }

}
