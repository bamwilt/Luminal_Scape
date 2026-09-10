package Render3D;

import UtilsRender.Shader;
import UtilsRender.TextureLoader;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Import3D {
    private int vao;
    private int vertexCount;
    private int textureID = -1;
    private Vector3f position;
    private Vector3f rotation;
    private Vector3f scale;
    private boolean hasTexture = false;

    public Import3D(String modelPath) {
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
        loadModel(modelPath);
    }

    public Import3D(String modelPath, String texturePath) {
        this(modelPath);
        loadTexture(texturePath);
    }

    private void loadModel(String modelPath) {
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> textureCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Leer modelo desde el classpath (compatible Windows/Linux, sin depender
        // de rutas de filesystem con caracteres no-ASCII).
        InputStream is = getClass().getClassLoader().getResourceAsStream(modelPath);
        if (is == null) {
            System.err.println("Error loading model: " + modelPath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (tokens.length == 0 || tokens[0].isEmpty()) continue;

                switch (tokens[0]) {
                    case "v":
                        // Vértice: v x y z
                        Vector3f vertex = new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3]));
                        vertices.add(vertex);
                        break;
                    case "vn":
                        // Normal: vn x y z
                        Vector3f normal = new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3]));
                        normals.add(normal);
                        break;
                    case "vt":
                        // Coordenada de textura: vt u v
                        Vector2f texCoord = new Vector2f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]));
                        textureCoords.add(texCoord);
                        break;
                    case "f":
                        // Cara: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
                        for (int i = 1; i <= 3; i++) {
                            String[] faceToken = tokens[i].split("/");
                            int vertexIndex = Integer.parseInt(faceToken[0]) - 1;
                            indices.add(vertexIndex);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading model: " + modelPath);
            e.printStackTrace();
        }

        // Convertir listas a arrays de floats para VBOs
        float[] verticesArray = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i * 3] = vertices.get(i).x;
            verticesArray[i * 3 + 1] = vertices.get(i).y;
            verticesArray[i * 3 + 2] = vertices.get(i).z;
        }

        // Crear VAO y VBOs
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Vértices
        int vboVertices = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboVertices);
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(verticesArray.length);
        verticesBuffer.put(verticesArray).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // Configurar el índice buffer (EBO) si hay índices
        if (!indices.isEmpty()) {
            int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();
            int ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesArray, GL15.GL_STATIC_DRAW);
            vertexCount = indicesArray.length;
        } else {
            vertexCount = verticesArray.length / 3;
        }

        GL30.glBindVertexArray(0);
    }

    private void loadTexture(String texturePath) {
        textureID = TextureLoader.loadTexture(texturePath);
        hasTexture = true;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
    }

    public void setScale(float uniformScale) {
        this.scale.set(uniformScale, uniformScale, uniformScale);
    }

    public void render(Shader shader) {
        // Calcular matriz de transformación
        Matrix4f modelMatrix = new Matrix4f()
                .translate(position)
                .rotateXYZ((float) Math.toRadians(rotation.x), 
                          (float) Math.toRadians(rotation.y), 
                          (float) Math.toRadians(rotation.z))
                .scale(scale);

        shader.use();
        shader.setMat4("model", modelMatrix);
        
        if (hasTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            shader.setInt("textureSampler", 0);
        }

        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);
        
        if (vertexCount > 0) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        } else {
            System.err.println("No vertices to render in Import3D model");
        }
        
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        
        if (hasTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
    }
}