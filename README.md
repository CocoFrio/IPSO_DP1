# PaqRap — Componente Planificador con algoritmo IPSO

Implementación en Java del componente **planificador** de PaqRap usando el
algoritmo metaheurístico **IPSO (Improved Particle Swarm Optimization)**
seleccionado en el *Informe de Selección de Algoritmos* (Equipo 6F), aplicado
sobre el fragmento del diagrama de clases de dominio entregado y sobre las
reglas de negocio de la situación auténtica del curso.

## 1. Cómo compilar y ejecutar

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out paqrap.demo.DemoPaqRap
```

Para ejecutar el algoritmo usando los archivos de `data`:

```powershell
javac -encoding UTF-8 -d .\out (Get-Content .\sources.txt)
java -cp .\out paqrap.demo.DemoConDatos 2026 9 13 8
```

`DemoConDatos` carga las ventas y bloqueos del mes indicado, lee el archivo de
mantenimiento preventivo, crea la flota correspondiente y muestra cuántos
registros fueron leídos, cuántas rutas generó IPSO y cuántos pedidos quedaron
pendientes. Los parámetros corresponden a `año mes día hora`; el día y la hora
son opcionales y, si se omiten, se usa el primer día del mes a las 00:00.
La flota completa se crea independientemente del archivo de mantenimiento:
10 autos (`TA01`-`TA10`), 12 bicicletas (`TB01`-`TB12`) y 15 motos
(`TM01`-`TM15`). El mantenimiento solo marca como no disponible la unidad y
fecha que correspondan.
Las posiciones usadas por los demos con datos son: almacén central `(27,14)`,
almacén intermedio Nor-Oeste `(12,38)` y almacén intermedio Este `(57,27)`.
La flota inicia en el almacén central.

Para simular el avance del tiempo, usa:

```powershell
java -cp .\out paqrap.demo.SimulacionConDatos 2026 9 1 0 24
```

Los últimos cinco parámetros son `año mes día hora horasDeSimulación`. La
simulación avanza 15 minutos por iteración, incorpora pedidos y bloqueos
únicamente cuando llega su fecha, y muestra los pedidos pendientes ordenados
por el tiempo restante hasta su límite de entrega. Los pedidos y bloqueos
anteriores al día y hora iniciales se consideran históricos y se ignoran.
En cada intervalo también se imprime cada pedido recién incorporado, su
posición, límite de entrega y estado. `PENDIENTE` significa que llegó pero aún
no tiene ruta; `EN_RUTA` significa que fue asignado a una ruta activa.

En Windows PowerShell, `@sources.txt` se interpreta como una expansión de
argumentos. Usa esta variante:

```powershell
Get-ChildItem -Path .\src -Recurse -Filter *.java |
    ForEach-Object { $_.FullName } |
    Set-Content -Encoding ASCII .\sources.txt
New-Item -ItemType Directory -Force .\out
javac -encoding UTF-8 -d .\out (Get-Content .\sources.txt)
java -cp .\out paqrap.demo.DemoPaqRap
```

`DemoPaqRap` arma un escenario pequeño (3 almacenes, 3 vehículos, 8 pedidos),
ejecuta `planificarRutas()`, simula un `Bloqueo` de calle y ejecuta
`replanificar()`, imprimiendo las rutas, el semáforo de urgencia y el
historial de `Reasignacion` generado.

## 2. Estructura del proyecto

```
src/paqrap/model/          Clases de dominio (fragmento del diagrama de clases)
src/paqrap/planificador/   Planificador, clusterización y proveedor de distancias
src/paqrap/planificador/ipso/   Motor del algoritmo IPSO
src/paqrap/demo/           Caso de uso ejecutable
```

## 3. Mapeo con el diagrama de clases de dominio

| Clase del diagrama | Archivo Java                     | Notas |
|---------------------|-----------------------------------|-------|
| `Pedido`            | `model/Pedido.java`               | Atributos y `calcularFechaLimite()` idénticos al diagrama. |
| `Ruta`               | `model/Ruta.java`                 | `secuenciaEntrega` es la salida del IPSO. |
| `Bloqueo`            | `model/Bloqueo.java`              | `secuenciaNodos` + verificación de vigencia/interferencia con un tramo. |
| `Reasignacion`       | `model/Reasignacion.java`         | Generada en `replanificar()`, referencia opcional (0..1) a `Bloqueo`. |
| `EntregaParcial`     | `model/EntregaParcial.java`       | Punto de extensión para registrar entregas parciales por `Ruta`. |
| `Planificador`       | `planificador/Planificador.java`  | Firma **idéntica** al diagrama: `planificarRutas(): List`, `replanificar(): List`, sin parámetros — el estado (pedidos, flota, almacenes, bloqueos) se inyecta antes en `PlanificadorIPSO`. |

Clases de apoyo no centrales al algoritmo (`CargaArchivoVentas`,
`Simulacion`, `ReporteDesempeno`) no se implementaron porque no participan
del cálculo de rutas; son puntos de integración con el resto del sistema
(carga de pedidos y motor de simulación/visualización) ya cubiertos por el
prototipo de frontend.

## 3.1 Lectura de archivos TXT

`paqrap.io.LectorArchivos` lee los tres formatos de la zona de datos:

```java
List<Pedido> pedidos = LectorArchivos.leerVentas(
        Paths.get("data/ventas/ventas.202601.txt"), 2026, 1);
