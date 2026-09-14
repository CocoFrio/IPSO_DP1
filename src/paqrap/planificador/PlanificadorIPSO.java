package paqrap.planificador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import paqrap.model.Almacen;
import paqrap.model.Bloqueo;
import paqrap.model.MantenimientoPreventivo;
import paqrap.model.Pedido;
import paqrap.model.PuntoMapa;
import paqrap.model.Reasignacion;
import paqrap.model.Ruta;
import paqrap.model.Vehiculo;
import paqrap.planificador.ipso.IPSOConfig;
import paqrap.planificador.ipso.IPSOOptimizador;
import paqrap.planificador.ipso.ResultadoIPSO;

/**
 * Implementación de Planificador basada en IPSO. Mantiene el estado de la
 * operación (pedidos pendientes, flota, almacenes, bloqueos y rutas
 * activas) y expone las dos operaciones del diagrama de dominio:
 *
 *  - planificarRutas(): agrupa pedidos pendientes en lotes por vehículo
 *    (ClusterizadorPedidos) y ejecuta IPSO para secuenciar cada lote.
 *  - replanificar(): ante un Bloqueo vigente o una avería de vehículo,
 *    libera los pedidos aún no entregados de las rutas afectadas, registra
 *    la Reasignacion correspondiente y vuelve a planificar priorizando al
 *    cliente con el plazo más crítico, tal como exige la situación
 *    auténtica.
 */
public class PlanificadorIPSO implements Planificador {

    private final List<Pedido> pedidosPendientes = new ArrayList<>();
    private final List<Vehiculo> flota;
    private final List<Almacen> almacenes;
    private final List<Bloqueo> bloqueosActivos = new ArrayList<>();
    private final List<MantenimientoPreventivo> mantenimientos = new ArrayList<>();
    private final List<Ruta> rutasActivas = new ArrayList<>();
    private final List<Reasignacion> historialReasignaciones = new ArrayList<>();
    private final IPSOConfig config;
    private LocalDateTime instanteActual;
    private int contadorRutas = 0;

    public PlanificadorIPSO(List<Vehiculo> flota, List<Almacen> almacenes,
                             IPSOConfig config, LocalDateTime instanteInicial) {
        this.flota = flota;
        this.almacenes = almacenes;
        this.config = config;
        this.instanteActual = instanteInicial;
    }

    public void registrarPedido(Pedido pedido) {
        pedidosPendientes.add(pedido);
    }

    public void registrarBloqueo(Bloqueo bloqueo) {
        bloqueosActivos.add(bloqueo);
    }

    public void registrarMantenimiento(MantenimientoPreventivo mantenimiento) {
        mantenimientos.add(mantenimiento);
        actualizarDisponibilidadPorMantenimiento();
    }

    public void avanzarTiempo(LocalDateTime nuevoInstante) {
        this.instanteActual = nuevoInstante;
        actualizarDisponibilidadPorMantenimiento();
    }

    @Override
    public List<Ruta> planificarRutas() {
        List<Ruta> nuevasRutas = new ArrayList<>();
        if (pedidosPendientes.isEmpty()) {
            return nuevasRutas;
        }

        ClusterizadorPedidos clusterizador = new ClusterizadorPedidos();
        List<ClusterizadorPedidos.LoteVehiculo> lotes = clusterizador.clusterizar(
                pedidosPendientes, flota, almacenes, instanteActual);

        DistanciaProveedor distancia = new DistanciaGridConBloqueos(bloqueosActivos, instanteActual);

        for (ClusterizadorPedidos.LoteVehiculo lote : lotes) {
            IPSOOptimizador ipso = new IPSOOptimizador(
                    lote.pedidos, lote.almacen, lote.vehiculo, distancia, config, instanteActual);
            ResultadoIPSO resultado = ipso.ejecutar();

            Ruta ruta = new Ruta("R-" + (++contadorRutas), lote.vehiculo, lote.almacen);
            ruta.setSecuenciaEntrega(resultado.getSecuenciaOptima());
            ruta.setCostoEstimado((float) resultado.getCostoTotal());
            ruta.setDuracionEstimada((float) resultado.getDuracionTotalHoras());
            ruta.setEstado(resultado.isCumplePlazos()
                    ? Ruta.ESTADO_PLANIFICADA
                    : "PLANIFICADA_RIESGO_DE_INCUMPLIMIENTO");

            for (Pedido p : resultado.getSecuenciaOptima()) {
                p.setEstado(Pedido.ESTADO_ASIGNADO);
                pedidosPendientes.remove(p);
            }

            rutasActivas.add(ruta);
            nuevasRutas.add(ruta);
        }

        return nuevasRutas;
    }

