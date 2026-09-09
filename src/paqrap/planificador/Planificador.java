package paqrap.planificador;

import java.util.List;
import paqrap.model.Ruta;

/**
 * Planificador — interfaz del diagrama de dominio.
 * Firmas idénticas al diagrama: planificarRutas(): List, replanificar(): List.
 * La implementación (PlanificadorIPSO) mantiene su propio estado interno
 * (pedidos pendientes, flota, almacenes, bloqueos activos) inyectado antes
 * de invocar estos métodos, de modo que las firmas se mantienen sin
 * parámetros tal como en el diagrama.
 */
public interface Planificador {

    List<Ruta> planificarRutas();

    List<Ruta> replanificar();
}
