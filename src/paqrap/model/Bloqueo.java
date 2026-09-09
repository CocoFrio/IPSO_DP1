package paqrap.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bloqueo — clase del diagrama de dominio.
 * Atributos idénticos al diagrama: secuenciaNodos, fechaInicio, fechaFin.
 * Relación "afecta" Ruta / "0..1 originada por" -> puede disparar una
 * Reasignacion.
 */
public class Bloqueo {

    private final String idBloqueo;
    private final List<Nodo> secuenciaNodos;
    private final LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    public Bloqueo(String idBloqueo, List<Nodo> secuenciaNodos, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this.idBloqueo = idBloqueo;
        this.secuenciaNodos = secuenciaNodos;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public String getIdBloqueo() {
        return idBloqueo;
    }

    public List<Nodo> getSecuenciaNodos() {
        return secuenciaNodos;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public boolean estaVigente(LocalDateTime instante) {
        return !instante.isBefore(fechaInicio) && (fechaFin == null || !instante.isAfter(fechaFin));
    }

    /**
     * Proxy simplificado de interferencia: un bloqueo interfiere con el
     * tramo origen-destino si alguno de sus nodos cae dentro del rectángulo
     * (bounding box) que forma dicho tramo en la malla vial. El
     * componente visualizador (grafo real de calles) puede reemplazar este
     * método por un chequeo exacto de intersección de aristas.
     */
    public boolean interfiereCon(PuntoMapa origen, PuntoMapa destino) {
        int minX = Math.min(origen.getPosX(), destino.getPosX());
        int maxX = Math.max(origen.getPosX(), destino.getPosX());
        int minY = Math.min(origen.getPosY(), destino.getPosY());
        int maxY = Math.max(origen.getPosY(), destino.getPosY());
        for (Nodo n : secuenciaNodos) {
            if (n.getPosX() >= minX && n.getPosX() <= maxX
                    && n.getPosY() >= minY && n.getPosY() <= maxY) {
                return true;
            }
        }
        return false;
    }
}
