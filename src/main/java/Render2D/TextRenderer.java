package Render2D;

import Render3D.Shader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class TextRenderer {

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

    public TextRenderer(String fontPath, int fontSize) {
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
        rendererRelativo(texto, relX, relY, r, g, b, 1f);
    }

    public void rendererRelativo(String texto, float relX, float relY,
                                 float r, float g, float b, float alpha) {
        float x = relX * windowWidth;
        float y = relY * windowHeight;
        renderer(texto, x, y, r, g, b, alpha);
    }

    public void renderer(String texto, float x, float y, float r, float g, float b) {
        renderer(texto, x, y, r, g, b, 1f);
    }

    public void renderer(String texto, float x, float y, float r, float g, float b, float alpha) {
        renderer(texto, x, y, r, g, b, alpha, 0f);
    }

    /**
     * Dibuja texto verticalmente desplazado para que la tinta quede centrada en
     * {@code y}.
     *
     * @param correccionVertical desplazamiento que se resta al punto de dibujo;
     *                            ver {@link #drawCenteredV}
     */
    public void renderer(String texto, float x, float y, float r, float g, float b, float alpha,
                         float correccionVertical) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        FloatBuffer vertices = procesarTexto(texto, x, y - correccionVertical);

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

    /**
     * Caja vertical real de la tinta de un texto, en dos piezas.
     *
     * <p>Las glyf horneadas por stbtt NO son simetricas alrededor del punto de
     * dibujo: una 'A' va de -17 a 0 y una 'Q' de -18 a 4. Por eso el alto por si
     * solo no basta para colocar el texto, hacen falta las dos distancias: el
     * texto dibujado en {@code y} ocupa de {@code y - ascendente} a
     * {@code y + descendente}.
     */
    public static final class TextMetrics {

        public final float ascendente;
        public final float descendente;

        TextMetrics(float ascendente, float descendente) {
            this.ascendente = ascendente;
            this.descendente = descendente;
        }

        /** Alto de la tinta: de la cima de la letra alta al pie de la letra baja. */
        public float alto() {
            return descendente - ascendente;
        }

        /** Punto medio de la tinta respecto al punto de dibujo, suele ser negativo. */
        public float centroVertical() {
            return (ascendente + descendente) / 2f;
        }
    }

    /** Caja vertical de la tinta de {@code texto}. */
    public TextMetrics measureText(String texto) {
        float minY0 = 0f;
        float maxY1 = 0f;
        boolean primero = true;
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
                if (primero || quad.y0() < minY0) {
                    minY0 = quad.y0();
                }
                if (primero || quad.y1() > maxY1) {
                    maxY1 = quad.y1();
                }
                primero = false;
            }
        }
        return new TextMetrics(minY0, maxY1);
    }

    public float getTextHeight(String texto) {
        return measureText(texto).alto();
    }

    /**
     * Dibuja {@code texto} con el CENTRO de su tinta en ({@code centroX},
     * {@code centroY}).
     *
     * <p>Este es el metodo que evita que los rotulos queden picados o altos: el
     * alto de una linea no es simetrico respecto al punto de dibujo, asi que
     * calcular {@code y + (alto - altoTinta)/2} deja el texto descentrado unos
     * pocos pixels. Aqui se corrige con {@link TextMetrics#centroVertical()}.
     * @param texto
     */
    public void drawCentered(String texto, float centroX, float centroY,
                             float r, float g, float b) {
        drawCentered(texto, centroX, centroY, r, g, b, 1f);
    }

    public void drawCentered(String texto, float centroX, float centroY,
                             float r, float g, float b, float alpha) {
        TextMetrics m = measureText(texto);
        renderer(texto, centroX - getTextWidth(texto) / 2f,
                centroY - m.centroVertical(), r, g, b, alpha);
    }

    /**
     * Dibuja {@code texto} centrado horizontalmente dentro del ancho dado, con
     * la tinta centrada verticalmente en {@code centroY}.
     */
    public void drawCenteredH(String texto, float ancho, float centroY,
                              float r, float g, float b) {
        drawCentered(texto, ancho / 2f, centroY, r, g, b);
    }

    /**
     * Dibuja {@code texto} alineado a la derecha dejando {@code margen} de hueco
     * hasta el borde.
     */
    public void drawRightAligned(String texto, float margenDerecho, float y,
                                 float r, float g, float b) {
        TextMetrics m = measureText(texto);
        renderer(texto, margenDerecho - getTextWidth(texto),
                y - m.centroVertical(), r, g, b);
    }

    /**
     * Punto de dibujo para que el BORDE SUPERIOR de la tinta de {@code texto}
     * caiga en {@code y}.
     *
     * <p>Sirve para apilar lineas de arriba abajo midiendo cada una por su caja
     * real: se pregunta "donde va el tope de esta linea" y sale la base que hay
     * que pasarle al dibujo.
     */
    public float baseParaTope(float y, String texto) {
        return y - measureText(texto).ascendente;
    }

    /**
     * Dibuja {@code texto} con el BORDE SUPERIOR de su tinta en {@code y}.
     *
     * <p>Es lo que usan los bloques que se apilan de arriba abajo (HUD, menu):
     * asi el hueco que se deja a la siguiente linea es exactamente el que
     * mide el texto, y no el alto de una caja cualquiera.
     */
    public void drawTope(String texto, float x, float y, float r, float g, float b) {
        drawTope(texto, x, y, r, g, b, 1f);
    }

    public void drawTope(String texto, float x, float y, float r, float g, float b, float alpha) {
        renderer(texto, x, baseParaTope(y, texto), r, g, b, alpha);
    }

    /**
     * Dibuja {@code texto} con el BORDE INFERIOR de su tinta en {@code y}.
     *
     * <p>La otra mitad de {@link #drawTope}: para cerrar un bloque desde abajo.
     */
    public void drawBase(String texto, float x, float y, float r, float g, float b) {
        renderer(texto, x, baseParaBase(y, texto), r, g, b);
    }

    public void drawBase(String texto, float x, float y, float r, float g, float b, float alpha) {
        renderer(texto, x, baseParaBase(y, texto), r, g, b, alpha);
    }

    /**
     * Punto de dibujo para que el BORDE INFERIOR de la tinta de {@code texto}
     * caiga en {@code y}.
     */
    public float baseParaBase(float y, String texto) {
        return y - measureText(texto).descendente;
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

    /**
     * Recorta un texto para que quepa en {@code maxAncho}, con puntos suspensivos.
     *
     * <p>Un boton o una etiqueta con mas texto del que le toca no se desborda ni
     * lo tapa el vecino: se corta y se le pone {@code "..."} para que se lea que
     * habia mas. Si el propio {@code "..."} no cabe, se devuelve vacio.
     *
     * @param texto    el texto completo
     * @param maxAncho ancho disponible en pixeles de interfaz
     * @return el texto que cabe, con {@code "..."} si hizo falta cortar
     */
    public String elide(String texto, float maxAncho) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        if (maxAncho <= 0f || getTextWidth(texto) <= maxAncho) {
            return texto;
        }
        final String puntos = "...";
        float puntosW = getTextWidth(puntos);
        if (puntosW > maxAncho) {
            return "";
        }
        int n = texto.length();
        while (n > 0 && getTextWidth(texto.substring(0, n)) + puntosW > maxAncho) {
            n--;
        }
        return texto.substring(0, n) + puntos;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.cleanup();
        glDeleteTextures(textureID);
    }
}