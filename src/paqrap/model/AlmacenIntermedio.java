package paqrap.model;

/**
 * Almacén intermedio: capacidad máxima de 1000 unidades del producto "P",
 * se recarga cada 24 horas (23:59:59). Para el curso se asume recarga
 * instantánea.
 */
public class AlmacenIntermedio extends Almacen {

    public static final int CAPACIDAD_MAXIMA = 1000;

    private int stockActual;

    public AlmacenIntermedio(String idAlmacen, int posX, int posY) {
        super(idAlmacen, posX, posY);
        this.stockActual = CAPACIDAD_MAXIMA;
    }

    @Override
    public boolean tieneStock(int cantidad) {
        return stockActual >= cantidad;
    }

    @Override
    public void descontarStock(int cantidad) {
        if (cantidad > stockActual) {
            throw new IllegalStateException(
                    "Stock insuficiente en almacén intermedio " + getIdAlmacen());
        }
        stockActual -= cantidad;
    }

    @Override
    public void recargar() {
        stockActual = CAPACIDAD_MAXIMA;
    }

    public int getStockActual() {
        return stockActual;
    }
}
