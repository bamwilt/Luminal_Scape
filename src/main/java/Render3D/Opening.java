package Render3D;

import java.util.ArrayList;
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
 */
public abstract class Opening {

    private static final float MINIMAL_SILL = 0.05f;

    /**
     * Genera los paneles de la abertura con la orientación dada por
     * {@code width} / {@code depth} (ancho y espesor de la celda).
     */
    public abstract List<Wall> buildPanels(float width, float depth, float centerX, float centerZ,
                                           int wallTexture, float wallHeight, float textureScale);

    protected static void addBottomPanel(List<Wall> panels, float width, float sill, float depth,
                                         float centerX, float centerZ, int wallTexture, float textureScale) {
        if (sill < MINIMAL_SILL) {
            return;
        }
        Wall bottom = new Wall(width, sill, depth, wallTexture, true);
        bottom.setPosition(centerX, sill / 2f, centerZ);
        bottom.setTexture(wallTexture, textureScale, textureScale, true);
        panels.add(bottom);
    }

    protected static void addTopPanel(List<Wall> panels, float width, float header, float depth,
                                      float centerX, float centerZ, int wallTexture,
                                      float wallHeight, float textureScale) {
        float height = wallHeight - header;
        Wall top = new Wall(width, height, depth, wallTexture, true);
        top.setPosition(centerX, header + height / 2f, centerZ);
        top.setTexture(wallTexture, textureScale, textureScale, true);
        panels.add(top);
    }
}