package Core;

import Render2D.components.SharedQuadMesh;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Fundido a negro a pantalla completa.
 *
 * <p>Se dibuja en la capa POPUPS, por encima de todo, para que sirva tanto de
 * transicion entre pantallas (titulo &lt;-&gt; partida) como de revelado del panel
 * de resultado (victoria/derrota/fin). No es un {@code Effect} porque esos viven
 * en la capa EFFECTS, por debajo de la UI, y aqui hace falta tapar tambien los
 * menus.
 */
public final class ScreenFade {

    private float timer = 1f;
    private float duration = 0f;
    private float fromAlpha = 0f;
    private float toAlpha = 0f;

    /** Revela: de negro opaco a transparente (al entrar en una pantalla). */
    public void fadeIn(float segundos) {
        fromAlpha = 1f;
        toAlpha = 0f;
        duration = Math.max(0.0001f, segundos);
        timer = 0f;
    }

    /** Oscurece: de transparente a negro opaco (al salir de una pantalla). */
    public void fadeOut(float segundos) {
        fromAlpha = 0f;
        toAlpha = 1f;
        duration = Math.max(0.0001f, segundos);
        timer = 0f;
    }

    public void update(float deltaTime) {
        if (timer < duration) {
            timer += deltaTime;
        }
    }

    public boolean isActive() {
        return timer < duration;
    }

    public void draw(int windowWidth, int windowHeight) {
        if (!isActive()) {
            return;
        }
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
                new int[]{0, 0, 0, (int) Math.min(255f, alpha * 255f)}, 0f);
        glDisable(GL_BLEND);
    }
}
