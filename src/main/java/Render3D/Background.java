package Render3D;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

/**
 * Fondo animado del exterior: un cuadrilátero a pantalla completa con un
 * shader de olas y partículas. Se dibuja sin depth test, así que las paredes
 * del nivel quedan por encima y se ve a través de ventanas y puertas.
 */
public class Background {

    private final Shader shader;
    private int vao;
    private int vbo;

    public Background() {
        this.shader = new Shader("shaders/fondo_vertex.glsl", "shaders/fondo_fragment.glsl");
        setupQuad();
    }

    private void setupQuad() {
        float[] vertices = {
            -1f,  1f,
            -1f, -1f,
             1f,  1f,
             1f, -1f,
        };

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
    }

    public void render(float time, float width, float height) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        shader.use();
        shader.setFloat("uTime", time);
        shader.setVec2("uResolution", width, height);
        GL33.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL33.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void cleanup() {
        shader.cleanup();
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
    }
}