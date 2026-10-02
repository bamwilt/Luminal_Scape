package Render2D;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Malla 2D compartida por todos los botones. Centraliza el VAO/VBO/Shader
 * común, evitando crear y destruir recursos GL por cada instancia de Button.
 */
public class SharedQuadMesh {

    private static final String VERTEX_SHADER = "shaders/button_vertex.glsl";
    private static final String FRAGMENT_SHADER = "shaders/button_frag.glsl";

    private final int vao;
    private final int vbo;
    private final Shader shader;

    public SharedQuadMesh() {
        vao = GL30.glGenVertexArrays();
        vbo = glGenBuffers();

        GL30.glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL30.glBindVertexArray(0);

        shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public void draw(float x, float y, int width, int height,
            int windowWidth, int windowHeight, int[] rgbaColor, float cornerRadius) {
        float[] vertices = new float[]{
            x, y,
            x + width, y,
            x + width, y + height,
            x, y,
            x + width, y + height,
            x, y + height
        };

        GL30.glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);

        shader.use();
        shader.setFloat("screenWidth", windowWidth);
        shader.setFloat("screenHeight", windowHeight);
        shader.setVec4(
                "buttonColor",
                rgbaColor[0] / 255f,
                rgbaColor[1] / 255f,
                rgbaColor[2] / 255f,
                rgbaColor[3] / 255f
        );
        shader.setVec2("uRectPos", x, y);
        shader.setVec2("uRectSize", width, height);
        shader.setFloat("uCornerRadius", cornerRadius);

        glDrawArrays(GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    public Shader getShader() {
        return shader;
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.cleanup();
    }
}