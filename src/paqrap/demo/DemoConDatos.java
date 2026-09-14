package paqrap.demo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * Ejecuta IPSO usando los archivos de la carpeta data.
 *
 * <p>Uso: java -cp out paqrap.demo.DemoConDatos [anio] [mes] [dia] [hora]</p>
 */
public final class DemoConDatos {

    private DemoConDatos() {
    }

    public static void main(String[] args) throws Exception {
        int anio = args.length > 0 ? Integer.parseInt(args[0]) : 2026;
        int mes = args.length > 1 ? Integer.parseInt(args[1]) : 9;
        int dia = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int hora = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        String mm = String.format("%02d", mes);
        String aamm = String.format("%02d%02d", anio % 100, mes);
        Path data = Paths.get("data");

        List<Pedido> pedidos = LectorArchivos.leerVentas(
                data.resolve("ventas").resolve("ventas." + anio + mm + ".txt"), anio, mes);
        List<Bloqueo> bloqueos = LectorArchivos.leerBloqueos(
                data.resolve("bloqueos").resolve("bloqueo." + aamm + ".txt"), anio, mes);
        List<MantenimientoPreventivo> mantenimientos = LectorArchivos.leerMantenimientos(
                data.resolve("mant.preventivo.09.10.txt"));

        List<Vehiculo> flota = crearFlota(mantenimientos);
        List<Almacen> almacenes = new ArrayList<>();
        almacenes.add(new AlmacenCentral("ALM-CENTRAL", 27, 14));
        almacenes.add(new AlmacenIntermedio("ALM-NOR-OESTE", 12, 38));
        almacenes.add(new AlmacenIntermedio("ALM-ESTE", 57, 27));

        LocalDateTime ahora = LocalDateTime.of(anio, mes, dia, hora, 0);
        IPSOConfig config = new IPSOConfig()
                .setTamanoPoblacionN(10)
                .setIteracionesMaximasT(30)
                .setSemillaAleatoria(42L);
        PlanificadorIPSO planificador = new PlanificadorIPSO(flota, almacenes, config, ahora);

        for (Pedido pedido : pedidos) {
            planificador.registrarPedido(pedido);
        }
        for (Bloqueo bloqueo : bloqueos) {
            planificador.registrarBloqueo(bloqueo);
        }
        for (MantenimientoPreventivo mantenimiento : mantenimientos) {
            planificador.registrarMantenimiento(mantenimiento);
        }

        System.out.println("=== Datos cargados ===");
        System.out.println("Pedidos: " + pedidos.size());
        System.out.println("Bloqueos: " + bloqueos.size());
        System.out.println("Mantenimientos: " + mantenimientos.size());
        System.out.println("Vehiculos: " + flota.size());
        System.out.println("Inicio de planificacion: " + ahora);

        List<Ruta> rutas = planificador.planificarRutas();
        System.out.println("\n=== Resultado IPSO ===");
        System.out.println("Rutas generadas: " + rutas.size());
        System.out.println("Pedidos pendientes: " + planificador.getPedidosPendientes().size());
        for (Ruta ruta : rutas) {
            System.out.println(ruta);
        }
    }

    private static List<Vehiculo> crearFlota(List<MantenimientoPreventivo> mantenimientos) {
        List<Vehiculo> flota = new ArrayList<>();
        agregarVehiculos(flota, "TA", TipoVehiculo.AUTO, 10);
        agregarVehiculos(flota, "TB", TipoVehiculo.BICICLETA, 12);
        agregarVehiculos(flota, "TM", TipoVehiculo.MOTO, 15);
        return flota;
    }

    private static void agregarVehiculos(List<Vehiculo> flota, String prefijo,
                                         TipoVehiculo tipo, int cantidad) {
        for (int numero = 1; numero <= cantidad; numero++) {
            flota.add(new Vehiculo(prefijo + String.format("%02d", numero), tipo, 27, 14));
        }
    }
}
