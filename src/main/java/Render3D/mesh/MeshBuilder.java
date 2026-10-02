package Render3D.mesh;

import java.util.Arrays;

/**
 * Acumulador de geometría: posiciones, normales y UVs de todas las cajas que
 * compondrán una misma malla. Al terminar se sube una sola vez a un
 * {@link Mesh} (un único VAO/VBO/EBO), de modo que un mapa entero se dibuja
 * con <b>una</b> llamada a {@code glDrawElements} en lugar de una por objeto.
 *
 * <h2>World-space UV tiling</h2>
 * Cada cara calcula su UV a partir del tamaño real de esa cara, nunca de un
 * (0,0)-(1,1) fijo:
 *
 * <pre>
 *   U_max - U_min = anchoDeCara * u_scale
 *   V_max - V_min = altoDeCara  * v_scale
 * </pre>
 *
 * La UV se proyecta sobre el plano de la cara tomando los dos ejes que la
 * DEFINEN, es decir los que no son el de su normal:
 *
 * <table>
 *   <caption>Proyección por normal</caption>
 *   <tr><th>Normal</th><th>U</th><th>V</th></tr>
 *   <tr><td>±X</td><td>Z</td><td>Y</td></tr>
 *   <tr><td>±Y</td><td>X</td><td>Z</td></tr>
 *   <tr><td>±Z</td><td>X</td><td>Y</td></tr>
 * </table>
 *
 * Con {@link UvSpace#WORLD} esas coordenadas son las posiciones del vértice, de
 * modo que el rango absoluto de la UV es proporcional al tamaño de la cara Y dos
 * cajas que comparten cara quedan con la textura alineada (sin costuras ni
 * reinicios de patrón en las uniones). Como las UV pueden pasar de 1, la
 * textura debe cargarse con {@code GL_REPEAT}.
 *
 * <p>No es thread-safe: se construye en el hilo que carga el nivel y luego se
 * descarta.
 */
public final class MeshBuilder {

    /** posición(3) + normal(3) + uv(2). Coincide con los atributos del shader. */
    public static final int FLOATS_PER_VERTEX = 8;

    private static final int INITIAL_VERTICES = 512;

    private float[] vertices = new float[INITIAL_VERTICES * FLOATS_PER_VERTEX];
    private int vertexCount;

    private int[] indices = new int[INITIAL_VERTICES * 6];
    private int indexCount;

    /** Reutilizado por {@link #addFace} para no reservar por cara. */
    private final float[] corners = new float[12];

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /**
     * Añade una caja alineada a los ejes entre los puntos opuestos
     * {@code (minX,minY,minZ)} y {@code (maxX,maxY,maxZ)}.
     *
     * @param uvSpace  de dónde se miden las UV (ver {@link UvSpace})
     * @param uScale   repeticiones de textura por unidad de mundo en U
     * @param vScale   repeticiones de textura por unidad de mundo en V
     */
    public void addBox(float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ,
                       UvSpace uvSpace, float uScale, float vScale) {
        float ox = 0f;
        float oy = 0f;
        float oz = 0f;
        if (uvSpace == UvSpace.LOCAL) {
            // Medir desde el centro de la caja: la UV queda pegada al objeto
            // cuando la matriz model lo mueve o lo rota.
            ox = (minX + maxX) * 0.5f;
            oy = (minY + maxY) * 0.5f;
            oz = (minZ + maxZ) * 0.5f;
        }
        addBoxUv(minX, minY, minZ, maxX, maxY, maxZ, ox, oy, oz, uScale, vScale);
    }

    /** Caja definida por su centro y sus dimensiones. */
    public void addBoxCentered(float centerX, float centerY, float centerZ,
                               float sizeX, float sizeY, float sizeZ,
                               UvSpace uvSpace, float uScale, float vScale) {
        float hx = sizeX * 0.5f;
        float hy = sizeY * 0.5f;
        float hz = sizeZ * 0.5f;
        addBox(centerX - hx, centerY - hy, centerZ - hz,
                centerX + hx, centerY + hy, centerZ + hz,
                uvSpace, uScale, vScale);
    }

    /**
     * Igual que {@link #addBox} pero con un origen de UV explícito, para que una
     * malla construida en espacio local pueda alinearse con la geometría del
     * mundo (pasa el centro en XZ y la base en Y del objeto).
     */
    public void addBox(float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ,
                       float originX, float originY, float originZ,
                       float uScale, float vScale) {
        addBoxUv(minX, minY, minZ, maxX, maxY, maxZ, originX, originY, originZ, uScale, vScale);
    }

