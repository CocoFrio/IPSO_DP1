package paqrap.planificador.ipso;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import paqrap.model.Pedido;
import paqrap.model.PuntoMapa;
import paqrap.model.Vehiculo;
import paqrap.planificador.DistanciaProveedor;

/**
 * Implementación del algoritmo IPSO (Improved Particle Swarm Optimization)
 * descrito en el informe de selección de algoritmos de PaqRap. Cada
 * comentario de sección corresponde línea a línea con el pseudocódigo:
 *
 *  Entrada: Grafo G(V, E), Matriz de distancias D, Tamaño de población N,
 *  Iteraciones máximas T_max, Coeficientes c1, c2, Tasa de cruce p_c
 *  Salida: Ruta óptima global G_best
 *
 * El problema se modela como un TSP con un solo depósito (el almacén desde
 * el cual sale el vehículo): V = {pedidos a entregar}, y el depósito se
 * agrega como nodo adicional en la matriz de distancias D.
 *
 * Nota de escalabilidad (observación del informe): para más de 30-50
 * paradas se recomienda clusterizar por zonas y ejecutar el IPSO por
 * cuadrante; esa clusterización la realiza el ClusterizadorPedidos antes de
 * invocar este optimizador, por lo que aquí "pedidos" ya corresponde a la
 * carga de un solo vehículo.
 */
public class IPSOOptimizador {

    private static final double TIEMPO_ENTREGA_HORAS = 1.0; // fijo, según situación auténtica
    private static final double PENALIZACION_POR_UNIDAD_EXCEDENTE = 500.0;
    private static final double PENALIZACION_POR_HORA_TARDIA = 300.0;

    private final List<Pedido> pedidos;      // V (sin el depósito)
    private final PuntoMapa deposito;        // nodo de partida/llegada (almacén)
    private final Vehiculo vehiculo;
    private final DistanciaProveedor proveedorDistancia;
    private final IPSOConfig config;
    private final LocalDateTime horaSalida;
    private final Random rnd;

    private double[][] D; // matriz de distancias: índices 0..n-1 = pedidos, índice n = depósito
    private int indiceDeposito;

    public IPSOOptimizador(List<Pedido> pedidos, PuntoMapa deposito, Vehiculo vehiculo,
                            DistanciaProveedor proveedorDistancia, IPSOConfig config,
                            LocalDateTime horaSalida) {
        this.pedidos = pedidos;
        this.deposito = deposito;
        this.vehiculo = vehiculo;
        this.proveedorDistancia = proveedorDistancia;
        this.config = config;
        this.horaSalida = horaSalida;
        this.rnd = new Random(config.getSemillaAleatoria());
    }

    /** Ejecuta el algoritmo completo y devuelve la mejor ruta encontrada (G_best). */
    public ResultadoIPSO ejecutar() {
        int n = pedidos.size();
        if (n == 0) {
            return new ResultadoIPSO(new ArrayList<>(), 0.0, 0.0, true);
        }
        if (n == 1) {
            return traducirResultado(new int[]{0});
        }

        construirMatrizDistancias();

        // Inicializar población P = {X_1, X_2, ..., X_N} con permutaciones
        // aleatorias de vértices V
        Particula[] poblacion = inicializarPoblacion(n);

        // Para cada partícula i en P: Calcular Fitness(X_i); P_best[i] = X_i
        for (Particula p : poblacion) {
            double fit = fitness(p.getPosicion());
            p.setFitnessActual(fit);
            p.setMejorPersonal(p.getPosicion().clone());
            p.setFitnessMejorPersonal(fit);
        }

        // G_best = argmax(Fitness(P_best[i]))
        int[] gBest = poblacion[0].getMejorPersonal().clone();
        double fitnessGBest = poblacion[0].getFitnessMejorPersonal();
        for (Particula p : poblacion) {
            if (p.getFitnessMejorPersonal() > fitnessGBest) {
                fitnessGBest = p.getFitnessMejorPersonal();
                gBest = p.getMejorPersonal().clone();
            }
        }

        int t = 0;
        while (t < config.getIteracionesMaximasT()) {

            for (Particula p : poblacion) {
                // --- Actualización de velocidad basada en operadores de intercambio ---
                // V_i = V_i + c1*rand()*(P_best[i] - X_i) + c2*rand()*(G_best - X_i)
                List<OperadorIntercambio> nuevaVelocidad =
                        combinarVelocidad(p.getVelocidad(), p.getPosicion(), p.getMejorPersonal(), gBest);
                p.setVelocidad(nuevaVelocidad);

                // --- Actualización de posición ---  X_i = X_i + V_i
                int[] nuevaPosicion = aplicarVelocidad(p.getPosicion(), nuevaVelocidad);

                // --- Operador de Cruce de Orden (OX) para diversidad ---
                if (rnd.nextDouble() < config.getTasaCruceP_c()) {
                    nuevaPosicion = cruceDeOrden(nuevaPosicion, gBest);
                }
                p.setPosicion(nuevaPosicion);

                double fit = fitness(nuevaPosicion);
                p.setFitnessActual(fit);

                if (fit > p.getFitnessMejorPersonal()) {
                    p.setMejorPersonal(nuevaPosicion.clone());
                    p.setFitnessMejorPersonal(fit);
                }
                if (fit > fitnessGBest) {
                    fitnessGBest = fit;
                    gBest = nuevaPosicion.clone();
                }
            }

            // --- Prevención de estancamiento en óptimo local (Factor Heurístico) ---
            double[] fitnessPoblacion = new double[poblacion.length];
            for (int i = 0; i < poblacion.length; i++) {
                fitnessPoblacion[i] = poblacion[i].getFitnessActual();
            }
            if (varianza(fitnessPoblacion) < config.getUmbralEstancamiento()) {
                aplicarMutacionHeuristicaBasadaEnDistancia(poblacion);
                // recalcular G_best por si la mutación produjo una mejora
                for (Particula p : poblacion) {
                    if (p.getFitnessMejorPersonal() > fitnessGBest) {
                        fitnessGBest = p.getFitnessMejorPersonal();
                        gBest = p.getMejorPersonal().clone();
                    }
                }
            }

            t++;
        }

        return traducirResultado(gBest);
    }

