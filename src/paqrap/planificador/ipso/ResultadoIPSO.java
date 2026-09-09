package paqrap.planificador.ipso;

import java.util.List;
import paqrap.model.Pedido;

/** Salida del IPSO: G_best traducido a secuencia de Pedido, costo y duración. */
public class ResultadoIPSO {
    private final List<Pedido> secuenciaOptima;
    private final double costoTotal;
    private final double duracionTotalHoras;
    private final boolean cumplePlazos;

    public ResultadoIPSO(List<Pedido> secuenciaOptima, double costoTotal,
                          double duracionTotalHoras, boolean cumplePlazos) {
        this.secuenciaOptima = secuenciaOptima;
        this.costoTotal = costoTotal;
        this.duracionTotalHoras = duracionTotalHoras;
        this.cumplePlazos = cumplePlazos;
    }

    public List<Pedido> getSecuenciaOptima() {
        return secuenciaOptima;
    }

    public double getCostoTotal() {
        return costoTotal;
    }

    public double getDuracionTotalHoras() {
        return duracionTotalHoras;
    }

    public boolean isCumplePlazos() {
        return cumplePlazos;
    }
}
