package Render2D.render.layers;

import org.lwjgl.opengl.GL11;

/**
 * Gestor de renderizado por capas (layered rendering).
 *
 * <p>Orden de dibujo (de abajo hacia arriba):
 * <ol>
 *   <li>{@link Layer#BASE}        - Fondo negro sólido (0,0,0,1)</li>
 *   <li>{@link Layer#SCENE_3D}    - Cielo, muros, suelo, techo, props, items</li>
 *   <li>{@link Layer#EFFECTS}     - Efectos visuales: transiciones, partículas, overlays full-screen (DEBAJO de UI_2D)</li>
 *   <li>{@link Layer#UI_2D}       - HUD, minimapa, paneles, botones, sliders, labels</li>
 *   <li>{@link Layer#POPUPS}      - ComboBox opciones, tooltips, toasts, overlays temporales</li>
 * </ol>
 */
public final class RenderLayers {

    public enum Layer {
        BASE,
        SCENE_3D,
        EFFECTS,
        UI_2D,
        POPUPS
    }

    private boolean[] layerEnabled = new boolean[Layer.values().length];
    private Layer currentLayer = null;

    public RenderLayers() {
        for (int i = 0; i < layerEnabled.length; i++) {
            layerEnabled[i] = true;
        }
    }

    public void beginFrame() {
        if (layerEnabled[Layer.BASE.ordinal()]) {
            GL11.glClearColor(0f, 0f, 0f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        } else {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }
        currentLayer = null;
    }

    public boolean beginLayer(Layer layer) {
        if (!layerEnabled[layer.ordinal()]) {
            currentLayer = layer;
            return false;
        }

        switch (layer) {
            case BASE -> {}
            case SCENE_3D -> {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);
                GL11.glDepthFunc(GL11.GL_LESS);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glCullFace(GL11.GL_BACK);
            }
            case EFFECTS, UI_2D, POPUPS -> {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }
        currentLayer = layer;
        return true;
    }

    public void endLayer(Layer layer) {
        if (currentLayer != layer) return;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        currentLayer = null;
    }

    public void toggleLayer(Layer layer) {
        layerEnabled[layer.ordinal()] = !layerEnabled[layer.ordinal()];
    }

    public boolean isLayerEnabled(Layer layer) {
        return layerEnabled[layer.ordinal()];
    }

    public void setLayerEnabled(Layer layer, boolean enabled) {
        layerEnabled[layer.ordinal()] = enabled;
    }

    public void resetAllLayers() {
        for (int i = 0; i < layerEnabled.length; i++) {
            layerEnabled[i] = true;
        }
    }

    public void cleanup() {}
}
