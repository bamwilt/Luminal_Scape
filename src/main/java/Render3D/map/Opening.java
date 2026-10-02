package Render3D.map;

import Render3D.mesh.Box;

import java.util.List;

/**
 * Plantilla base para aberturas en un muro (ventanas y puertas). Una abertura
 * se dibuja como paneles apilados a alturas configurables:
 * <ul>
 *   <li>Pared inferior (alféizar): de la base del muro hasta {@code sill}.</li>
 *   <li>Pared superior (dintel): desde {@code header} hasta el tope del muro.</li>
 * </ul>
 * El hueco entre ambas deja pasar la vista (y, si la pared inferior es mínima,
 * el paso del jugador).
 *
 * <p>Las plantillas devuelven CAJAS en coordenadas de mundo, no mallas: el
 * {@link Render3D.mesh.MeshBuilder} las vuelca todas en la misma malla de la
 * mazmorra. Al compartir sistema de coordenadas con los muros, la textura sigue
 * siendo continua a través del alféizar y el dintel (una caja con UV en
 * espacio local reiniciaría el patrón justo en la unión).
 */
public abstract class Opening {

    private static final float MINIMAL_SILL = 0.05f;

    /**
     * Genera los paneles de la abertura con la orientación dada por
     * {@code width} / {@code depth} (ancho y espesor de la celda).
     */
    public abstract List<Box> buildBoxes(float width, float depth, float centerX, float centerZ,
                                         float wallHeight);

    protected static void addBottomPanel(List<Box> panels, float width, float sill, float depth,
                                         float centerX, float centerZ) {
        if (sill < MINIMAL_SILL) {
            return;
        }
        panels.add(Box.centeredOnFloor(centerX, 0f, centerZ, width, sill, depth));
    }

    protected static void addTopPanel(List<Box> panels, float width, float header, float depth,
                                      float centerX, float centerZ, float wallHeight) {
        float height = wallHeight - header;
        if (height <= 0f) {
            return;
        }
        panels.add(Box.centeredOnFloor(centerX, header, centerZ, width, height, depth));
    }
}
