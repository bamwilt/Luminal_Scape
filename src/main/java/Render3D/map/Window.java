package Render3D.map;

import Render3D.mesh.Box;

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
    public List<Box> buildBoxes(float width, float depth, float centerX, float centerZ,
                                float wallHeight) {
        List<Box> panels = new ArrayList<>(2);
        addBottomPanel(panels, width, MapConfig.WINDOW_SILL_HEIGHT, depth, centerX, centerZ);
        addTopPanel(panels, width, MapConfig.WINDOW_HEADER_HEIGHT, depth, centerX, centerZ, wallHeight);
        return panels;
    }
}
