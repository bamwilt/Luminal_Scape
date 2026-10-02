package Render2D;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gestor de efectos de visión temporales superpuestos a la pantalla:
 * <ul>
 *   <li>{@link #showMessage}: mensaje grande centrado que aparece, se mantiene
 *       un instante y se desvanece; puede oscurecer el fondo mientras dura.</li>
 *   <li>{@link #addOverlay}: capa de color que oscurece o tiñe la vista (para
 *       "cambiar cómo se miran los colores", flashes, etc.).</li>
 *   <li>{@link #darken}: atajo para oscurecer la vista unos momentos.</li>
 * </ul>
 *
 * Todos los efectos son temporales: tienen una duración y un envolvente de
 * entrada (fade-in), meseta y salida (fade-out). Al acabar la duración se
 * eliminan solos, sin intervención externa.
 */
public class VisionManager {

    private static final String OVERLAY_VERTEX = "shaders/vision_vertex.glsl";
    private static final String OVERLAY_FRAG = "shaders/vision_frag.glsl";

    private final Shader overlayShader;
    private final int vao;
    private final int vbo;
    private final TextRender messageRenderer;

    private int screenWidth = 800;
    private int screenHeight = 600;

    private final List<Effect> effects = new ArrayList<>();

    public VisionManager(String fontPath, int messageFontSize) {
        overlayShader = new Shader(OVERLAY_VERTEX, OVERLAY_FRAG);

        float[] vertices = {-1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f};
        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();
        GL33.glBindVertexArray(vao);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, buffer, GL33.GL_STATIC_DRAW);
        GL33.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL33.glEnableVertexAttribArray(0);
        GL33.glBindVertexArray(0);

        messageRenderer = new TextRender(fontPath, messageFontSize);
    }

    public void setProjection(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        messageRenderer.setProjection(width, height);
    }

    /** Mensaje grande centrado que se desvanece solo; {@code darkenStrength} (0..1)
     *  oscurece el fondo mientras el mensaje está visible. */
    public void showMessage(String text, float r, float g, float b, float darkenStrength, float duration) {
        Effect e = new Effect();
        e.type = Effect.MESSAGE;
        e.r = r;
        e.g = g;
        e.b = b;
        e.amount = Math.min(Math.max(darkenStrength, 0f), 1f);
        e.text = text;
        e.duration = Math.max(duration, 0.1f);
        effects.add(e);
    }

    /** Capa temporal de color (tiñe/cambia la percepción del color de la vista). */
    public void addOverlay(float r, float g, float b, float amount, float duration) {
        Effect e = new Effect();
        e.type = Effect.OVERLAY;
        e.r = r;
        e.g = g;
        e.b = b;
        e.amount = Math.min(Math.max(amount, 0f), 1f);
        e.duration = Math.max(duration, 0.1f);
        effects.add(e);
    }

    /** Oscurece la vista unos momentos y se recupera sola. */
    public void darken(float amount, float duration) {
        addOverlay(0f, 0f, 0f, amount, duration);
    }

    public void update(float deltaTime) {
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.elapsed += deltaTime;
            if (e.elapsed >= e.duration) {
                it.remove();
            }
        }
    }

    public void render() {
        if (effects.isEmpty()) {
            return;
        }
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (Effect e : effects) {
            float a = envelope(e.elapsed / e.duration);
            if (e.type == Effect.OVERLAY) {
                drawOverlay(e.r, e.g, e.b, e.amount * a);
            } else {
                if (e.amount > 0.01f) {
                    drawOverlay(0f, 0f, 0f, e.amount * a);
                }
                drawMessage(e, a);
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        if (!wasBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private void drawOverlay(float r, float g, float b, float alpha) {
        if (alpha <= 0.001f) {
            return;
        }
        overlayShader.use();
        overlayShader.setVec4("uOverlayColor", r, g, b, Math.min(alpha, 1f));
        GL33.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL33.glBindVertexArray(0);
    }

    private void drawMessage(Effect e, float alpha) {
        if (e.text == null || alpha <= 0.001f) {
            return;
        }
        float w = messageRenderer.getTextWidth(e.text);
        float h = messageRenderer.getTextHeight(e.text);
        float x = (screenWidth - w) / 2f;
        float y = (screenHeight - h) / 2f;
        messageRenderer.renderer(e.text, x, y, e.r, e.g, e.b, alpha);
    }

    /** Envolvente 0..1: entra rápido, se mantiene y sale suave. */
    private static float envelope(float t) {
        float fadeIn = 0.12f;
        float fadeOut = 0.3f;
        if (t < fadeIn) {
            return t / fadeIn;
        }
        if (t > 1f - fadeOut) {
            return Math.max(0f, (1f - t) / fadeOut);
        }
        return 1f;
    }

    public void cleanup() {
        overlayShader.cleanup();
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
        messageRenderer.cleanup();
    }

    private static class Effect {

        static final int OVERLAY = 0;
        static final int MESSAGE = 1;

        int type;
        float r, g, b;
        float amount;
        String text;
        float duration;
        float elapsed;
    }
}