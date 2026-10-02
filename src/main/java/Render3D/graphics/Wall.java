package Render3D.graphics;

import Render3D.mesh.Mesh;
import Render3D.mesh.MeshBuilder;
import Render3D.mesh.UvSpace;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import UtilsRender.Shader;

import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Caja rectangular con textura, orientada al mundo mediante una matriz
 * {@code model}. Es la geometría de <b>objetos sueltos que se mueven</b>: los
 * items que rotan, que son lo único que no entra en las mallas batcheadas del
 * nivel. Muros, aberturas, pisos, techos y barandillas se acumulan en tres
 * mallas estáticas con {@link Render3D.map.WallAutotiler} y
 * {@link Render3D.mesh.MeshBuilder}.
 *
 * <h2>UVs proporcionales al tamaño de cada cara</h2>
 * El mesh se construye con un {@link MeshBuilder} en {@link UvSpace#LOCAL}: la
 * UV de cada cara se mide en el espacio local del objeto, no como un
 * (0,0)-(1,1) fijo. Para una cara de ancho {@code w} y alto {@code h}:
 *
 * <pre>
 *   U_max - U_min = w * uScale
 *   V_max - V_min = h * vScale
 * </pre>
 *
 * así un panel de 4 x 1 muestra cuatro veces más textura en U que en V, y el
 * canto de 4 x 0.4 de un piso no hereda el tiling de su cara de 4 x 4. Como
 * las UV pueden superar 1, la textura necesita {@code GL_REPEAT} (por eso
 * {@code TextureLoader} lo activa siempre y {@link #setTexture} lo vuelve a
 * fijar en cada uso).
 *
 * <p>Al medir en espacio local, la textura queda <b>pegada al objeto</b> cuando
 * rota o se desplaza. Para la geometría estática que ya vive en coordenadas de
 * mundo, el {@link MeshBuilder} usa {@link UvSpace#WORLD}, que da además
 * continuidad entre cajas contiguas.
 */
public class Wall {

    private Mesh mesh;

    private final int textureID;
    private final boolean hasTexture;
    private float[] color;
    private boolean hasLighting;

    private final float width;
    private final float height;
    private final float depth;

    // UVs: repeticiones de textura por unidad de objeto.
    private float uvScaleU;
    private float uvScaleV;

    // Transformaciones
    private final Vector3f position = new Vector3f(0, 0, 0);
    private final Vector3f rotation = new Vector3f(0, 0, 0);
    private final Vector3f scale = new Vector3f(1, 1, 1);

    private final Matrix4f modelMatrix = new Matrix4f().identity();

    public Wall(float width, float height, float depth, int textureID, boolean withLighting) {
        this(width, height, depth, textureID, withLighting,
                Render3D.map.MapConfig.UV_SCALE, Render3D.map.MapConfig.UV_SCALE);
    }

    public Wall(float width, float height, float depth, int textureID, boolean withLighting,
                float uvScaleU, float uvScaleV) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.textureID = textureID;
        this.hasTexture = true;
        this.color = new float[] {1f, 1f, 0f, 1f};
        this.hasLighting = withLighting;
        this.uvScaleU = uvScaleU;
        this.uvScaleV = uvScaleV;
        this.mesh = buildMesh();
        updateModelMatrix();
    }

    public Wall(float width, float height, float depth, float[] color, boolean withLighting) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.textureID = 0;
        this.hasTexture = false;
        this.color = color != null ? color : new float[] {1f, 1f, 0f, 1f};
        this.hasLighting = withLighting;
        this.uvScaleU = Render3D.map.MapConfig.UV_SCALE;
        this.uvScaleV = Render3D.map.MapConfig.UV_SCALE;
        this.mesh = buildMesh();
        updateModelMatrix();
    }

    private Mesh buildMesh() {
        MeshBuilder builder = new MeshBuilder();
        builder.addBoxCentered(0f, 0f, 0f, width, height, depth,
                UvSpace.LOCAL, uvScaleU, uvScaleV);
        return builder.build();
    }

    private void updateModelMatrix() {
        modelMatrix.identity()
                .translate(position)
                .rotateX(rotation.x)
                .rotateY(rotation.y)
                .rotateZ(rotation.z)
                .scale(scale);
    }

    public void render(Shader shader) {
        if (hasTexture) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureID);
            shader.setInt("textureSampler", 0);
            shader.setBool("useTexture", true);
        } else {
            shader.setVec4("objectColor", color[0], color[1], color[2], color[3]);
            shader.setBool("useTexture", false);
        }

        shader.setBool("useLighting", hasLighting);
        mesh.render(shader, modelMatrix);
    }

    public void cleanup() {
        mesh.cleanup();
    }

    // Métodos de transformación
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        updateModelMatrix();
    }

    public void translate(float x, float y, float z) {
        position.add(x, y, z);
        updateModelMatrix();
    }

    public void setRotation(float x, float y, float z) {
        rotation.set(x, y, z);
        updateModelMatrix();
    }

    public void rotate(float x, float y, float z) {
        rotation.add(x, y, z);
        updateModelMatrix();
    }

    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
        updateModelMatrix();
    }

    // Getters
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getSize() {
        return new Vector3f(width, height, depth);
    }

    /**
    // Textura y color
    /**
     * Fija la textura y su modo de repetición. No reconstruye la geometría: las
     * UV ya salen del tamaño real de cada cara, así que solo hace falta fijar el
     * wrap, que debe ser {@code GL_REPEAT} para que el world-space tiling
     * funcione.
     */
    public void setTexture(int textureID, boolean repeat) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        int wrap = repeat ? GL_REPEAT : GL_CLAMP_TO_EDGE;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void setColor(float[] color) {
        this.color = color;
    }

    public void setLightingEnabled(boolean enabled) {
        this.hasLighting = enabled;
    }

    /**
     * Cambia la densidad de textura y <b>reconstruye</b> el mesh, porque el
     * rango de UV es proporcional a la escala. Solo hay que llamarlo si se
     * quiere otra densidad distinta de {@link Render3D.map.MapConfig#UV_SCALE}.
     */
    public void setUvScale(float uvScaleU, float uvScaleV) {
        if (uvScaleU == this.uvScaleU && uvScaleV == this.uvScaleV) {
            return;
        }
        this.uvScaleU = uvScaleU;
        this.uvScaleV = uvScaleV;
        Mesh previous = this.mesh;
        this.mesh = buildMesh();
        previous.cleanup();
    }

    public float getUvScaleU() {
        return uvScaleU;
    }

    public float getUvScaleV() {
        return uvScaleV;
    }

    public int getTextureID() {
        return textureID;
    }

    public boolean hasTexture() {
        return hasTexture;
    }

    /** Malla GPU de la caja, por si hace falta dibujarla con otro shader. */
    public Mesh getMesh() {
        return mesh;
    }

}
