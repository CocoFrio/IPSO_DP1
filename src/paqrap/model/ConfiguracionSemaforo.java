package paqrap.model;

/**
 * Umbrales configurables (requisito no funcional "d") para clasificar la
 * urgencia de un Pedido según la fracción de su plazo límite ya consumida.
 */
public class ConfiguracionSemaforo {

    private double umbralAmbar; // ej. 0.60 -> desde 60% del plazo consumido
    private double umbralRojo;  // ej. 0.85 -> desde 85% del plazo consumido

    public ConfiguracionSemaforo(double umbralAmbar, double umbralRojo) {
        this.umbralAmbar = umbralAmbar;
        this.umbralRojo = umbralRojo;
    }

    public SemaforoColor evaluar(double fraccionTiempoConsumido) {
        if (fraccionTiempoConsumido >= umbralRojo) {
            return SemaforoColor.ROJO;
        }
        if (fraccionTiempoConsumido >= umbralAmbar) {
            return SemaforoColor.AMBAR;
        }
        return SemaforoColor.VERDE;
    }

    public double getUmbralAmbar() {
        return umbralAmbar;
    }

    public void setUmbralAmbar(double umbralAmbar) {
        this.umbralAmbar = umbralAmbar;
    }

    public double getUmbralRojo() {
        return umbralRojo;
    }

    public void setUmbralRojo(double umbralRojo) {
        this.umbralRojo = umbralRojo;
    }
}
