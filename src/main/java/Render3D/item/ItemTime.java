package Render3D.item;

import org.joml.Vector3f;

import Render3D.graphics.PropMesh;
import Render3D.map.MapConfig;

/**
 * El item de tiempo: lo que suplanta al mapa en la dificultad facil.
 *
 * <p>En facil el minimapa ya está encendido desde el principio, asi que el item
 * de mapa no tendria nada que revelar y se cambia por este, que suma segundos al
 * reloj igual que un artefacto. Comparte con el su modelo y su escala a
 * proposito: la recompensa es la misma y un modelo distinto solo haria dudar
 * al jugador de si cuenta para el total de artefactos.
 */
public final class ItemTime extends Item {

    /** Modelo del artefacto, el mismo que usa {@link ItemKey}. */
    public static final String MODEL = ItemKey.MODEL;

    /** Misma escala que la llave: es el mismo premio que un artefacto. */
    public static final float SCALE = ItemKey.SCALE;

    private static final float BOB_AMOUNT = 0.10f;
    private static final float BOB_SPEED = 2.0f;

    public ItemTime(PropMesh mesh, Vector3f center) {
        super(mesh, center, SCALE, MapConfig.ITEM_SPIN_SPEED, BOB_AMOUNT, BOB_SPEED);
    }
}
