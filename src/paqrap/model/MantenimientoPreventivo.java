package paqrap.model;

import java.time.LocalDate;

/**
 * Mantenimiento preventivo programado para una unidad de transporte.
 */
public class MantenimientoPreventivo {

    private final LocalDate fecha;
    private final String tipoVehiculo;
    private final String numeroVehiculo;

    public MantenimientoPreventivo(LocalDate fecha, String tipoVehiculo, String numeroVehiculo) {
        if (fecha == null || tipoVehiculo == null || numeroVehiculo == null
                || tipoVehiculo.isEmpty() || numeroVehiculo.isEmpty()) {
            throw new IllegalArgumentException("Los datos del mantenimiento son obligatorios");
        }
        this.fecha = fecha;
        this.tipoVehiculo = tipoVehiculo;
        this.numeroVehiculo = numeroVehiculo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public String getNumeroVehiculo() {
        return numeroVehiculo;
    }

    public String getIdVehiculo() {
        return tipoVehiculo + numeroVehiculo;
    }
}
