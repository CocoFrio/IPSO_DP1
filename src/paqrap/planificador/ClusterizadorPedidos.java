package paqrap.planificador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import paqrap.model.Almacen;
import paqrap.model.Pedido;
import paqrap.model.PuntoMapa;
import paqrap.model.Vehiculo;

/**
 * Agrupa los pedidos pendientes en lotes por vehículo, respetando:
 *  - la capacidad máxima de paquetes de cada vehículo (24/8/4),
 *  - la disponibilidad de stock del almacén asignado (central o intermedio),
 *  - la prioridad de atención según el plazo comprometido (semáforo).
 *
 * Esta clusterización previa es la que recomienda el informe de selección
 * de algoritmos para instancias de más de 30-50 paradas ("agrupar primero
 * los destinos por zonas... y ejecutar el IPSO por cuadrantes"): aquí cada
 * lote resultante es exactamente la instancia de TSP que resolverá el
 * IPSOOptimizador para un vehículo.
 */
public class ClusterizadorPedidos {

    public static class LoteVehiculo {
        public final Vehiculo vehiculo;
        public final Almacen almacen;
        public final List<Pedido> pedidos = new ArrayList<>();

        public LoteVehiculo(Vehiculo vehiculo, Almacen almacen) {
            this.vehiculo = vehiculo;
            this.almacen = almacen;
        }
    }

    public List<LoteVehiculo> clusterizar(List<Pedido> pedidosPendientes,
                                           List<Vehiculo> flotaDisponible,
                                           List<Almacen> almacenes,
                                           LocalDateTime instanteReferencia) {

        List<Pedido> pendientes = new ArrayList<>(pedidosPendientes);
        // Prioridad: pedidos con plazo límite más próximo primero (semáforo ámbar/rojo primero)
        pendientes.sort(Comparator.comparing(Pedido::calcularFechaLimite));

        List<LoteVehiculo> lotes = new ArrayList<>();

        for (Vehiculo vehiculo : flotaDisponible) {
            if (pendientes.isEmpty() || !vehiculo.isDisponible()) {
                continue;
            }

            Almacen almacenAsignado = almacenMasCercanoConStock(vehiculo, almacenes, pendientes.get(0));
            if (almacenAsignado == null) {
                continue; // ningún almacén tiene stock disponible en este instante
            }

            LoteVehiculo lote = new LoteVehiculo(vehiculo, almacenAsignado);
            int cargaAcumulada = 0;
            PuntoMapa referenciaCercania = almacenAsignado;

            while (true) {
                Pedido candidato = siguienteMasCercanoQueQuepa(
                        pendientes, cargaAcumulada, vehiculo, referenciaCercania, almacenAsignado);
                if (candidato == null) {
                    break;
                }
                lote.pedidos.add(candidato);
                pendientes.remove(candidato);
                cargaAcumulada += candidato.getCantidadSolicitada();
                referenciaCercania = candidato;
            }

            if (!lote.pedidos.isEmpty()) {
                almacenAsignado.descontarStock(cargaAcumulada);
                lotes.add(lote);
            }
        }

        return lotes;
    }

    private Pedido siguienteMasCercanoQueQuepa(List<Pedido> pendientes, int cargaAcumulada,
                                                Vehiculo vehiculo, PuntoMapa referencia, Almacen almacen) {
        Pedido mejor = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (Pedido p : pendientes) {
            int cargaSiSeAgrega = cargaAcumulada + p.getCantidadSolicitada();
            if (cargaSiSeAgrega > vehiculo.getCapacidad()) {
                continue;
            }
            if (!almacen.tieneStock(cargaSiSeAgrega)) {
                continue;
            }
            double dist = distanciaManhattan(p, referencia);
            if (dist < mejorDistancia) {
                mejorDistancia = dist;
                mejor = p;
            }
        }
        return mejor;
    }

    private Almacen almacenMasCercanoConStock(Vehiculo vehiculo, List<Almacen> almacenes, Pedido referencia) {
        Almacen mejor = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (Almacen a : almacenes) {
            if (!a.tieneStock(referencia.getCantidadSolicitada())) {
                continue;
            }
            double dist = distanciaManhattan(a, vehiculo);
            if (dist < mejorDistancia) {
                mejorDistancia = dist;
                mejor = a;
            }
        }
        return mejor;
    }

    private double distanciaManhattan(PuntoMapa a, PuntoMapa b) {
        return Math.abs(a.getPosX() - b.getPosX()) + Math.abs(a.getPosY() - b.getPosY());
    }
}
