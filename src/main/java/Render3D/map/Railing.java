package Render3D.map;

import Render3D.mesh.Box;

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
 * <p>La barra visible NO llega a los pies del jugador, así que para que no se
 * pase existe además un COLISIONADOR OCULTO a toda altura (como un muro normal)
 * con el MISMO contorno en planta ({@link #colliderBox}). Sus colisiones van en
 * la lista aparte del gestor de colisiones.
 *
 * <p>Ambas son cajas en coordenadas de mundo: la barra entra en la malla
 * batcheada de la mazmorra (compartiendo sistema de coordenadas, la textura
 * continúa sin costuras) y el colisionador solo aporta su {@code Aabb}.
 */
public final class Railing {

    private Railing() {
    }

    /** Barra visible, baja (no colisiona). */
    public static Box barBox(float width, float depth, float centerX, float centerZ) {
        return Box.centeredOnFloor(centerX, 0f, centerZ, width, MapConfig.RAILING_HEIGHT, depth);
    }

    /** Colisionador invisible a toda altura, con el mismo contorno en planta. */
    public static Box colliderBox(float width, float depth, float centerX, float centerZ) {
        return Box.centeredOnFloor(centerX, 0f, centerZ, width, MapConfig.WALL_HEIGHT, depth);
    }
}
