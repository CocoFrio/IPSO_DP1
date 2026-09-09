package paqrap.planificador;

import java.time.LocalDateTime;
import java.util.List;
import paqrap.model.Bloqueo;
import paqrap.model.PuntoMapa;

/**
 * Distancia sobre una malla vial tipo grid (calles de doble sentido, RNF "c"):
 * usa distancia Manhattan como aproximación de recorrido real por calles, y
 * añade una penalización cuando el tramo es interferido por un Bloqueo
 * vigente, simulando el costo del desvío. El componente visualizador puede
 * reemplazar esta clase por el cálculo exacto de ruta más corta sobre el
 * grafo real (este fragmento del dominio no incluye el grafo de calles).
 */
public class DistanciaGridConBloqueos implements DistanciaProveedor {

    private static final double PENALIZACION_KM_POR_BLOQUEO = 6.0;

    private final List<Bloqueo> bloqueosActivos;
    private final LocalDateTime instanteReferencia;

    public DistanciaGridConBloqueos(List<Bloqueo> bloqueosActivos, LocalDateTime instanteReferencia) {
        this.bloqueosActivos = bloqueosActivos;
        this.instanteReferencia = instanteReferencia;
    }

    @Override
    public double distanciaKm(PuntoMapa origen, PuntoMapa destino) {
        double base = Math.abs(origen.getPosX() - destino.getPosX())
                + Math.abs(origen.getPosY() - destino.getPosY());
        double penalizacion = 0.0;
        for (Bloqueo b : bloqueosActivos) {
            if (b.estaVigente(instanteReferencia) && b.interfiereCon(origen, destino)) {
                penalizacion += PENALIZACION_KM_POR_BLOQUEO;
            }
        }
        return base + penalizacion;
    }
}
