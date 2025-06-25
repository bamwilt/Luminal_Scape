package Render2D;

import java.io.IOException;
import java.io.InputStream;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class TextRender {

    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;

    // Variables para tamaño ventana y posición relativa
    private int windowWidth = 800;
    private int windowHeight = 600;
    private float relX = 0.02f, relY = 0.05f; //

    private int shaderProgram;
    private int vao, vbo;
    private int textureID;
    private STBTTBakedChar.Buffer charData;
    private Matrix4f projectionMatrix;

    public TextRender(String fontPath, int fontSize) {
        try {
            // Cargar fuente
            ByteBuffer fontBuffer = loadFont(fontPath);
            crearTexturaFuente(fontBuffer, fontSize);
            inicializarShaders();
            inicializarBuffers();
            setProjection(800, 600); // Ejemplo inicial
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

    private void inicializarShaders() {
        String vertexShaderSource = """
                                     #version 330 core
                                     layout(location = 0) in vec4 vertex;
                                    uniform mat4 proj;
                                    uniform mat4 model;

                                     out vec2 texCoords;

                                     void main() {
                                         gl_Position = proj * model * vec4(vertex.xy, 0.0, 1.0);
                                         texCoords = vertex.zw;
                                     }
            """;

        String fragmentShaderSource = """
                                      #version 330 core
                                      in vec2 texCoords;
                                      uniform sampler2D tex;
                                      uniform vec3 color;
                                      out vec4 FragColor;
                                      void main() {
                                         float alpha = texture(tex, texCoords).r;
                                         FragColor = vec4(color, alpha);
                                      }""" // Canal alfa correcto
                ;

        shaderProgram = crearProgramaShader(vertexShaderSource, fragmentShaderSource);
    }

    public void setPosition(float x, float y) {
        modelMatrix.identity().translate(x, y, 0);
    }

    public void setScale(float sx, float sy) {
        modelMatrix.scale(sx, sy, 1);
    }

    public void setRotation(float angleDegrees) {
        float radians = (float) Math.toRadians(angleDegrees);
        modelMatrix.rotateZ(radians);
    }

    public void resetTransform() {
        modelMatrix.identity();
    }

    private Matrix4f modelMatrix = new Matrix4f().identity();

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

    public void setProjection(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        projectionMatrix = new Matrix4f().ortho(0, width, height, 0, -1, 1);
    }

    public void setRelativePosition(float relX, float relY) {
        this.relX = relX;
        this.relY = relY;
        float x = relX * windowWidth;
        float y = relY * windowHeight;
        setPosition(x, y);
    }

    public void rendererRelativo(String texto, float relX, float relY, float r, float g, float b) {
        float x = relX * windowWidth;
        float y = relY * windowHeight;
        renderer(texto, x, y, r, g, b);
    }

    public void renderer(String texto, float x, float y, float r, float g, float b) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        FloatBuffer vertices = procesarTexto(texto, x, y);

        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "proj"),
                false, projectionMatrix.get(new float[16]));

        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "model"),
                false, modelMatrix.get(new float[16]));

        glUniform3f(glGetUniformLocation(shaderProgram, "color"), r, g, b);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glUniform1i(glGetUniformLocation(shaderProgram, "tex"), 0);

        glDrawArrays(GL_TRIANGLES, 0, vertices.remaining() / 4);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    private FloatBuffer procesarTexto(String texto, float x, float y) {
        // Primero dividimos el texto en líneas
        String[] lineas = texto.split("\n");
        int totalVertices = 0;

        // Calcular el total de vértices necesarios
        for (String linea : lineas) {
            totalVertices += linea.length() * 6; // 6 vértices por carácter
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

                    // Triángulo 1
                    buffer.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
                    buffer.put(quad.x1()).put(quad.y0()).put(quad.s1()).put(quad.t0());
                    buffer.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());

                    // Triángulo 2
                    buffer.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());
                    buffer.put(quad.x0()).put(quad.y1()).put(quad.s0()).put(quad.t1());
                    buffer.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
                }

                // Calcular la altura de la línea para la siguiente
                posY += getLineHeight();
            }
        }
        buffer.flip();
        return buffer;
    }

    private float getLineHeight() {
        // Obtener la altura de un carácter de referencia (por ejemplo, 'A')
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0f);
            FloatBuffer y = stack.floats(0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, 'A' - FIRST_CHAR, x, y, quad, true);
            return (quad.y1() - quad.y0()) * 1.5f; // Añadir 50% de espacio extra
        }
    }

    private int crearProgramaShader(String vertexSource, String fragmentSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);

        // Verificar errores de compilación del vertex shader
        int[] success = new int[1];
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, success);
        if (success[0] == GL_FALSE) {
            String log = glGetShaderInfoLog(vertexShader);
            glDeleteShader(vertexShader);
            throw new RuntimeException("Error compilando vertex shader:\n" + log);
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);

        // Verificar errores de compilación del fragment shader
        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, success);
        if (success[0] == GL_FALSE) {
            String log = glGetShaderInfoLog(fragmentShader);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw new RuntimeException("Error compilando fragment shader:\n" + log);
        }

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        // Verificar errores de linking
        glGetProgramiv(program, GL_LINK_STATUS, success);
        if (success[0] == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw new RuntimeException("Error linkeando shaders:\n" + log);
        }

        // Limpiar shaders
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private ByteBuffer loadFont(String path) throws Exception {
        // Primero intentar cargar desde classpath
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is != null) {
            try {
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
                buffer.put(bytes).flip();
                return buffer;
            } finally {
                is.close();
            }
        }

        // Si no se encuentra en classpath, intentar filesystem
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                throw new IOException("Font not found: " + path);
            }

            ByteBuffer buffer = BufferUtils.createByteBuffer((int) Files.size(filePath));
            try (FileChannel fc = FileChannel.open(filePath)) {
                fc.read(buffer);
            }
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new Exception("Error loading font from filesystem: " + e.getMessage());
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
        glDeleteProgram(shaderProgram);
        glDeleteTextures(textureID);
    }
}
