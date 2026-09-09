package paqrap.model;

/** Almacén central: abastecimiento permanente, inventario infinito. */
public class AlmacenCentral extends Almacen {

    public AlmacenCentral(String idAlmacen, int posX, int posY) {
        super(idAlmacen, posX, posY);
    }

    @Override
    public boolean tieneStock(int cantidad) {
        return true; // inventario infinito
    }

    @Override
    public void descontarStock(int cantidad) {
        // inventario infinito: no aplica descuento
    }

    @Override
    public void recargar() {
        // no aplica: nunca se agota
    }
}
