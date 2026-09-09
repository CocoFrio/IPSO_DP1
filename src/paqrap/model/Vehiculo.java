package paqrap.model;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Unidad de flota (auto, moto o bicicleta). Encapsula las reglas de turno
 * (cambios cada 8h: 07:00, 15:00, 23:00) y de refrigerio (1 hora dentro de
 * la jornada, respetando 1 hora antes/después de un cambio de turno).
 */
public class Vehiculo implements PuntoMapa {

    private static final LocalTime[] CAMBIOS_TURNO = {
            LocalTime.of(7, 0), LocalTime.of(15, 0), LocalTime.of(23, 0)
    };
    private static final long VENTANA_PROTEGIDA_MIN = 60;

    private final String idVehiculo;
    private final TipoVehiculo tipo;
    private int posX;
    private int posY;
    private boolean disponible;
    private LocalTime inicioRefrigerio;

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

    public void setInicioRefrigerio(LocalTime inicioRefrigerio) {
        this.inicioRefrigerio = inicioRefrigerio;
    }

    /**
     * Indica si el vehículo puede estar circulando en el instante dado,
     * respetando cambios de turno y la hora de refrigerio asignada.
     * (Requisito: 1 hora de refrigerio, al menos 1h antes/después de un
     * cambio de turno).
     */
    public boolean disponibleEnInstante(LocalTime instante) {
        for (LocalTime cambio : CAMBIOS_TURNO) {
            if (Math.abs(Duration.between(cambio, instante).toMinutes()) < VENTANA_PROTEGIDA_MIN) {
                return false;
            }
        }
        if (inicioRefrigerio != null) {
            long minutosDesdeRefrigerio = Math.abs(
                    Duration.between(inicioRefrigerio, instante).toMinutes());
            if (minutosDesdeRefrigerio < VENTANA_PROTEGIDA_MIN) {
                return false;
            }
        }
        return true;
    }
}
