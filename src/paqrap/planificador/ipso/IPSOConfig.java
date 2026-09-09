package paqrap.planificador.ipso;

/**
 * Parámetros de entrada del algoritmo IPSO, según el pseudocódigo del
 * informe de selección de algoritmos:
 * "Tamaño de población N, Iteraciones máximas T_max, Coeficientes c1, c2,
 *  Tasa de cruce p_c".
 * Se añade umbralEstancamiento e inercia como parámetros de calibración
 * empírica mencionados en las observaciones del informe.
 */
public class IPSOConfig {

    private int tamanoPoblacionN = 40;
    private int iteracionesMaximasT = 300;
    private double c1 = 1.5;             // peso hacia P_best
    private double c2 = 1.5;             // peso hacia G_best
    private double tasaCruceP_c = 0.30;  // probabilidad de aplicar Order Crossover
    private double inercia = 0.35;       // fracción de velocidad anterior conservada
    private double umbralEstancamiento = 1.0e-4; // varianza mínima de fitness aceptada
    private long semillaAleatoria = System.nanoTime();

    public int getTamanoPoblacionN() {
        return tamanoPoblacionN;
    }

    public IPSOConfig setTamanoPoblacionN(int n) {
        this.tamanoPoblacionN = n;
        return this;
    }

    public int getIteracionesMaximasT() {
        return iteracionesMaximasT;
    }

    public IPSOConfig setIteracionesMaximasT(int t) {
        this.iteracionesMaximasT = t;
        return this;
    }

    public double getC1() {
        return c1;
    }

    public IPSOConfig setC1(double c1) {
        this.c1 = c1;
        return this;
    }

    public double getC2() {
        return c2;
    }

    public IPSOConfig setC2(double c2) {
        this.c2 = c2;
        return this;
    }

    public double getTasaCruceP_c() {
        return tasaCruceP_c;
    }

    public IPSOConfig setTasaCruceP_c(double p) {
        this.tasaCruceP_c = p;
        return this;
    }

    public double getInercia() {
        return inercia;
    }

    public IPSOConfig setInercia(double inercia) {
        this.inercia = inercia;
        return this;
    }

    public double getUmbralEstancamiento() {
        return umbralEstancamiento;
    }

    public IPSOConfig setUmbralEstancamiento(double umbral) {
        this.umbralEstancamiento = umbral;
        return this;
    }

    public long getSemillaAleatoria() {
        return semillaAleatoria;
    }

    public IPSOConfig setSemillaAleatoria(long semilla) {
        this.semillaAleatoria = semilla;
        return this;
    }
}
