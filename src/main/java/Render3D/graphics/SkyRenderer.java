package Render3D.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

import Render3D.map.LevelTypePreset;
import UtilsRender.Shader;

/**
 * Cielo 3D: un cubo unitario dibujado alrededor de la camara con un degradado
 * de dos colores (horizonte y cenit).
 *
 * <p>Sustituye al cuadrilatero a pantalla completa con espirales porque este
 * si responde a la rotacion de la camara y se puede mover de forma coherente
 * con el nivel (y con la niebla, que comparte su color de horizonte).
 *
 * <p>El cubo no usa la vista normal: el shader descarta la traslacion de la
 * matriz de vista y lo recentra en la camara, de modo que esta puede caminar
 * sin dejar el cielo atras. Por eso el cubo debe ser mayor que el frustum
 * completo; {@link #SKY_SCALE_FACTOR} lo hace holgado.
 */
public class SkyRenderer {

    /**
     * Multiplicador del far plane para el lado del cubo. A distancia d el
     * frustum ocupa medio ancho {@code d * tan(fov/2) * aspect}; con el factor
     * 1.2 el cubo sigue conteniendo el frustum aunque se mire a una esquina.
     */
    private static final float SKY_SCALE_FACTOR = 1.2f;

    /** Esquina del cubo unitario: coordenadas en [-0.5, 0.5]. */
    private static final float[][] CORNERS = {
            {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, -0.5f},
            {0.5f, 0.5f, -0.5f}, {-0.5f, 0.5f, -0.5f},
            {-0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, 0.5f},
            {0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f},
    };

    /** Caras en dos triangulos, con el orden de vertices que las mira desde dentro. */
    private static final int[][] FACES = {
            {4, 5, 6, 4, 6, 7},   // +Z
            {1, 0, 3, 1, 3, 2},   // -Z
            {5, 1, 2, 5, 2, 6},   // +X
            {0, 4, 7, 0, 7, 3},   // -X
            {7, 6, 2, 7, 2, 3},   // +Y
            {0, 1, 5, 0, 5, 4},   // -Y
    };

    private final Shader skyShader;
    private int vao;
    private int vbo;
    private int vertexCount;
    private float scale;

    public SkyRenderer() {
        skyShader = new Shader("shaders/sky_vertex.glsl", "shaders/sky_frag.glsl");
        setupCube();
    }

    private void setupCube() {
        // El buffer se dimensiona contando los vertices de verdad, no con una
        // cuenta a mano sobre las caras: asi no puede desincronizarse.
        int total = 0;
        for (int[] face : FACES) {
            total += face.length;
        }
        vertexCount = total;
        float[] vertices = new float[vertexCount * 3];
        int v = 0;
        for (int[] face : FACES) {
            for (int index : face) {
                vertices[v++] = CORNERS[index][0];
                vertices[v++] = CORNERS[index][1];
                vertices[v++] = CORNERS[index][2];
            }
        }

        vao = GL33.glGenVertexArrays();
        vbo = GL33.glGenBuffers();
        GL33.glBindVertexArray(vao);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, buffer, GL33.GL_STATIC_DRAW);
        GL33.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL33.glEnableVertexAttribArray(0);
        GL33.glBindVertexArray(0);
    }

    /**
     * Dibuja el cielo. Deja el estado de profundidad como lo encuentra para que
     * el escenario se dibuje con normalidad justo despues.
     *
     * @param preset      colores y velocidad de deriva del nivel actual.
     * @param farPlane    far plane de la proyeccion; define el tamano del cubo.
     */
    public void render(Matrix4f projection, Matrix4f view, Vector3f cameraPos,
                       float time, float farPlane, LevelTypePreset preset) {
        scale = farPlane * SKY_SCALE_FACTOR;

        // El cubo se ve desde dentro, asi que hay que garantizar que no se
        // descarte por culling aunque el juego lo tenga activado.
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Sin escribir profundidad: el cielo no debe tapar nada, solo pintar
        // donde no hay geometria. LEQUAL para que pase siempre el test.
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        skyShader.use();
        skyShader.setMat4("u_projection", projection);
        skyShader.setMat4("u_view", view);
        skyShader.setFloat("u_skyScale", scale);
        skyShader.setVec3("u_cameraPos", cameraPos);
        skyShader.setFloat("u_time", time);
        skyShader.setFloat("u_speed", preset.getSkySpeed());
        skyShader.setVec3("u_skyHorizonColor", preset.getSkyHorizonColor());
        skyShader.setVec3("u_skyZenithColor", preset.getSkyZenithColor());

        GL33.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL33.glBindVertexArray(0);

        // Devuelve el estado a como lo espera la geometria del escenario.
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDepthMask(true);
        if (cullEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    /** Lado actual del cubo (diagnostico). */
    public float getScale() {
        return scale;
    }

    public void cleanup() {
        skyShader.cleanup();
        GL33.glDeleteBuffers(vbo);
        GL33.glDeleteVertexArrays(vao);
    }
}
