package Render3D.mesh;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import UtilsRender.Shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDrawElements;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Malla estática en GPU: un VAO con su VBO de vértices (posición, normal, UV
 * intercalados) y su EBO de índices. Se crea desde un {@link MeshBuilder} y se
 * dibuja entera con una sola llamada.
 *
 * <p>Los atributos usan las mismas localizaciones que el shader de muros
 * ({@code wall_vertex.glsl}): 0 = posición, 1 = normal, 2 = UV, con un paso de
 * {@link MeshBuilder#FLOATS_PER_VERTEX} floats.
 *
 * <p>La geometría de estas mallas ya está en coordenadas de mundo, así que
 * {@link #render(Shader)} envía una matriz {@code model} identidad.
 *
 * <p>Cada malla lleva su propia textura y la liga antes de dibujarse, con los
 * uniforms {@code useTexture}/{@code useLighting} en {@code true}: como el mapa
 * entero se dibuja en tres llamadas con tres texturas distintas,omitir el bind en
 * el dibujado dejaría la última textura pegada a las tres.
 *
 * <p>La textura debe cargarse con {@code GL_REPEAT} (lo hace
 * {@link UtilsRender.TextureLoader}), porque las UV se miden en coordenadas de
 * mundo y salen con holgura del rango [0, 1].
 */
public final class Mesh {

    /** Matriz identidad compartida: la geometría de estas mallas ya está en el mundo. */
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int vertexCount;
    private final int indexCount;
    private final int texture;

    Mesh(float[] vertices, int vertexCount, int[] indices, int indexCount, int texture) {
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
        this.texture = texture;

        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        FloatBuffer vb = BufferUtils.createFloatBuffer(vertexCount * MeshBuilder.FLOATS_PER_VERTEX);
        vb.put(vertices, 0, vertexCount * MeshBuilder.FLOATS_PER_VERTEX).flip();
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(indexCount);
        ib.put(indices, 0, indexCount).flip();
        this.ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        int stride = MeshBuilder.FLOATS_PER_VERTEX * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Dibuja la malla completa en una sola llamada, ligando antes su textura.
     * La matriz {@code model} va en identidad: la geometría ya está colocada en
     * el mundo.
     */
    public void render(Shader shader) {
        if (texture != 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture);
            shader.setInt("textureSampler", 0);
        }
        shader.setBool("useTexture", texture != 0);
        shader.setBool("useLighting", true);
        render(shader, null);
    }

    /**
     * Dibuja la malla aplicando una transformación extra. Si {@code model} es
     * {@code null} se envía la identidad, que es lo correcto para la geometría
     * ya collocada en el mundo.
     */
    public void render(Shader shader, Matrix4f model) {
        shader.setMat4("model", model == null ? IDENTITY : model);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public int triangleCount() {
        return indexCount / 3;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}