List<Bloqueo> bloqueos = LectorArchivos.leerBloqueos(
        Paths.get("data/bloqueos/bloqueo.2601.txt"), 2026, 1);
List<MantenimientoPreventivo> mantenimientos =
        LectorArchivos.leerMantenimientos(
                Paths.get("data/mant.preventivo.09.10.txt"));
```

La lectura valida cada línea y reporta el archivo y número de línea cuando
encuentra un registro inválido. Los nombres anteriores corresponden a los
archivos entregados; también son compatibles con los nombres descritos en la
especificación (`aaaamm.bloqueadas` y `ventas2026mm`).

Para aplicar los mantenimientos a la planificación, se registra cada elemento
en `PlanificadorIPSO` mediante `registrarMantenimiento(...)`. La unidad queda
fuera de la planificación durante todo ese día.

## 4. Mapeo con la situación auténtica

- **Flota** (`TipoVehiculo`): autos (24 paq., 40 Km/h, S/8/Km), motos (8 paq.,
  25 Km/h, S/6/Km), bicicletas (4 paq., 12 Km/h, S/3/Km).
- **Almacenes** (`AlmacenCentral`, `AlmacenIntermedio`): central con
  inventario infinito; intermedios con capacidad máxima 1000 unidades y
  recarga instantánea (simplificación del curso).
- **Plazos** (36h normal / 4-8-12-18h priorizado): `Pedido.horasLimite` +
  `calcularFechaLimite()`, penalizados numéricamente en la función de
  aptitud del IPSO si se incumplen.
- **Semáforo configurable** (`ConfiguracionSemaforo`): umbrales de color
  parametrizables sobre la fracción de plazo consumida (RNF "d").
- **Calles de doble sentido** (RNF "c") y **bloqueos**: `DistanciaGridConBloqueos`
  usa distancia Manhattan sobre el grid y penaliza tramos interferidos por un
  `Bloqueo` vigente (proxy simplificado; reemplazable por ruta más corta real
  del grafo de calles del visualizador).
- **Disponibilidad y refrigerio**: `Vehiculo.disponibleEnInstante(...)` no
  bloquea la unidad por cambios de turno; solo considera el estado de la
  unidad, sus mantenimientos preventivos y, si se configuró, su refrigerio.
- **Reasignación ante bloqueos/averías**: `PlanificadorIPSO.replanificar()`
  libera los pedidos aún no entregados de la ruta afectada, los prioriza por
  plazo más crítico y vuelve a invocar el IPSO, generando un registro
  `Reasignacion` por cada pedido movido.

## 5. Mapeo del pseudocódigo IPSO (informe de selección) → código

| Paso del pseudocódigo | Método en `IPSOOptimizador` |
|---|---|
| `Inicializar población P` con permutaciones aleatorias | `inicializarPoblacion(n)` / `permutacionAleatoria(n)` |
| `Inicializar velocidades V_i` | dentro de `inicializarPoblacion` |
| `Calcular Fitness(X_i) = 1 / Costo_Ruta(X_i, D)` | `fitness(...)` / `costoRuta(...)` |
| `P_best[i] = X_i`, `G_best = argmax(...)` | bucle de inicialización en `ejecutar()` |
| `V_i = V_i + c1*rand()*(P_best-X_i) + c2*rand()*(G_best-X_i)` | `combinarVelocidad(...)` + `calcularSecuenciaIntercambio(...)` |
| `X_i = X_i + V_i` | `aplicarVelocidad(...)` |
| `Si rand() < p_c: X_i = OrderCrossover(X_i, G_best)` | `cruceDeOrden(...)` |
| Actualización de `P_best` / `G_best` | bucle principal en `ejecutar()` |
| `Si Varianza_Poblacion(Fitness(P)) < Umbral: Mutación heurística` | `varianza(...)` + `aplicarMutacionHeuristicaBasadaEnDistancia(...)` |
| `Retornar G_best` | `traducirResultado(gBest)` |

La función de aptitud (`costoRuta`) penaliza numéricamente el exceso de
capacidad del vehículo y el incumplimiento del plazo comprometido del
cliente, tal como exigen las observaciones del informe de selección de
algoritmos.

## 6. Supuestos y puntos de extensión

- La distancia usa coordenadas de grid (Manhattan) como aproximación de la
  malla vial; se aísla detrás de `DistanciaProveedor` para poder conectarla
  al grafo real de calles del componente visualizador (con Dijkstra/A* y
  bloqueos exactos) sin modificar el IPSO.
- Para más de 30-50 paradas (observación del informe), `ClusterizadorPedidos`
  agrupa primero por vehículo/zona antes de invocar el IPSO por lote,
  evitando el crecimiento exponencial del espacio de permutaciones.
- Los parámetros del IPSO (`IPSOConfig`: N, T_max, c1, c2, p_c, inercia,
  umbral de estancamiento) deben calibrarse empíricamente mediante la
  experimentación numérica exigida en los requisitos no funcionales.
- Este código cubre el algoritmo **IPSO**; el informe exige un segundo
  algoritmo metaheurístico (ALNS) para la comparación experimental, no
  incluido en este entregable por no haber sido solicitado.
