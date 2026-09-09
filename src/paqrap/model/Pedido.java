package paqrap.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pedido — clase del diagrama de dominio.
 * Atributos idénticos al diagrama: idPedido, idCliente, posX, posY,
 * cantidadSolicitada, horasLimite, fechaIngreso, estado, calcularFechaLimite().
 */
public class Pedido implements PuntoMapa {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ASIGNADO = "ASIGNADO";
    public static final String ESTADO_EN_RUTA = "EN_RUTA";
    public static final String ESTADO_ENTREGADO = "ENTREGADO";
    public static final String ESTADO_REASIGNADO = "REASIGNADO";

    private final String idPedido;
    private final String idCliente;
    private final int posX;
    private final int posY;
    private final int cantidadSolicitada;
    private final int horasLimite; // 36 (normal) o 4 / 8 / 12 / 18 (priorizada)
    private final LocalDateTime fechaIngreso;
    private String estado;

    public Pedido(String idPedido, String idCliente, int posX, int posY,
                   int cantidadSolicitada, int horasLimite, LocalDateTime fechaIngreso) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.posX = posX;
        this.posY = posY;
        this.cantidadSolicitada = cantidadSolicitada;
        this.horasLimite = horasLimite;
        this.fechaIngreso = fechaIngreso;
        this.estado = ESTADO_PENDIENTE;
    }

    /** calcularFechaLimite(): fechaIngreso + horasLimite comprometidas. */
    public LocalDateTime calcularFechaLimite() {
        return fechaIngreso.plusHours(horasLimite);
    }

    /** Fracción de plazo consumida a un instante dado; insumo del semáforo. */
    public double fraccionPlazoConsumido(LocalDateTime instante) {
        long totalMin = Duration.between(fechaIngreso, calcularFechaLimite()).toMinutes();
        long transcurridoMin = Duration.between(fechaIngreso, instante).toMinutes();
        if (totalMin <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) transcurridoMin / totalMin));
    }

    public String getIdPedido() {
        return idPedido;
    }

    public String getIdCliente() {
        return idCliente;
    }

    @Override
    public int getPosX() {
        return posX;
    }

    @Override
    public int getPosY() {
        return posY;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }

    public int getHorasLimite() {
        return horasLimite;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return idPedido + "(cli=" + idCliente + ", qty=" + cantidadSolicitada
                + ", limite=" + horasLimite + "h, estado=" + estado + ")";
    }
}
