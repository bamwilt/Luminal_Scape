package Render3D.map;

import Render3D.graphics.Wall;

/**
 * Railing: una barra de muro bajo (a la altura de un pasamanos) sobre el borde
 * de una celda. Llega justo por debajo de la cámara ({@link
 * MapConfig#RAILING_HEIGHT}) y es MUCHO más fina que un muro normal
 * ({@link MapConfig#RAILING_THICKNESS}). El símbolo '◰' genera un tramo de
 * piso SIN techo (un panel por celda, ver
 * {@link DungeonManager#buildRailingFloors}) con dos de estas barras en sus
 * bordes largos (ver {@link DungeonManager#buildRailings}); se construye UNA
 * barra continua por segmento, extendida hasta los muros/puertas del cuarto.
 *
 * La barra visible NO llega a los pies del jugador, así que para que no se pase
 * se registra además un COLISIONADOR OCULTO a toda altura (como un muro
 * normal) con el MISMO contorno en planta ({@link #getCollider()}). Sus
 * colisiones van en la lista aparte del gestor de colisiones.
 */
public class Railing {

    private final Wall wall;
    private final Wall collider;

    public Railing(float width, float depth, float centerX, float centerZ, int texture) {
        wall = new Wall(width, MapConfig.RAILING_HEIGHT, depth, texture, true);
        wall.setPosition(centerX, MapConfig.RAILING_HEIGHT / 2f, centerZ);
        wall.setTexture(texture, MapConfig.TEXTURE_SCALE,
                MapConfig.TEXTURE_SCALE, true);

        collider = new Wall(width, MapConfig.WALL_HEIGHT, depth, texture, true);
        collider.setPosition(centerX, MapConfig.WALL_CENTER_Y, centerZ);
        collider.setTexture(texture, MapConfig.TEXTURE_SCALE,
                MapConfig.TEXTURE_SCALE, true);
    }

    /** Barra visible, baja (no colisiona). */
    public Wall getWall() {
        return wall;
    }

    /** Colisionador oculto a toda altura, con el mismo contorno en planta. */
    public Wall getCollider() {
        return collider;
    }

    public void cleanup() {
        wall.cleanup();
        collider.cleanup();
    }
}