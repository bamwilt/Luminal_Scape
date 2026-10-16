package Render2D.effects;

import Render2D.components.SharedQuadMesh;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Destello a pantalla completa que se desvanece (capa EFFECTS, bajo la UI).
 *
 * <p>Es el feedback al recoger un item: un pulso breve del color elegido que
 * pasa de {@code fromAlpha} a {@code toAlpha} en {@code duration} segundos.
 */
public final class FadeEffect implements Effect {

    private final float duration;
    private final float fromAlpha;
    private final float toAlpha;
    private final int[] color;
    private float timer = 0f;

    public FadeEffect(float duration, float fromAlpha, float toAlpha,
                      float r, float g, float b) {
        this.duration = Math.max(0.0001f, duration);
        this.fromAlpha = fromAlpha;
        this.toAlpha = toAlpha;
        this.color = new int[]{(int) (r * 255f), (int) (g * 255f), (int) (b * 255f), 0};
    }

    @Override
    public void init() {
        timer = 0f;
    }

    @Override
    public boolean update(float deltaTime) {
        timer += deltaTime;
        return timer < duration;
    }

    @Override
    public void draw(int windowWidth, int windowHeight) {
        float t = Math.min(1f, timer / duration);
        float alpha = fromAlpha + (toAlpha - fromAlpha) * t;
        if (alpha <= 0.001f) {
            return;
        }
        SharedQuadMesh mesh = SharedQuadMesh.getInstance();
        if (mesh == null) {
            return;
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        mesh.draw(0, 0, windowWidth, windowHeight, windowWidth, windowHeight,
                new int[]{color[0], color[1], color[2], (int) Math.min(255f, alpha * 255f)}, 0f);
        glDisable(GL_BLEND);
    }

    @Override
    public void cleanup() {
    }
}
