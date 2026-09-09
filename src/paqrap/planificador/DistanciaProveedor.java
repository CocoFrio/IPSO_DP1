package paqrap.planificador;

import paqrap.model.PuntoMapa;

/**
 * Abstrae el cálculo de distancia real entre dos puntos del mapa. Se separa
 * de IPSOOptimizador para poder sustituir la implementación por un cálculo
 * de ruta más preciso (Dijkstra/A* sobre el grafo real del componente
 * visualizador) sin tocar el algoritmo metaheurístico.
 */
public interface DistanciaProveedor {
    double distanciaKm(PuntoMapa origen, PuntoMapa destino);
}
