package Render3D.map;

import Render3D.graphics.Wall;

import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla de ventana: dos paredes (inferior = alféizar, superior = dintel)
 * con alturas ajustables. El hueco queda a la altura de la vista.
 *
 * Las alturas se ajustan desde MapConfig ({@link MapConfig#WINDOW_SILL_HEIGHT}
 * y {@link MapConfig#WINDOW_HEADER_HEIGHT}).
 */
public class Window extends Opening {

    @Override
    public List<Wall> buildPanels(float width, float depth, float centerX, float centerZ,
                                  int wallTexture, float wallHeight, float textureScale) {
        List<Wall> panels = new ArrayList<>();
        addBottomPanel(panels, width, MapConfig.WINDOW_SILL_HEIGHT, depth, centerX, centerZ, wallTexture, textureScale);
        addTopPanel(panels, width, MapConfig.WINDOW_HEADER_HEIGHT, depth, centerX, centerZ, wallTexture, wallHeight, textureScale);
        return panels;
    }
}