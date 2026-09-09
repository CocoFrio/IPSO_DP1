package paqrap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Ruta — clase del diagrama de dominio.
 * Atributos idénticos al diagrama: idRuta, costoEstimado, duracionEstimada,
 * estado, secuenciaEntrega (List).
 */
public class Ruta {

    public static final String ESTADO_PLANIFICADA = "PLANIFICADA";
    public static final String ESTADO_EN_EJECUCION = "EN_EJECUCION";
    public static final String ESTADO_FINALIZADA = "FINALIZADA";
    public static final String ESTADO_REPLANIFICADA = "REPLANIFICADA";

    private final String idRuta;
    private final Vehiculo vehiculoAsignado;
    private final Almacen almacenOrigen;
    private float costoEstimado;
    private float duracionEstimada; // horas
    private String estado;
    private List<Pedido> secuenciaEntrega;

    public Ruta(String idRuta, Vehiculo vehiculoAsignado, Almacen almacenOrigen) {
        this.idRuta = idRuta;
        this.vehiculoAsignado = vehiculoAsignado;
        this.almacenOrigen = almacenOrigen;
        this.secuenciaEntrega = new ArrayList<>();
        this.estado = ESTADO_PLANIFICADA;
    }

    public String getIdRuta() {
        return idRuta;
    }

    public Vehiculo getVehiculoAsignado() {
        return vehiculoAsignado;
    }

    public Almacen getAlmacenOrigen() {
        return almacenOrigen;
    }

    public float getCostoEstimado() {
        return costoEstimado;
    }

    public void setCostoEstimado(float costoEstimado) {
        this.costoEstimado = costoEstimado;
    }

    public float getDuracionEstimada() {
        return duracionEstimada;
    }

    public void setDuracionEstimada(float duracionEstimada) {
        this.duracionEstimada = duracionEstimada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<Pedido> getSecuenciaEntrega() {
        return secuenciaEntrega;
    }

    public void setSecuenciaEntrega(List<Pedido> secuenciaEntrega) {
        this.secuenciaEntrega = secuenciaEntrega;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ruta ").append(idRuta)
          .append(" [").append(vehiculoAsignado.getTipo()).append(" ")
          .append(vehiculoAsignado.getIdVehiculo()).append("] costo=S/ ")
          .append(String.format("%.2f", costoEstimado))
          .append(" duracion=").append(String.format("%.2f", duracionEstimada)).append("h")
          .append(" estado=").append(estado).append("\n  secuencia: ");
        for (Pedido p : secuenciaEntrega) {
            sb.append(p.getIdPedido()).append(" -> ");
        }
        sb.append("FIN");
        return sb.toString();
    }
}
