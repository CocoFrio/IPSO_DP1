package paqrap.model;

/** Nodo de la malla vial (intersección) usado en Bloqueo.secuenciaNodos. */
public class Nodo implements PuntoMapa {
    private final int posX;
    private final int posY;

    public Nodo(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }

    @Override
    public int getPosX() {
        return posX;
    }

    @Override
    public int getPosY() {
        return posY;
    }
}