    @Override
    public List<Ruta> replanificar() {
        List<Pedido> pedidosLiberados = new ArrayList<>();
        List<Ruta> rutasAfectadas = new ArrayList<>();

        for (Ruta ruta : rutasActivas) {
            if (Ruta.ESTADO_FINALIZADA.equals(ruta.getEstado())) {
                continue;
            }
            Bloqueo bloqueoCausante = detectarBloqueoQueAfecta(ruta);
            boolean averiada = !ruta.getVehiculoAsignado().isDisponible();

            if (bloqueoCausante == null && !averiada) {
                continue;
            }

            List<Pedido> pendientesDeLaRuta = new ArrayList<>();
            for (Pedido p : ruta.getSecuenciaEntrega()) {
                if (!Pedido.ESTADO_ENTREGADO.equals(p.getEstado())) {
                    pendientesDeLaRuta.add(p);
                }
            }
            if (pendientesDeLaRuta.isEmpty()) {
                continue;
            }

            // Se prioriza primero al cliente más crítico (plazo más próximo),
            // tal como exige la situación auténtica ante bloqueos/averías.
            pendientesDeLaRuta.sort(Comparator.comparing(Pedido::calcularFechaLimite));

            String motivo = averiada
                    ? "Avería de vehículo " + ruta.getVehiculoAsignado().getIdVehiculo()
                    : "Bloqueo de calle " + bloqueoCausante.getIdBloqueo();

            for (Pedido p : pendientesDeLaRuta) {
                p.setEstado(Pedido.ESTADO_REASIGNADO);
                historialReasignaciones.add(
                        new Reasignacion(instanteActual, motivo, p, ruta, null, bloqueoCausante));
            }

            pedidosLiberados.addAll(pendientesDeLaRuta);
            ruta.setEstado(Ruta.ESTADO_REPLANIFICADA);
            rutasAfectadas.add(ruta);
        }

        rutasActivas.removeAll(rutasAfectadas);
        // los pedidos liberados entran con máxima prioridad a la siguiente corrida
        pedidosPendientes.addAll(0, pedidosLiberados);

        return planificarRutas();
    }

    private Bloqueo detectarBloqueoQueAfecta(Ruta ruta) {
        PuntoMapa anterior = ruta.getAlmacenOrigen();
        for (Pedido p : ruta.getSecuenciaEntrega()) {
            for (Bloqueo b : bloqueosActivos) {
                if (b.estaVigente(instanteActual) && b.interfiereCon(anterior, p)) {
                    return b;
                }
            }
            anterior = p;
        }
        return null;
    }

    public List<Reasignacion> getHistorialReasignaciones() {
        return historialReasignaciones;
    }

    public List<Ruta> getRutasActivas() {
        return rutasActivas;
    }

    public List<Pedido> getPedidosPendientes() {
        return pedidosPendientes;
    }

    public void imprimirPedidosPendientes() {
        if (pedidosPendientes.isEmpty()) {
            System.out.println("Pedidos pendientes: ninguno");
            return;
        }
        System.out.println("Pedidos pendientes:");
        pedidosPendientes.stream()
                .sorted(Comparator.comparing(Pedido::calcularFechaLimite))
                .forEach(pedido -> System.out.println("  " + pedido.getIdPedido()
                        + " | limite: " + pedido.calcularFechaLimite()
                        + " | estado: " + pedido.getEstado()));
    }

    private void actualizarDisponibilidadPorMantenimiento() {
        for (Vehiculo vehiculo : flota) {
            boolean enMantenimiento = mantenimientos.stream()
                    .anyMatch(mantenimiento ->
                            mantenimiento.getFecha().equals(instanteActual.toLocalDate())
                                    && mantenimiento.getIdVehiculo()
                                            .equalsIgnoreCase(vehiculo.getIdVehiculo()));
            if (enMantenimiento) {
                vehiculo.setDisponible(false);
            }
        }
    }
}
