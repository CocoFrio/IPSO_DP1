package paqrap.model;

/**
 * Abstracción común de cualquier elemento ubicable en la malla vial (grid)
 * usada por el componente visualizador. Permite que el planificador calcule
 * distancias entre Pedido, Almacen y nodos de Bloqueo de manera uniforme.
 */
public interface PuntoMapa {
    int getPosX();
    int getPosY();
}
