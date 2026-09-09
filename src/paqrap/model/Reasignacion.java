package paqrap.model;

import java.time.LocalDateTime;

/**
 * Reasignacion — clase del diagrama de dominio.
 * Atributos idénticos al diagrama: fechaHora, motivo.
 * Relaciones: "ejecutada por" Planificador, "0..1 originada por" Bloqueo
 * (también puede originarse por avería de vehículo), "afecta" a una Ruta.
 */
public class Reasignacion {

    private final LocalDateTime fechaHora;
    private final String motivo;
    private final Pedido pedidoReasignado;
    private final Ruta rutaOrigen;
    private final Ruta rutaDestino;
    private final Bloqueo bloqueoOrigen; // 0..1 — puede ser null (p.ej. avería)

    public Reasignacion(LocalDateTime fechaHora, String motivo, Pedido pedidoReasignado,
                         Ruta rutaOrigen, Ruta rutaDestino, Bloqueo bloqueoOrigen) {
        this.fechaHora = fechaHora;
        this.motivo = motivo;
        this.pedidoReasignado = pedidoReasignado;
        this.rutaOrigen = rutaOrigen;
        this.rutaDestino = rutaDestino;
        this.bloqueoOrigen = bloqueoOrigen;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public Pedido getPedidoReasignado() {
        return pedidoReasignado;
    }

    public Ruta getRutaOrigen() {
        return rutaOrigen;
    }

    public Ruta getRutaDestino() {
        return rutaDestino;
    }

    public Bloqueo getBloqueoOrigen() {
        return bloqueoOrigen;
    }

    @Override
    public String toString() {
        return "Reasignacion[" + fechaHora + "] pedido=" + pedidoReasignado.getIdPedido()
                + " de " + (rutaOrigen != null ? rutaOrigen.getIdRuta() : "-")
                + " a " + (rutaDestino != null ? rutaDestino.getIdRuta() : "-")
                + " motivo=" + motivo;
    }
}
