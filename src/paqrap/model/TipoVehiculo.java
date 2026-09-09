package paqrap.model;

/**
 * Catálogo de tipos de flota de PaqRap.
 * Valores tomados de la situación auténtica del curso:
 *  - Autos:      24 paquetes, 40 Km/h, S/ 8.00 por Km
 *  - Motos:       8 paquetes, 25 Km/h, S/ 6.00 por Km
 *  - Bicicletas:  4 paquetes, 12 Km/h, S/ 3.00 por Km
 */
public enum TipoVehiculo {
    AUTO(24, 40.0, 8.00),
    MOTO(8, 25.0, 6.00),
    BICICLETA(4, 12.0, 3.00);

    private final int capacidadPaquetes;
    private final double velocidadKmH;
    private final double costoPorKm;

    TipoVehiculo(int capacidadPaquetes, double velocidadKmH, double costoPorKm) {
        this.capacidadPaquetes = capacidadPaquetes;
        this.velocidadKmH = velocidadKmH;
        this.costoPorKm = costoPorKm;
    }

    public int getCapacidadPaquetes() {
        return capacidadPaquetes;
    }

    public double getVelocidadKmH() {
        return velocidadKmH;
    }

    public double getCostoPorKm() {
        return costoPorKm;
    }
}
