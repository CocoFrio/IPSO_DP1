package paqrap.planificador.ipso;

import java.util.ArrayList;
import java.util.List;

/**
 * Partícula del enjambre. Representa una permutación válida de entregas
 * (posición X_i), su "velocidad" V_i como secuencia de operadores de
 * intercambio, y sus memorias P_best (óptimo personal) y fitness asociado,
 * tal como los define el pseudocódigo del IPSO.
 */
public class Particula {

    private int[] posicion;                 // X_i
    private List<OperadorIntercambio> velocidad; // V_i
    private int[] mejorPersonal;             // P_best[i]
    private double fitnessMejorPersonal;     // Fitness(P_best[i])
    private double fitnessActual;            // Fitness(X_i)

    public Particula(int[] posicionInicial) {
        this.posicion = posicionInicial;
        this.velocidad = new ArrayList<>();
        this.mejorPersonal = posicionInicial.clone();
    }

    public int[] getPosicion() {
        return posicion;
    }

    public void setPosicion(int[] posicion) {
        this.posicion = posicion;
    }

    public List<OperadorIntercambio> getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(List<OperadorIntercambio> velocidad) {
        this.velocidad = velocidad;
    }

    public int[] getMejorPersonal() {
        return mejorPersonal;
    }

    public void setMejorPersonal(int[] mejorPersonal) {
        this.mejorPersonal = mejorPersonal;
    }

    public double getFitnessMejorPersonal() {
        return fitnessMejorPersonal;
    }

    public void setFitnessMejorPersonal(double fitnessMejorPersonal) {
        this.fitnessMejorPersonal = fitnessMejorPersonal;
    }

    public double getFitnessActual() {
        return fitnessActual;
    }

    public void setFitnessActual(double fitnessActual) {
        this.fitnessActual = fitnessActual;
    }
}