    // ------------------------------------------------------------------
    // Construcción de la matriz de distancias D
    // ------------------------------------------------------------------
    private void construirMatrizDistancias() {
        int n = pedidos.size();
        indiceDeposito = n;
        D = new double[n + 1][n + 1];
        List<PuntoMapa> puntos = new ArrayList<>(pedidos);
        puntos.add(deposito);
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == j) {
                    D[i][j] = 0.0;
                } else {
                    D[i][j] = proveedorDistancia.distanciaKm(puntos.get(i), puntos.get(j));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Inicialización del enjambre: cada partícula es una permutación
    // aleatoria de vértices (secuencia aleatoria de clientes a visitar
    // desde el almacén), con velocidad inicial = secuencia de operadores
    // de intercambio aleatorios.
    // ------------------------------------------------------------------
    private Particula[] inicializarPoblacion(int n) {
        Particula[] poblacion = new Particula[config.getTamanoPoblacionN()];
        for (int i = 0; i < poblacion.length; i++) {
            int[] permutacion = permutacionAleatoria(n);
            Particula particula = new Particula(permutacion);
            int numSwapsIniciales = rnd.nextInt(n);
            List<OperadorIntercambio> velocidadInicial = new ArrayList<>();
            for (int k = 0; k < numSwapsIniciales; k++) {
                int a = rnd.nextInt(n);
                int b = rnd.nextInt(n);
                velocidadInicial.add(new OperadorIntercambio(a, b));
            }
            particula.setVelocidad(velocidadInicial);
            poblacion[i] = particula;
        }
        return poblacion;
    }

    private int[] permutacionAleatoria(int n) {
        int[] permutacion = new int[n];
        for (int i = 0; i < n; i++) {
            permutacion[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = permutacion[i];
            permutacion[i] = permutacion[j];
            permutacion[j] = tmp;
        }
        return permutacion;
    }

    // ------------------------------------------------------------------
    // Cálculo de aptitud (Fitness): Fitness(X_i) = 1 / Costo_Ruta(X_i, D).
    // El costo penaliza numéricamente exceso de capacidad y
    // vencimiento de plazos, tal como exige el informe de selección.
    // ------------------------------------------------------------------
    private double fitness(int[] permutacion) {
        return 1.0 / costoRuta(permutacion);
    }

    private double costoRuta(int[] permutacion) {
        double distanciaTotalKm = 0.0;
        double tiempoAcumuladoHoras = 0.0;
        double penalizacionPlazos = 0.0;
        int cargaTotal = 0;

        int nodoAnterior = indiceDeposito;
        for (int idx : permutacion) {
            Pedido pedido = pedidos.get(idx);
            cargaTotal += pedido.getCantidadSolicitada();

            double distanciaTramo = D[nodoAnterior][idx];
            distanciaTotalKm += distanciaTramo;
            tiempoAcumuladoHoras += distanciaTramo / vehiculo.getVelocidadKmH();

            LocalDateTime llegadaEstimada = horaSalida.plusMinutes(Math.round(tiempoAcumuladoHoras * 60));
            if (llegadaEstimada.isAfter(pedido.calcularFechaLimite())) {
                double horasTarde = java.time.Duration.between(pedido.calcularFechaLimite(), llegadaEstimada).toMinutes() / 60.0;
                penalizacionPlazos += horasTarde * PENALIZACION_POR_HORA_TARDIA;
            }

            tiempoAcumuladoHoras += TIEMPO_ENTREGA_HORAS; // tiempo de entrega al destinatario
            nodoAnterior = idx;
        }
        // regreso al depósito para cerrar el ciclo de la ruta del vehículo
        distanciaTotalKm += D[nodoAnterior][indiceDeposito];

        double costoDistancia = distanciaTotalKm * vehiculo.getCostoPorKm();

        double penalizacionCapacidad = 0.0;
        int exceso = cargaTotal - vehiculo.getCapacidad();
        if (exceso > 0) {
            penalizacionCapacidad = exceso * PENALIZACION_POR_UNIDAD_EXCEDENTE;
        }

        double costoTotal = costoDistancia + penalizacionCapacidad + penalizacionPlazos;
        return Math.max(costoTotal, 0.000001); // evitar división por cero en Fitness
    }

    // ------------------------------------------------------------------
    // Actualización de velocidad basada en operadores de intercambio.
    // Adapta la fórmula continua del PSO clásico a operaciones de
    // permutación discreta, tal como describe el mecanismo del informe.
    // ------------------------------------------------------------------
    private List<OperadorIntercambio> combinarVelocidad(List<OperadorIntercambio> velocidadAnterior,
                                                          int[] posicionActual, int[] pBest, int[] gBest) {
        List<OperadorIntercambio> nuevaVelocidad = new ArrayList<>();

        // Inercia: se conserva una fracción de la velocidad anterior
        for (OperadorIntercambio op : velocidadAnterior) {
            if (rnd.nextDouble() < config.getInercia()) {
                nuevaVelocidad.add(op);
            }
        }

        // c1 * rand() * (P_best[i] - X_i): secuencia de intercambios que
        // transforma X_i en P_best[i], incorporada con probabilidad c1*rand()
        List<OperadorIntercambio> ssPBest = calcularSecuenciaIntercambio(pBest, posicionActual);
        double probPBest = Math.min(1.0, config.getC1() * rnd.nextDouble());
        for (OperadorIntercambio op : ssPBest) {
            if (rnd.nextDouble() < probPBest) {
                nuevaVelocidad.add(op);
            }
        }

        // c2 * rand() * (G_best - X_i): ídem hacia el óptimo global
        List<OperadorIntercambio> ssGBest = calcularSecuenciaIntercambio(gBest, posicionActual);
        double probGBest = Math.min(1.0, config.getC2() * rnd.nextDouble());
        for (OperadorIntercambio op : ssGBest) {
            if (rnd.nextDouble() < probGBest) {
                nuevaVelocidad.add(op);
            }
        }

        return nuevaVelocidad;
    }

    /**
     * Calcula la secuencia de operadores de intercambio (Swap Sequence) que,
     * aplicada sobre "origen", produce "objetivo". Es la representación
     * discreta de la resta vectorial (objetivo - origen) del PSO clásico.
     */
    private List<OperadorIntercambio> calcularSecuenciaIntercambio(int[] objetivo, int[] origen) {
        List<OperadorIntercambio> secuencia = new ArrayList<>();
        int[] temp = origen.clone();
        int n = temp.length;
        for (int i = 0; i < n; i++) {
            if (temp[i] != objetivo[i]) {
                int j = indexOf(temp, objetivo[i], i);
                OperadorIntercambio op = new OperadorIntercambio(i, j);
                op.aplicar(temp);
                secuencia.add(op);
            }
        }
        return secuencia;
    }

    private int indexOf(int[] arreglo, int valor, int desde) {
        for (int i = desde; i < arreglo.length; i++) {
            if (arreglo[i] == valor) {
                return i;
            }
        }
        return desde;
    }

    /** Actualización de posición: X_i = X_i + V_i (aplicación secuencial de swaps). */
    private int[] aplicarVelocidad(int[] posicion, List<OperadorIntercambio> velocidad) {
        int[] nueva = posicion.clone();
        for (OperadorIntercambio op : velocidad) {
            op.aplicar(nueva);
        }
        return nueva;
    }

    // ------------------------------------------------------------------
    // Cruce de Orden (Order Crossover, OX): fusiona un fragmento de
    // G_best (genes dominantes) dentro de la partícula, preservando el
    // orden relativo del resto — mecanismo tomado de los Algoritmos
    // Genéticos según el informe.
    // ------------------------------------------------------------------
    private int[] cruceDeOrden(int[] padreA, int[] padreB) {
        int n = padreA.length;
        int[] hijo = new int[n];
        boolean[] usado = new boolean[n];
        java.util.Arrays.fill(hijo, -1);

        int corte1 = rnd.nextInt(n);
        int corte2 = rnd.nextInt(n);
        int inicio = Math.min(corte1, corte2);
        int fin = Math.max(corte1, corte2);

        for (int i = inicio; i <= fin; i++) {
            hijo[i] = padreA[i];
            usado[padreA[i]] = true;
        }

        int posicionHijo = (fin + 1) % n;
        int posicionPadreB = (fin + 1) % n;
        int insertados = 0;
        int totalAInsertar = n - (fin - inicio + 1);

        while (insertados < totalAInsertar) {
            int gen = padreB[posicionPadreB];
            if (!usado[gen]) {
                hijo[posicionHijo] = gen;
                usado[gen] = true;
                posicionHijo = (posicionHijo + 1) % n;
                insertados++;
            }
            posicionPadreB = (posicionPadreB + 1) % n;
        }

        return hijo;
    }

    // ------------------------------------------------------------------
    // Escape heurístico (Mutación heurística basada en distancia): cuando
    // la varianza de fitness de la población cae por debajo del umbral de
    // estancamiento, se reconstruye la cola de la ruta de la mitad peor de
    // las partículas mediante una heurística de vecino más cercano guiada
    // por las distancias reales, forzando la exploración de atajos.
    // ------------------------------------------------------------------
    private void aplicarMutacionHeuristicaBasadaEnDistancia(Particula[] poblacion) {
        int n = poblacion[0].getPosicion().length;
        if (n < 3) {
            return;
        }

        Particula[] ordenada = poblacion.clone();
        java.util.Arrays.sort(ordenada, (a, b) -> Double.compare(a.getFitnessActual(), b.getFitnessActual()));
        int mitad = ordenada.length / 2;

        for (int idx = 0; idx < mitad; idx++) {
            Particula peor = ordenada[idx];
            int[] posicionActual = peor.getPosicion();
            int puntoCorte = 1 + rnd.nextInt(n - 1);

            boolean[] visitado = new boolean[n];
            int[] nuevaPosicion = new int[n];
            for (int i = 0; i < puntoCorte; i++) {
                nuevaPosicion[i] = posicionActual[i];
                visitado[posicionActual[i]] = true;
            }

            int nodoActual = nuevaPosicion[puntoCorte - 1];
            for (int i = puntoCorte; i < n; i++) {
                int mejorCandidato = -1;
                double mejorDistancia = Double.MAX_VALUE;
                for (int cand = 0; cand < n; cand++) {
                    if (!visitado[cand] && D[nodoActual][cand] < mejorDistancia) {
                        mejorDistancia = D[nodoActual][cand];
                        mejorCandidato = cand;
                    }
                }
                nuevaPosicion[i] = mejorCandidato;
                visitado[mejorCandidato] = true;
                nodoActual = mejorCandidato;
            }

            double nuevoFitness = fitness(nuevaPosicion);
            peor.setPosicion(nuevaPosicion);
            peor.setFitnessActual(nuevoFitness);
            if (nuevoFitness > peor.getFitnessMejorPersonal()) {
                peor.setMejorPersonal(nuevaPosicion.clone());
                peor.setFitnessMejorPersonal(nuevoFitness);
            }
        }
    }

    private double varianza(double[] valores) {
        double media = 0.0;
        for (double v : valores) {
            media += v;
        }
        media /= valores.length;
        double suma = 0.0;
        for (double v : valores) {
            suma += (v - media) * (v - media);
        }
        return suma / valores.length;
    }

    private ResultadoIPSO traducirResultado(int[] gBest) {
        List<Pedido> secuencia = new ArrayList<>();
        for (int idx : gBest) {
            secuencia.add(pedidos.get(idx));
        }
        double costo = costoRuta(gBest);

        double distanciaTotalKm = 0.0;
        double tiempoAcumuladoHoras = 0.0;
        boolean cumplePlazos = true;
        int nodoAnterior = indiceDeposito;
        for (int idx : gBest) {
            double tramo = D[nodoAnterior][idx];
            distanciaTotalKm += tramo;
            tiempoAcumuladoHoras += tramo / vehiculo.getVelocidadKmH();
            LocalDateTime llegada = horaSalida.plusMinutes(Math.round(tiempoAcumuladoHoras * 60));
            if (llegada.isAfter(pedidos.get(idx).calcularFechaLimite())) {
                cumplePlazos = false;
            }
            tiempoAcumuladoHoras += TIEMPO_ENTREGA_HORAS;
            nodoAnterior = idx;
        }
        double tramoRegreso = D[nodoAnterior][indiceDeposito];
        distanciaTotalKm += tramoRegreso;
        tiempoAcumuladoHoras += tramoRegreso / vehiculo.getVelocidadKmH();

        return new ResultadoIPSO(secuencia, costo, tiempoAcumuladoHoras, cumplePlazos);
    }
}
