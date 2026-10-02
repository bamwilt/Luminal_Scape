package Render2D;

import UtilsRender.Shader;
import UtilsRender.TextureLoader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Quad con textura en coordenadas de pantalla (píxeles, origen arriba
 * izquierda), al estilo de {@link QuadBatch} pero para imágenes en vez de
 * color plano. Se usa para el logo de la pantalla de título.
 *
 * <p>La altura se calcula a partir de la anchura pedida y de la proporción
 * original de la imagen ({@link #getAspectRatio()}), así el logo nunca se
 * deforma al cambiar el tamaño de la ventana.
 */
public class ImageQuad {

    private static final String VERTEX_SHADER = "shaders/image_vertex.glsl";
    private static final String FRAGMENT_SHADER = "shaders/image_frag.glsl";

    /** x, y, u, v */
    private static final int FLOATS_PER_VERTEX = 4;

    private final int textureId;
    private final float aspectRatio;

    private final Shader shader;
    private final int vao;
    private final int vbo;

    public ImageQuad(String resourcePath) {
        TextureLoader.LoadedTexture loaded = TextureLoader.loadTextureInfo(resourcePath);
        this.textureId = loaded.id();
        this.aspectRatio = loaded.width() / (float) Math.max(1, loaded.height());

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES,
                2L * Float.BYTES);
        glBindVertexArray(0);

        shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /** Proporción anchura/altura de la imagen original. */
    public float getAspectRatio() {
        return aspectRatio;
    }

    /** Altura que corresponde a una anchura dada, respetando la proporción. */
    public float heightForWidth(float width) {
        return width / aspectRatio;
    }

    /**
     * Dibuja la imagen en el rectángulo (x, y, w, h) en píxeles, con el canal
     * alpha multiplicado por {@code alpha}.
     */
    public void draw(float x, float y, float w, float h,
                     int windowWidth, int windowHeight, float alpha) {
        if (w <= 0f || h <= 0f) {
            return;
        }

        // El vertex shader coloca y=0 (arriba de la pantalla) en la parte de
        // arriba del viewport, asi que los vertices de arriba tienen que llevar
        // V=0 para mapear la PRIMERA fila de la imagen. Con V invertida la
        // imagen sale del reves.
        float[] vertices = {
            x,     y,     0f, 0f,
            x + w, y,     1f, 0f,
            x + w, y + h, 1f, 1f,
            x,     y,     0f, 0f,
            x + w, y + h, 1f, 1f,
            x,     y + h, 0f, 1f
        };

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);

        shader.use();
        shader.setVec2("uScreenSize", windowWidth, windowHeight);
        shader.setFloat("uAlpha", alpha);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        shader.setInt("uTexture", 0);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    /** Centrada horizontalmente en la ventana, con la parte superior en {@code y}. */
    public void drawCentered(float y, float w, int windowWidth, int windowHeight, float alpha) {
        draw((windowWidth - w) / 2f, y, w, heightForWidth(w), windowWidth, windowHeight, alpha);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteTextures(textureId);
        shader.cleanup();
    }
}
