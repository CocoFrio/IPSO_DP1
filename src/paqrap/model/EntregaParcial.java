package paqrap.model;

import java.time.LocalDateTime;

/**
 * EntregaParcial — clase del diagrama de dominio.
 * Atributos idénticos al diagrama: cantidadEntregada, fechaEntrega.
 * Relación "se atiende mediante" (Pedido 1 -> * EntregaParcial) y
 * "ejecutada en" (EntregaParcial -> Ruta).
 */
public class EntregaParcial {

    private final Pedido pedido;
    private final Ruta rutaEjecutora;
    private final int cantidadEntregada;
    private final LocalDateTime fechaEntrega;

    public EntregaParcial(Pedido pedido, Ruta rutaEjecutora, int cantidadEntregada, LocalDateTime fechaEntrega) {
        this.pedido = pedido;
        this.rutaEjecutora = rutaEjecutora;
        this.cantidadEntregada = cantidadEntregada;
        this.fechaEntrega = fechaEntrega;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Ruta getRutaEjecutora() {
        return rutaEjecutora;
    }

    public int getCantidadEntregada() {
        return cantidadEntregada;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }
}
