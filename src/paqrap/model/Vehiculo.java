package paqrap.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Unidad de flota (auto, moto o bicicleta). Encapsula las reglas de turno
 * y de refrigerio (1 hora dentro de la jornada).
 */
public class Vehiculo implements PuntoMapa {

    private static final long VENTANA_PROTEGIDA_MIN = 60;

    private final String idVehiculo;
    private final TipoVehiculo tipo;
    private int posX;
    private int posY;
    private boolean disponible;
    private LocalTime inicioRefrigerio;
    private final Set<LocalDate> fechasMantenimiento = new HashSet<>();

    public Vehiculo(String idVehiculo, TipoVehiculo tipo, int posXInicial, int posYInicial) {
        this.idVehiculo = idVehiculo;
        this.tipo = tipo;
        this.posX = posXInicial;
        this.posY = posYInicial;
        this.disponible = true;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public int getCapacidad() {
        return tipo.getCapacidadPaquetes();
    }

    public double getVelocidadKmH() {
        return tipo.getVelocidadKmH();
    }

    public double getCostoPorKm() {
        return tipo.getCostoPorKm();
    }

    @Override
    public int getPosX() {
        return posX;
    }

    @Override
    public int getPosY() {
        return posY;
    }

    public void setPosicion(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public void registrarMantenimiento(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de mantenimiento es obligatoria");
        }
        fechasMantenimiento.add(fecha);
    }

    public boolean estaEnMantenimiento(LocalDate fecha) {
        return fechasMantenimiento.contains(fecha);
    }

    public boolean disponibleEnInstante(LocalDateTime instante) {
        return disponible && !estaEnMantenimiento(instante.toLocalDate())
                && disponibleEnInstante(instante.toLocalTime());
    }

    public void setInicioRefrigerio(LocalTime inicioRefrigerio) {
        this.inicioRefrigerio = inicioRefrigerio;
    }

    /**
     * Indica si el vehículo puede estar circulando en el instante dado.
     * Los cambios de turno no bloquean la disponibilidad; solo se respeta
     * la hora de refrigerio configurada.
     */
    public boolean disponibleEnInstante(LocalTime instante) {
        if (inicioRefrigerio != null) {
            long minutoInicio = inicioRefrigerio.toSecondOfDay() / 60;
            long minutoActual = instante.toSecondOfDay() / 60;
            long diferencia = Math.abs(minutoInicio - minutoActual);
            if (diferencia < VENTANA_PROTEGIDA_MIN) {
                return false;
            }
        }
        return true;
    }
}
