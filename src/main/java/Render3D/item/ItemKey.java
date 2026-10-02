package Render3D.item;

import org.joml.Vector3f;

import Render3D.graphics.PropMesh;
import Render3D.map.MapConfig;

/**
 * La llave: el item que aparece en las celdas {@code ◈} de los mapas.
 *
 * <p>Solo declara que recurso usa y a que escala. El giro y el flotado son los
 * del mapa ({@link MapConfig#ITEM_SPIN_SPEED}), no numeros inventados aqui, para
 * que cambiar la sensacion del item se siga haciendo en un unico sitio.
 */
public final class ItemKey extends Item {

    /** Modelo del pack de items. */
    public static final String MODEL = "models/items/Key.glb";

    /**
     * El GLB mide 0,87 m en Z, casi media celda. A escala 1 la llave se perdia
     * entre la decoracion, y se subio a 2,2 para que se viera, con lo cual
     * ocupaba la celda entera y parecia un mueble. Con 1,2 mide cerca de 1 m:
     * se distingue girando, sin tapar el suelo alrededor.
     */
    public static final float SCALE = 1.2f;

    /** Amplitud del flotado: suficiente para notar que flota, no para botar. */
    private static final float BOB_AMOUNT = 0.10f;

    /** Ciclos por segundo del flotado, sin relacion con el giro. */
    private static final float BOB_SPEED = 2.0f;

    public ItemKey(PropMesh mesh, Vector3f center) {
        super(mesh, center, SCALE, MapConfig.ITEM_SPIN_SPEED, BOB_AMOUNT, BOB_SPEED);
    }
}
