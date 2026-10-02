package Render3D.item;

import org.joml.Vector3f;

import Render3D.graphics.PropMesh;
import Render3D.map.MapConfig;

/**
 * El mapa: el item de las celdas {@code ☑}.
 *
 * <p>No da tiempo ni cuenta como artefacto. Al recogerlo se enciende el
 * minimapa y se ve la planta entera del nivel, que es justo para lo que sirve
 * un mapa. En la dificultad fácil ese mismo item se cambia por un
 * {@link ItemTime}, porque ahí el minimapa ya viene activado de salida y el mapa
 * no tendria nada que revelar.
 *
 * <p>Como {@link ItemKey}, solo declara su recurso y su escala: el giro y el
 * flotado son los del mapa, para que la sensacion de los items se cambie en un
 * unico sitio.
 */
public final class ItemMap extends Item {

    /** Modelo del pergamino, del pack de items de fantasia. */
    public static final String MODEL = "models/items/Parchment.glb";

    /**
     * El GLB mide 0,9 m de lado, igual que la llave, asi que se deja a 1,2
     * para que ambos items ocupen la misma celda y se distinguan por la forma
     * en lugar de por el tamano.
     */
    public static final float SCALE = 1.2f;

    /** El pergamino es una hoja plana: gira mas despacio que la llave. */
    private static final float SPIN_SPEED = MapConfig.ITEM_SPIN_SPEED * 0.6f;

    /** El flotado es mas marcado, para que una hoja se lea y no parezca un mueble. */
    private static final float BOB_AMOUNT = 0.14f;
    private static final float BOB_SPEED = 1.6f;

    public ItemMap(PropMesh mesh, Vector3f center) {
        super(mesh, center, SCALE, SPIN_SPEED, BOB_AMOUNT, BOB_SPEED);
    }
}
