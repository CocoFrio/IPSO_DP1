package paqrap.planificador.ipso;

/**
 * Operador de intercambio (Swap Operator, SO): intercambia los elementos de
 * las posiciones i y j de una permutación. Una secuencia de operadores de
 * intercambio (Swap Sequence, SS) es la representación discreta de
 * "velocidad" que usa el IPSO para adaptar el PSO clásico (continuo) al
 * problema combinatorio del TSP, tal como describe el mecanismo
 * "Operadores y secuencias de intercambio" del informe de selección.
 */
public class OperadorIntercambio {
    private final int i;
    private final int j;

    public OperadorIntercambio(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    /** Aplica el intercambio in-place sobre la permutación dada. */
    public void aplicar(int[] permutacion) {
        int tmp = permutacion[i];
        permutacion[i] = permutacion[j];
        permutacion[j] = tmp;
    }
}
