package Render3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla de puerta: una ventana cuya pared inferior es mínima (prácticamente
 * nula) y cuya superior es ajustable, dejando todo el hueco transitable.
 * Al agacharse se pasa; de pie el dintel bloquea.
 *
 * La altura se ajusta desde DungeonManager ({@link DungeonManager#DOOR_HEADER_HEIGHT}).
 */
public class Door extends Opening {

    @Override
    public List<Wall> buildPanels(float width, float depth, float centerX, float centerZ,
                                  int wallTexture, float wallHeight, float textureScale) {
        List<Wall> panels = new ArrayList<>();
        addTopPanel(panels, width, DungeonManager.DOOR_HEADER_HEIGHT, depth, centerX, centerZ, wallTexture, wallHeight, textureScale);
        return panels;
    }
}