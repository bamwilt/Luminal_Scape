package Render2D.components;

import Render3D.TextureLoader;
import Render3D.Shader;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ImageQuad {
    private final int texture;
    private final int vao, vbo;
    private final Shader shader;
    private final float aspectRatio;

    public ImageQuad(String texturePath) {
        texture = TextureLoader.loadTexture(texturePath);
        glBindTexture(GL_TEXTURE_2D, texture);
        IntBuffer w = BufferUtils.createIntBuffer(1), h = BufferUtils.createIntBuffer(1);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, w);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, h);
        aspectRatio = (float) w.get(0) / h.get(0);

        shader = new Shader("shaders/image_vertex.glsl", "shaders/image_frag.glsl");
        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL30.GL_DYNAMIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
    }

    public void draw(float x, float y, float w, float h, int winW, int winH, float alpha) {
        // Vértices en PÍXELES: el shader los normaliza con uScreenSize.
        float[] v = {
                x,     y,     0, 0,
                x + w, y,     1, 0,
                x + w, y + h, 1, 1,
                x,     y,     0, 0,
                x + w, y + h, 1, 1,
                x,     y + h, 0, 1
        };

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        shader.use();
        shader.setFloat("uAlpha", alpha);
        shader.setVec2("uScreenSize", winW, winH);
        glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        shader.setInt("uTexture", 0);
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, v);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        glEnable(GL_DEPTH_TEST);
    }

    public float heightForWidth(float w) { return w / aspectRatio; }
    public void cleanup() { GL30.glDeleteVertexArrays(vao); GL30.glDeleteBuffers(vbo); shader.cleanup(); }
}