    /** Añade una caja definida por su centro en planta y su altura, apoyada en la base. */
    public void addBoxOnFloor(float centerX, float baseY, float centerZ,
                              float width, float height, float depth,
                              float uScale, float vScale) {
        addBoxWorldUv(centerX - width * 0.5f, baseY, centerZ - depth * 0.5f,
                centerX + width * 0.5f, baseY + height, centerZ + depth * 0.5f,
                uScale, vScale);
    }

    /** Añade una caja ya construida, con UV en coordenadas de mundo. */
    public void addBox(Box box, float uScale, float vScale) {
        addBoxWorldUv(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, uScale, vScale);
    }

    /**
     * Igual que {@link #addBox} pero sin rango de origen configurable: la UV se
     * mide siempre en coordenadas de mundo. Es la forma que usan las mallas
     * estáticas de la mazmorra.
     */
    public void addBoxWorldUv(float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ,
                              float uScale, float vScale) {
        addBoxUv(minX, minY, minZ, maxX, maxY, maxZ, 0f, 0f, 0f, uScale, vScale);
    }

    public boolean isEmpty() {
        return indexCount == 0;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    /** Tamaño en triángulos, para estadísticas. */
    public int triangleCount() {
        return indexCount / 3;
    }

    /** Libera la memoria acumulada (el {@link Mesh} ya es independiente). */
    public void clear() {
        vertexCount = 0;
        indexCount = 0;
    }

    /**
     * Concatena al final de este builder toda la geometría acumulada en
     * {@code source}, rebasando sus índices para que apunten a los vértices
     * recién copiados. Sirve para que el autotiler y el resto de piezas
     * estáticas del nivel acaben en una única malla por textura:
     *
     * <pre>{@code destino.appendFrom(origen);   // destino += origen}</pre>
     */
    public void appendFrom(MeshBuilder source) {
        if (source == this) {
            return;
        }
        int sourceVertices = source.vertexCount;
        int sourceIndices = source.indexCount;
        int baseVertex = vertexCount;

        ensureVertices(baseVertex + sourceVertices);
        System.arraycopy(source.vertices, 0, vertices, baseVertex * FLOATS_PER_VERTEX,
                sourceVertices * FLOATS_PER_VERTEX);
        vertexCount = baseVertex + sourceVertices;

        ensureIndices(indexCount + sourceIndices);
        for (int i = 0; i < sourceIndices; i++) {
            indices[indexCount + i] = source.indices[i] + baseVertex;
        }
        indexCount += sourceIndices;
    }

    /**
     * Copia de los vértices acumulados en formato
     * {@code [x, y, z, nx, ny, nz, u, v]} por vértice. Pensado para pruebas y
     * depuración: no hace falta contexto de OpenGL para inspeccionar la
     * geometría antes de subirla.
     */
    public float[] vertexData() {
        return Arrays.copyOf(vertices, vertexCount * FLOATS_PER_VERTEX);
    }

    /** Copia de los índices acumulados. */
    public int[] indexData() {
        return Arrays.copyOf(indices, indexCount);
    }

    /**
     * Sube la geometría acumulada a un VAO/VBO/EBO nuevo. Devuelve {@code null}
     * si no se añadió nada, para no crear buffers vacíos.
     */
    /**
     * Sube la geometría acumulada a la GPU y la asocia a {@code texture}. Se
     * puede llamar aunque el builder esté vacío: devuelve {@code null} para no
     * crear un VAO sin geometría.
     */
    public Mesh build(int texture) {
        if (isEmpty()) {
            return null;
        }
        return new Mesh(vertexData(), vertexCount, indexData(), indexCount, texture);
    }

    /** Como {@link #build(int)}, pero sin asociar textura. */
    public Mesh build() {
        if (isEmpty()) {
            return null;
        }
        return new Mesh(vertexData(), vertexCount, indexData(), indexCount, 0);
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private void addBoxUv(float minX, float minY, float minZ,
                          float maxX, float maxY, float maxZ,
                          float originX, float originY, float originZ,
                          float uScale, float vScale) {
        // Orden de despliegue anti-horario visto desde fuera (regla de la
        // mano derecha): la normal de cada cara sale de (v1-v0) x (v2-v0).
        //
        // +Z
        if (minZ != maxZ) {
            setCorners(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
            addFace(2, originX, originY, originZ, uScale, vScale);
        }
        // -Z
        if (minZ != maxZ) {
            setCorners(maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
            addFace(2, originX, originY, originZ, uScale, vScale);
        }
        // +Y
        if (minY != maxY) {
            setCorners(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ);
            addFace(1, originX, originY, originZ, uScale, vScale);
        }
        // -Y
        if (minY != maxY) {
            setCorners(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
            addFace(1, originX, originY, originZ, uScale, vScale);
        }
        // +X
        if (minX != maxX) {
            setCorners(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
            addFace(0, originX, originY, originZ, uScale, vScale);
        }
        // -X
        if (minX != maxX) {
            setCorners(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            addFace(0, originX, originY, originZ, uScale, vScale);
        }
    }

    private void setCorners(float ax, float ay, float az,
                            float bx, float by, float bz,
                            float cx, float cy, float cz,
                            float dx, float dy, float dz) {
        corners[0] = ax;
        corners[1] = ay;
        corners[2] = az;
        corners[3] = bx;
        corners[4] = by;
        corners[5] = bz;
        corners[6] = cx;
        corners[7] = cy;
        corners[8] = cz;
        corners[9] = dx;
        corners[10] = dy;
        corners[11] = dz;
    }

    /**
     * Vuelca un cuadrilátero como dos triángulos. {@code faceAxis} es el eje
     * de la normal (0=X, 1=Y, 2=Z) y determina sobre qué par de ejes se proyecta
     * la UV.
     */
    private void addFace(int faceAxis, float originX, float originY, float originZ,
                         float uScale, float vScale) {
        float[] normal = FACE_NORMALS[faceAxis];
        int base = vertexCount;

        for (int i = 0; i < 4; i++) {
            float px = corners[i * 3];
            float py = corners[i * 3 + 1];
            float pz = corners[i * 3 + 2];
            pushVertex(px, py, pz, normal[0], normal[1], normal[2],
                    projectedU(faceAxis, px, py, pz, originX, originY, originZ) * uScale,
                    projectedV(faceAxis, px, py, pz, originX, originY, originZ) * vScale);
        }

        ensureIndices(indexCount + 6);
        indices[indexCount++] = base;
        indices[indexCount++] = base + 1;
        indices[indexCount++] = base + 2;
        indices[indexCount++] = base;
        indices[indexCount++] = base + 2;
        indices[indexCount++] = base + 3;
    }

    // Ejes de la cara, saltándose el de su normal.
    private static float projectedU(int faceAxis, float x, float y, float z,
                                    float ox, float oy, float oz) {
        return switch (faceAxis) {
            case 0 -> z - oz;   // normal ±X -> el plano es (Z, Y)
            case 1 -> x - ox;   // normal ±Y -> el plano es (X, Z)
            default -> x - ox;  // normal ±Z -> el plano es (X, Y)
        };
    }

    private static float projectedV(int faceAxis, float x, float y, float z,
                                    float ox, float oy, float oz) {
        return switch (faceAxis) {
            case 0 -> y - oy;   // normal ±X
            case 1 -> z - oz;   // normal ±Y
            default -> y - oy;  // normal ±Z
        };
    }

    /** Normales unitarias por eje de cara (0=X, 1=Y, 2=Z). */
    private static final float[][] FACE_NORMALS = {
            {1f, 0f, 0f},
            {0f, 1f, 0f},
            {0f, 0f, 1f},
    };

    private void pushVertex(float px, float py, float pz,
                            float nx, float ny, float nz,
                            float u, float v) {
        ensureVertices(vertexCount + 1);
        int i = vertexCount * FLOATS_PER_VERTEX;
        vertices[i] = px;
        vertices[i + 1] = py;
        vertices[i + 2] = pz;
        vertices[i + 3] = nx;
        vertices[i + 4] = ny;
        vertices[i + 5] = nz;
        vertices[i + 6] = u;
        vertices[i + 7] = v;
        vertexCount++;
    }

    private void ensureVertices(int needed) {
        int required = needed * FLOATS_PER_VERTEX;
        if (required <= vertices.length) {
            return;
        }
        int capacity = Math.max(vertices.length * 2, required);
        vertices = Arrays.copyOf(vertices, capacity);
    }

    private void ensureIndices(int needed) {
        if (needed <= indices.length) {
            return;
        }
        indices = Arrays.copyOf(indices, Math.max(indices.length * 2, needed));
    }
}
