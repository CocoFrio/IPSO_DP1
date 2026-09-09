package paqrap.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import paqrap.model.Almacen;
import paqrap.model.AlmacenCentral;
import paqrap.model.AlmacenIntermedio;
import paqrap.model.Bloqueo;
import paqrap.model.ConfiguracionSemaforo;
import paqrap.model.Nodo;
import paqrap.model.Pedido;
import paqrap.model.Reasignacion;
import paqrap.model.Ruta;
import paqrap.model.SemaforoColor;
import paqrap.model.TipoVehiculo;
import paqrap.model.Vehiculo;
import paqrap.planificador.PlanificadorIPSO;
import paqrap.planificador.ipso.IPSOConfig;

/**
 * Demostración ejecutable: crea un escenario pequeño con los 3 almacenes,
 * una flota mixta y un conjunto de pedidos con distinta urgencia, ejecuta
 * planificarRutas() usando IPSO, simula un bloqueo de calle y ejecuta
 * replanificar() para verificar la reasignación priorizando al cliente
 * más crítico.
 */
public class DemoPaqRap {

    public static void main(String[] args) {
        LocalDateTime ahora = LocalDateTime.of(2026, 9, 7, 8, 0);

        // --- Almacenes: 1 central + 2 intermedios (situación auténtica) ---
        List<Almacen> almacenes = new ArrayList<>(Arrays.asList(
                new AlmacenCentral("ALM-CENTRAL", 0, 0),
                new AlmacenIntermedio("ALM-INT-1", 10, 2),
                new AlmacenIntermedio("ALM-INT-2", 2, 12)
        ));

        // --- Flota mixta: autos, motos, bicicletas ---
        List<Vehiculo> flota = new ArrayList<>(Arrays.asList(
                new Vehiculo("AUTO-1", TipoVehiculo.AUTO, 0, 0),
                new Vehiculo("MOTO-1", TipoVehiculo.MOTO, 10, 2),
                new Vehiculo("BICI-1", TipoVehiculo.BICICLETA, 2, 12)
        ));

        // --- Configuración del IPSO (calibración empírica, ver informe) ---
        IPSOConfig config = new IPSOConfig()
                .setTamanoPoblacionN(30)
                .setIteracionesMaximasT(150)
                .setC1(1.5)
                .setC2(1.5)
                .setTasaCruceP_c(0.3)
                .setSemillaAleatoria(42L);

        PlanificadorIPSO planificador = new PlanificadorIPSO(flota, almacenes, config, ahora);

        // --- Pedidos: mezcla de plazos normales (36h) y priorizados (4/8/12/18h) ---
        planificador.registrarPedido(new Pedido("PED-01", "CLI-01", 3, 1, 5, 8, ahora));
        planificador.registrarPedido(new Pedido("PED-02", "CLI-02", 6, 4, 3, 36, ahora));
        planificador.registrarPedido(new Pedido("PED-03", "CLI-03", 1, 5, 4, 4, ahora));
        planificador.registrarPedido(new Pedido("PED-04", "CLI-04", 9, 3, 6, 18, ahora));
        planificador.registrarPedido(new Pedido("PED-05", "CLI-05", 8, 1, 2, 12, ahora));
        planificador.registrarPedido(new Pedido("PED-06", "CLI-06", 2, 10, 3, 36, ahora));
        planificador.registrarPedido(new Pedido("PED-07", "CLI-07", 4, 13, 2, 8, ahora));
        planificador.registrarPedido(new Pedido("PED-08", "CLI-08", 11, 4, 4, 36, ahora));

        System.out.println("=== Planificación inicial (IPSO) ===");
        List<Ruta> rutas = planificador.planificarRutas();
        imprimirRutas(rutas);
        imprimirSemaforo(rutas, ahora);

        // --- Simulación de un bloqueo de calle que afecta una ruta activa ---
        Bloqueo bloqueo = new Bloqueo(
                "BLQ-01",
                Arrays.asList(new Nodo(5, 2), new Nodo(6, 3)),
                ahora.plusMinutes(5),
                ahora.plusHours(2));
        planificador.registrarBloqueo(bloqueo);
        planificador.avanzarTiempo(ahora.plusMinutes(10));

        System.out.println("\n=== Replanificación tras Bloqueo BLQ-01 ===");
        List<Ruta> rutasReplanificadas = planificador.replanificar();
        imprimirRutas(rutasReplanificadas);

        System.out.println("\n=== Historial de reasignaciones ===");
        for (Reasignacion r : planificador.getHistorialReasignaciones()) {
            System.out.println(" - " + r);
        }
    }

    private static void imprimirRutas(List<Ruta> rutas) {
        if (rutas.isEmpty()) {
            System.out.println("(sin rutas nuevas)");
            return;
        }
        for (Ruta r : rutas) {
            System.out.println(r);
        }
    }

    private static void imprimirSemaforo(List<Ruta> rutas, LocalDateTime instante) {
        ConfiguracionSemaforo semaforo = new ConfiguracionSemaforo(0.60, 0.85);
        System.out.println("\n--- Semáforo de urgencia de los pedidos asignados ---");
        for (Ruta r : rutas) {
            for (Pedido p : r.getSecuenciaEntrega()) {
                double fraccion = p.fraccionPlazoConsumido(instante);
                SemaforoColor color = semaforo.evaluar(fraccion);
                System.out.printf("  %s -> %s (%.0f%% del plazo consumido)%n",
                        p.getIdPedido(), color, fraccion * 100);
            }
        }
    }
}
