package Render3D.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import UtilsRender.Shader;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class Wall {

    // Buffers
    private int vao;
    private int vbo;
    private int ebo;
    private int instanceVBO; // Buffer para instancias

    // Propiedades
    private int textureID;
    private boolean hasTexture;
    private boolean hasLighting;
    private float[] color;
    private float width;
    private float height;
    private float depth;

    // Transformaciones
    private Vector3f position = new Vector3f(0, 0, 0);
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f(1, 1, 1);

    private Matrix4f modelMatrix = new Matrix4f().identity();
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    // Textura
    private float textureScaleX = 1.0f;
    private float textureScaleY = 1.0f;

    // Instanced rendering
    private List<Matrix4f> instanceMatrices = new ArrayList<>();
    private boolean isInstanced = false;
    private int instanceCount = 0;

    // Constructores
    public Wall(float width, float height, float depth, int textureID, boolean withLighting) {
        init(width, height, depth, new float[]{1f, 1f, 0f, 1f}, withLighting);
        this.textureID = textureID;
        this.hasTexture = true;
    }

    public Wall(float width, float height, float depth, float[] color, boolean withLighting) {
        init(width, height, depth, color != null ? color : new float[]{1f, 1f, 0f, 1f}, withLighting);
        this.hasTexture = false;
        this.textureID = 0;
    }

    private void init(float width, float height, float depth, float[] color, boolean withLighting) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.color = color;
        this.hasLighting = withLighting;
        setupMesh();
        updateModelMatrix();
    }

    private void setupMesh() {
        float w = width / 2f;
        float h = height / 2f;
        float d = depth / 2f;

        float[] vertices = {
            // Posición           Normal              UV
            // Cara frontal (z+)
            -w, -h, d, 0f, 0f, 1f, 0f, 0f,
            w, -h, d, 0f, 0f, 1f, textureScaleX, 0f,
            w, h, d, 0f, 0f, 1f, textureScaleX, textureScaleY,
            -w, h, d, 0f, 0f, 1f, 0f, textureScaleY,
            // Cara trasera (z-)
            w, -h, -d, 0f, 0f, -1f, 0f, 0f,
            -w, -h, -d, 0f, 0f, -1f, textureScaleX, 0f,
            -w, h, -d, 0f, 0f, -1f, textureScaleX, textureScaleY,
            w, h, -d, 0f, 0f, -1f, 0f, textureScaleY,
            // Cara superior (y+)
            -w, h, d, 0f, 1f, 0f, 0f, 0f,
            w, h, d, 0f, 1f, 0f, textureScaleX, 0f,
            w, h, -d, 0f, 1f, 0f, textureScaleX, textureScaleY,
            -w, h, -d, 0f, 1f, 0f, 0f, textureScaleY,
            // Cara inferior (y-)
            -w, -h, -d, 0f, -1f, 0f, 0f, 0f,
            w, -h, -d, 0f, -1f, 0f, textureScaleX, 0f,
            w, -h, d, 0f, -1f, 0f, textureScaleX, textureScaleY,
            -w, -h, d, 0f, -1f, 0f, 0f, textureScaleY,
            // Cara derecha (x+)
            w, -h, d, 1f, 0f, 0f, 0f, 0f,
            w, -h, -d, 1f, 0f, 0f, textureScaleX, 0f,
            w, h, -d, 1f, 0f, 0f, textureScaleX, textureScaleY,
            w, h, d, 1f, 0f, 0f, 0f, textureScaleY,
            // Cara izquierda (x-)
            -w, -h, -d, -1f, 0f, 0f, 0f, 0f,
            -w, -h, d, -1f, 0f, 0f, textureScaleX, 0f,
            -w, h, d, -1f, 0f, 0f, textureScaleX, textureScaleY,
            -w, h, -d, -1f, 0f, 0f, 0f, textureScaleY
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0, // frontal
            4, 5, 6, 6, 7, 4, // trasera
            8, 9, 10, 10, 11, 8, // superior
            12, 13, 14, 14, 15, 12, // inferior
            16, 17, 18, 18, 19, 16, // derecha
            20, 21, 22, 22, 23, 20 // izquierda
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        // Atributos estándar
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Configurar instanced rendering si hay instancias
        if (!instanceMatrices.isEmpty()) {
            setupInstanceBuffer();
        }

        glBindVertexArray(0);
    }

    private void setupInstanceBuffer() {
        instanceVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        updateInstanceBuffer();
        
        glBindVertexArray(vao);
        
        // Atributos para matriz de instancia (4 vectores de 4 floats)
        for (int i = 0; i < 4; i++) {
            int attribLocation = 3 + i;
            glEnableVertexAttribArray(attribLocation);
            glVertexAttribPointer(attribLocation, 4, GL_FLOAT, false, 16 * Float.BYTES, i * 16);
            glVertexAttribDivisor(attribLocation, 1);
        }
        
        glBindVertexArray(0);
        isInstanced = true;
    }

    private void updateInstanceBuffer() {
        if (instanceMatrices.isEmpty()) return;
        
        FloatBuffer buffer = BufferUtils.createFloatBuffer(instanceCount * 16);
        for (Matrix4f matrix : instanceMatrices) {
            matrix.get(buffer);
        }
        buffer.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
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
        // Configurar shader
        if (!isInstanced || instanceCount == 0) {
            // Renderizado normal para una instancia
            modelMatrix.get(matrixBuffer);
            shader.setMat4("model", matrixBuffer);
        }
        
        // Textura
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
        shader.setBool("useInstance", isInstanced && instanceCount > 0);

        // Renderizado
        glBindVertexArray(vao);
        if (isInstanced && instanceCount > 0) {
            glDrawElementsInstanced(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0, instanceCount);
        } else {
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
        }
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        if (isInstanced) {
            glDeleteBuffers(instanceVBO);
        }
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

    // Métodos para instanced rendering
    public void addInstance(Matrix4f modelMatrix) {
        instanceMatrices.add(new Matrix4f(modelMatrix));
        instanceCount = instanceMatrices.size();
        
        if (isInstanced) {
            updateInstanceBuffer();
        } else {
            setupInstanceBuffer();
        }
    }

    public void clearInstances() {
        instanceMatrices.clear();
        instanceCount = 0;
        if (isInstanced) {
            glDeleteBuffers(instanceVBO);
            isInstanced = false;
        }
    }

    // Getters
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getSize() {
        return new Vector3f(width, height, depth);
    }

    // Textura y color
    public void setTexture(int textureID, float scaleX, float scaleY, boolean repeat) {
        this.textureID = textureID;
        this.hasTexture = true;
        this.textureScaleX = scaleX;
        this.textureScaleY = scaleY;

        glBindTexture(GL_TEXTURE_2D, textureID);
        if (repeat) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        updateTextureCoords();
    }

    public void setColor(float[] color) {
        this.color = color;
        this.hasTexture = false;
    }

    public void setLightingEnabled(boolean enabled) {
        this.hasLighting = enabled;
    }

    public void setTextureScale(float scaleX, float scaleY) {
        this.textureScaleX = scaleX;
        this.textureScaleY = scaleY;
        updateTextureCoords();
    }

    private void updateTextureCoords() {
        if (!hasTexture) return;

        // Regenerar geometría con nuevas coordenadas UV
        cleanup();
        setupMesh();
        if (isInstanced) {
            setupInstanceBuffer();
        }
    }

    public int getTextureID() {
        return textureID;
    }
    
    public boolean isInstanced() {
        return isInstanced && instanceCount > 0;
    }
}