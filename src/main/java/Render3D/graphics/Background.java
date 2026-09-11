package Render3D.graphics;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

/**
 * Fondo animado del exterior: un cuadrilátero a pantalla completa con un
 * shader de olas y partículas. Se dibuja sin depth test, así que las paredes
 * del nivel quedan por encima y se ve a través de ventanas y puertas.
 *
 * Hay varias variantes del mismo shader de espirales (dorado, azul, rojo y
 * verde) que se eligen según el nivel actual; al avanzar de nivel van
 * iterando.
 */
public class Background {

    private final Shader[] shaders;
    private int vao;
    private int vbo;

    public Background() {
        shaders = new Shader[4];
        shaders[0] = new Shader("shaders/fondo_vertex.glsl", "shaders/fondo_fragment_dorado.glsl");
        shaders[1] = new Shader("shaders/fondo_vertex.glsl", "shaders/fondo_fragment_azul.glsl");
        shaders[2] = new Shader("shaders/fondo_vertex.glsl", "shaders/fondo_fragment_rojo.glsl");
        shaders[3] = new Shader("shaders/fondo_vertex.glsl", "shaders/fondo_fragment_verde.glsl");
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

    /** Número de estilos de fondo disponibles. */
    public int getStyleCount() {
        return shaders.length;
    }

    public void render(float time, float width, float height, int style) {
        int index = ((style % shaders.length) + shaders.length) % shaders.length;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        shaders[index].use();
        shaders[index].setFloat("uTime", time);
        shaders[index].setVec2("uResolution", width, height);
        GL33.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL33.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void cleanup() {
        for (Shader shader : shaders) {
            shader.cleanup();
        }
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
    }
}