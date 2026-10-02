package Render3D.map;

import Render3D.mesh.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla de puerta: una ventana cuya pared inferior es mínima (prácticamente
 * nula) y cuya superior es ajustable, dejando todo el hueco transitable.
 * Al agacharse se pasa; de pie el dintel bloquea.
 *
 * La altura se ajusta desde MapConfig ({@link MapConfig#DOOR_HEADER_HEIGHT}).
 */
public class Door extends Opening {

    @Override
    public List<Box> buildBoxes(float width, float depth, float centerX, float centerZ,
                                float wallHeight) {
        List<Box> panels = new ArrayList<>(1);
        addTopPanel(panels, width, MapConfig.DOOR_HEADER_HEIGHT, depth, centerX, centerZ, wallHeight);
        return panels;
    }
}
