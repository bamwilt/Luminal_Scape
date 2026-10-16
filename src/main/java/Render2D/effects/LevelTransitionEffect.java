package Render2D.effects;

import Render2D.components.TextRenderer;
import Render2D.components.SharedQuadMesh;
import static org.lwjgl.opengl.GL11.*;

/**
 * Transición de nivel: rectángulo full-width con texto que se desvanece.
 * Se dibuja en la capa EFFECTS (debajo de UI_2D).
 */
public final class LevelTransitionEffect implements Effect {
    private final TextRenderer text;
    private final String levelName;
    private final int w, h;
    private float timer = 0f;
    private final float fadeIn = 0.4f, hold = 1.2f, fadeOut = 0.8f;
    private final float total;
    private final float barH = 60f;
    private final int barColor = 0x000000CC;

    public LevelTransitionEffect(TextRenderer text, String levelName, int w, int h) {
        this.text = text; this.levelName = levelName; this.w = w; this.h = h;
        this.total = fadeIn + hold + fadeOut;
    }

    @Override public void init() { timer = 0f; }

    @Override public boolean update(float dt) {
        timer += dt;
        return timer < total;
    }

    @Override public void draw(int ww, int hh) {
        float a = alpha();
        if (a <= 0) return;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float y = (hh - barH) / 2f;
        drawRect(0, y, ww, barH, barColor, a);

        String t = levelName.toUpperCase();
        float tw = text.getTextWidth(t);
        float tx = (ww - tw) / 2f;
        float ty = y + (barH + text.measureText(t).alto()) / 2f - 2f;
        text.drawBase(t, tx, ty, 1, 1, 1, a);

        glDisable(GL_BLEND);
    }

    private float alpha() {
        if (timer < fadeIn) return timer / fadeIn;
        if (timer < fadeIn + hold) return 1f;
        if (timer < total) return 1f - (timer - fadeIn - hold) / fadeOut;
        return 0f;
    }

    private void drawRect(float x, float y, float w, float h, int color, float a) {
        SharedQuadMesh mesh = SharedQuadMesh.getInstance();
        if (mesh == null) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int alpha = (int) (((color >> 24) & 0xFF) * a);
        mesh.draw(x, y, w, h, this.w, this.h, new int[]{r, g, b, alpha}, 0f);
    }

    @Override public void cleanup() {}
}
