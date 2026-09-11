package Render2D;

import UtilsRender.Shader;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class TextRender {

    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;
    private static final String VERTEX_SHADER = "shaders/text_vertex.glsl";
    private static final String FRAGMENT_SHADER = "shaders/text_fragment.glsl";

    private int windowWidth = 800;
    private int windowHeight = 600;

    private final Shader shader;
    private int vao, vbo;
    private int textureID;
    private STBTTBakedChar.Buffer charData;
    private Matrix4f projectionMatrix;
    private final Matrix4f modelMatrix = new Matrix4f().identity();

    public TextRender(String fontPath, int fontSize) {
        try {
            ByteBuffer fontBuffer = loadFont(fontPath);
            crearTexturaFuente(fontBuffer, fontSize);
            shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
            inicializarBuffers();
            setProjection(800, 600);
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando texto: " + e.getMessage());
        }
    }

    private void crearTexturaFuente(ByteBuffer ttf, int fontSize) {
        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
        charData = STBTTBakedChar.malloc(NUM_CHARS);
        stbtt_BakeFontBitmap(ttf, fontSize, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_W, BITMAP_H, 0,
                GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    }

    public void setProjection(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        projectionMatrix = new Matrix4f().ortho(0, width, height, 0, -1, 1);
    }

    private void inicializarBuffers() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void rendererRelativo(String texto, float relX, float relY, float r, float g, float b) {
        float x = relX * windowWidth;
        float y = relY * windowHeight;
        renderer(texto, x, y, r, g, b);
    }

    public void renderer(String texto, float x, float y, float r, float g, float b) {
        renderer(texto, x, y, r, g, b, 1f);
    }

    public void renderer(String texto, float x, float y, float r, float g, float b, float alpha) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        FloatBuffer vertices = procesarTexto(texto, x, y);

        shader.use();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        shader.setMat4("proj", projectionMatrix);
        shader.setMat4("model", modelMatrix);
        shader.setVec4("color", r, g, b, alpha);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        shader.setInt("tex", 0);

        glDrawArrays(GL_TRIANGLES, 0, vertices.remaining() / 4);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    private FloatBuffer procesarTexto(String texto, float x, float y) {
        String[] lineas = texto.split("\n");
        int totalVertices = 0;

        for (String linea : lineas) {
            totalVertices += linea.length() * 6;
        }

        FloatBuffer buffer = BufferUtils.createFloatBuffer(totalVertices * 4);
        float posY = y;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (String linea : lineas) {
                FloatBuffer posX = stack.floats(x);
                FloatBuffer posYBuf = stack.floats(posY);
                STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

                for (int i = 0; i < linea.length(); i++) {
                    char c = linea.charAt(i);
                    if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) {
                        continue;
                    }

                    stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, posX, posYBuf, quad, true);

                    buffer.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
                    buffer.put(quad.x1()).put(quad.y0()).put(quad.s1()).put(quad.t0());
                    buffer.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());

                    buffer.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());
                    buffer.put(quad.x0()).put(quad.y1()).put(quad.s0()).put(quad.t1());
                    buffer.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
                }

                posY += getLineHeight();
            }
        }
        buffer.flip();
        return buffer;
    }

    private float getLineHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0f);
            FloatBuffer y = stack.floats(0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, 'A' - FIRST_CHAR, x, y, quad, true);
            return (quad.y1() - quad.y0()) * 1.5f;
        }
    }

    private ByteBuffer loadFont(String path) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("Font not found: " + path);
        }

        try (is) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new IOException("Error loading font from classpath: " + e.getMessage(), e);
        }
    }

    public float getTextHeight(String texto) {
        float maxHeight = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0f);
            FloatBuffer y = stack.floats(0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) {
                    continue;
                }

                stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, x, y, quad, true);

                float height = quad.y1() - quad.y0();
                if (height > maxHeight) {
                    maxHeight = height;
                }
            }
        }
        return maxHeight;
    }

    public float getTextWidth(String texto) {
        float width;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0f);
            FloatBuffer y = stack.floats(0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) {
                    continue;
                }
                stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, x, y, quad, true);
            }

            width = x.get(0);
        }
        return width;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.cleanup();
        glDeleteTextures(textureID);
    }
}