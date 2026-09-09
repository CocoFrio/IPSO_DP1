package paqrap.model;

/**
 * Almacén base. PaqRap opera con un almacén central (inventario infinito,
 * abastecimiento permanente) y dos almacenes intermedios (capacidad máxima
 * 1000 unidades de "P", recarga instantánea cada 24h a las 23:59:59).
 */
public abstract class Almacen implements PuntoMapa {
    private final String idAlmacen;
    private final int posX;
    private final int posY;

    protected Almacen(String idAlmacen, int posX, int posY) {
        this.idAlmacen = idAlmacen;
        this.posX = posX;
        this.posY = posY;
    }

    public String getIdAlmacen() {
        return idAlmacen;
    }

    @Override
    public int getPosX() {
        return posX;
    }

    @Override
    public int getPosY() {
        return posY;
    }

    public abstract boolean tieneStock(int cantidad);

    public abstract void descontarStock(int cantidad);

    /** Recarga instantánea (simplificación del curso) a las 23:59:59. */
    public abstract void recargar();
}
