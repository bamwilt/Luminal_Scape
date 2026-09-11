package Render2D;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Batch de quads de color en coordenadas de pantalla (píxeles, origen
 * arriba-izquierda). Acumula primitivas ({@link #addQuad}, {@link #addLine},
 * {@link #addTriangle}) y las dibuja TODAS en un único draw call con
 * {@link #render()}. Pensado para minimapa, paneles y fondos de pantalla.
 */
public class QuadBatch {

    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a

    private final Shader shader;
    private final int vao;
    private final int vbo;
    private final List<Float> vertices = new ArrayList<>();

    private int windowWidth = 1;
    private int windowHeight = 1;

    public QuadBatch() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2L * Float.BYTES);
        glBindVertexArray(0);
        shader = new Shader("shaders/quad_vertex.glsl", "shaders/quad_frag.glsl");
    }

    public void setProjection(int width, int height) {
        this.windowWidth = Math.max(width, 1);
        this.windowHeight = Math.max(height, 1);
    }

    public void addQuad(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (a <= 0.001f || w <= 0f || h <= 0f) {
            return;
        }
        addVertex(x, y, r, g, b, a);
        addVertex(x + w, y, r, g, b, a);
        addVertex(x + w, y + h, r, g, b, a);
        addVertex(x, y, r, g, b, a);
        addVertex(x + w, y + h, r, g, b, a);
        addVertex(x, y + h, r, g, b, a);
    }

    public void addQuadCentered(float cx, float cy, float w, float h, float r, float g, float b, float a) {
        addQuad(cx - w / 2f, cy - h / 2f, w, h, r, g, b, a);
    }

    /** Segmento grueso entre dos puntos (píxeles), útil para flechas/brújula. */
    public void addLine(float x0, float y0, float x1, float y1,
                        float thickness, float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.hypot(dx, dy);
        if (len < 1e-4f || a <= 0.001f) {
            return;
        }
        float px = -dy / len * thickness / 2f;
        float py = dx / len * thickness / 2f;
        addVertex(x0 + px, y0 + py, r, g, b, a);
        addVertex(x1 + px, y1 + py, r, g, b, a);
        addVertex(x1 - px, y1 - py, r, g, b, a);
        addVertex(x0 + px, y0 + py, r, g, b, a);
        addVertex(x1 - px, y1 - py, r, g, b, a);
        addVertex(x0 - px, y0 - py, r, g, b, a);
    }

    public void addTriangle(float x0, float y0, float x1, float y1, float x2, float y2,
                            float r, float g, float b, float a) {
        if (a <= 0.001f) {
            return;
        }
        addVertex(x0, y0, r, g, b, a);
        addVertex(x1, y1, r, g, b, a);
        addVertex(x2, y2, r, g, b, a);
    }

    private void addVertex(float x, float y, float r, float g, float b, float a) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(r);
        vertices.add(g);
        vertices.add(b);
        vertices.add(a);
    }

    public void render() {
        if (vertices.isEmpty()) {
            return;
        }
        float[] data = new float[vertices.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = vertices.get(i);
        }
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);

        shader.use();
        shader.setVec2("uScreenSize", windowWidth, windowHeight);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, vertices.size() / FLOATS_PER_VERTEX);
        glBindVertexArray(0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
        vertices.clear();
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.cleanup();
    }
}