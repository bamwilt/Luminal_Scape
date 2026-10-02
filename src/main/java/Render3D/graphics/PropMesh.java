package Render3D.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import UtilsRender.Shader;

/**
 * Malla de un modelo GLB, subida a GPU y lista para dibujar.
 *
 * <p>Un VAO por material: cada parte de un modelo tiene su color plano, asi que
 * cada parte necesita su propio uniform de color en el shader. Se dibuja
 * completando la parte con su matriz, en vez de copiar la geometria, porque hay
 * muchas mas plantas que materiales y las plantas se dibujan en el mismo sitio
 * con distinta rotacion.
 */
public final class PropMesh {

    private final GlbModel model;
    private final int[][] vaos;
    private final int[][] vbos;
    private final int[][] ebos;
    private final int[] indexCounts;
    private final float height;

    private PropMesh(GlbModel model, int[][] vaos, int[][] vbos, int[][] ebos,
                     int[] indexCounts, float height) {
        this.model = model;
        this.vaos = vaos;
        this.vbos = vbos;
        this.ebos = ebos;
        this.indexCounts = indexCounts;
        this.height = height;
    }

    /**
     * Sube un modelo a GPU.
     *
     * @return null si el modelo no se pudo leer, para que quien lo coloca pueda
     *         seguir con el resto en vez de romper el nivel.
     */
    public static PropMesh upload(GlbModel model) {
        if (model == null || !model.isSupported() || model.parts().isEmpty()) {
            return null;
        }
        int parts = model.parts().size();
        int[][] vaos = new int[parts][];
        int[][] vbos = new int[parts][];
        int[][] ebos = new int[parts][];
        int[] counts = new int[parts];

        for (int i = 0; i < parts; i++) {
            GlbModel.Part part = model.parts().get(i);
            float[] positions = model.positionsOf(part);
            float[] normals = model.normalsOf(part);
            int[] indices = model.indicesOf(part);
            int vertices = positions.length / 3;

            // Posicion y normal intercaladas, sin UV: los modelos no traen
            // coordenadas de textura.
            FloatBuffer vb = BufferUtils.createFloatBuffer(vertices * 6);
            for (int v = 0; v < vertices; v++) {
                vb.put(positions[v * 3]);
                vb.put(positions[v * 3 + 1]);
                vb.put(positions[v * 3 + 2]);
                vb.put(normals[v * 3]);
                vb.put(normals[v * 3 + 1]);
                vb.put(normals[v * 3 + 2]);
            }
            vb.flip();

            int vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            int vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vb, GL15.GL_STATIC_DRAW);

            // 0 = posicion, 1 = normal (las mismas localizaciones que el muro).
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3L * 4);

            IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
            ib.put(indices).flip();
            int ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);

            GL30.glBindVertexArray(0);

            vaos[i] = new int[] { vao };
            vbos[i] = new int[] { vbo };
            ebos[i] = new int[] { ebo };
            counts[i] = indices.length;
        }
        return new PropMesh(model, vaos, vbos, ebos, counts, model.heightMeters());
    }

    public int partCount() {
        return vaos.length;
    }

    public float heightMeters() {
        return height;
    }

    public GlbModel.Part part(int i) {
        return model.parts().get(i);
    }

    /** Dibuja el modelo completo, una llamada por material. */
    public void render(Shader shader, Matrix4f model4) {
        for (int i = 0; i < vaos.length; i++) {
            GlbModel.Part part = model.parts().get(i);
            shader.setVec3("u_propColor", part.r, part.g, part.b);
            shader.setMat4("model", model4);
            GL30.glBindVertexArray(vaos[i][0]);
            GL20.glDrawElements(GL11.GL_TRIANGLES, indexCounts[i], GL11.GL_UNSIGNED_INT, 0L);
        }
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        for (int[] vao : vaos) {
            GL30.glDeleteVertexArrays(vao[0]);
        }
        for (int[] vbo : vbos) {
            GL15.glDeleteBuffers(vbo[0]);
        }
        for (int[] ebo : ebos) {
            if (ebo.length > 0) {
                GL15.glDeleteBuffers(ebo[0]);
            }
        }
    }
}
