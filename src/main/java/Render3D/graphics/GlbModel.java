package Render3D.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import UtilsRender.Json;

/**
 * Lector minimo de archivos GLB (glTF binario).
 *
 * <p>El motor no trae cargador glTF y la dependencia de assimp solo expone la
 * interfaz nativa, no una escena de alto nivel comoda de usar desde el hilo de
 * render. El subconjunto que necesitan los modelos de decoracion es muy
 * pequeno: contenedor GLB, {@code POSITION} y {@code NORMAL} en float, indices
 * y un color plano por material. Ningun modelo trae texturas ni UV.
 *
 * <p>Si un modelo se sale de ese subconjunto, {@link #isSupported()} lo dice y
 * quien lo usa lo descarta en vez de fallar al cargar.
 */
public final class GlbModel {

    /**
     * Los archivos estan en centimetros y con la altura en el eje Z.
     *
     * <p>Medido sobre los archivos reales: la planta mide 0,0025 en el binario
     * y la mesa redonda 0,0194, o sea que la planta es unas ocho veces mas
     * pequena que la mesa. Con este factor la planta queda en ~0,25 m de alto y
     * la mesa en ~1,9 m, que es la proporcion real entre una planta de interior
     * y una mesa redonda en una habitacion de 4 m de alto.
     *
     * <p>Ademas hay que girar los ejes: en el archivo la planta crece hacia +Z
     * y las lamparas cuelgan hacia -Z, o sea que la altura esta en Z. La
     * transformacion (x, y, z) -> (x, z, -y) es un giro de -90 grados alrededor
     * de X: conserva la orientacion y no espeja nada.
     */
    private static final float UNIT_TO_METERS = 100f;

    /** Un dibujo indexado, ya escalado y con los ejes del juego. */
    public static final class Part {
        public final float[] positions;
        public final float[] normals;
        public final int[] indices;
        public final float r;
        public final float g;
        public final float b;

        Part(float[] positions, float[] normals, int[] indices, float r, float g, float b) {
            this.positions = positions;
            this.normals = normals;
            this.indices = indices;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int triangleCount() {
            return indices.length / 3;
        }
    }

    private final List<Part> parts = new ArrayList<>();
    private final boolean supported;

    private GlbModel(boolean supported) {
        this.supported = supported;
    }

    public boolean isSupported() {
        return supported;
    }

    public List<Part> parts() {
        return parts;
    }

    public int triangleCount() {
        int total = 0;
        for (Part part : parts) {
            total += part.triangleCount();
        }
        return total;
    }

    /** Altura maxima en metros, ya escalada. */
    public float heightMeters() {
        float max = 0f;
        for (Part part : parts) {
            for (int i = 1; i < part.positions.length; i += 3) {
                max = Math.max(max, part.positions[i]);
            }
        }
        return max;
    }

    /** Radio maximo en metros, para comprobar que cabe en una celda. */
    public float radiusMeters() {
        float max = 0f;
        for (Part part : parts) {
            for (int i = 0; i < part.positions.length; i += 3) {
                max = Math.max(max, Math.abs(part.positions[i]));
                max = Math.max(max, Math.abs(part.positions[i + 2]));
            }
        }
        return max;
    }

    /** Lee un GLB desde el classpath. Si no se puede, devuelve un modelo vacio. */
    public static GlbModel load(String resourcePath) {
        try (InputStream in = GlbModel.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (in == null) {
                return new GlbModel(false);
            }
            return parse(in.readAllBytes());
        } catch (IOException e) {
            return new GlbModel(false);
        }
    }

    // ------------------------------------------------------------------
    // Formato
    // ------------------------------------------------------------------

    private static GlbModel parse(byte[] data) {
        if (data.length < 20 || !magic(data)) {
            return new GlbModel(false);
        }

        int total = readInt(data, 8);
        int offset = 12;
        byte[] json = null;
        byte[] bin = null;
        while (offset + 8 <= total) {
            int length = readInt(data, offset);
            int type = readInt(data, offset + 4);
            int start = offset + 8;
            if (type == 0x4E4F534A) {           // 'JSON'
                json = new byte[length];
                System.arraycopy(data, start, json, 0, length);
            } else if (type == 0x004E4942) {    // 'BIN'
                bin = new byte[length];
                System.arraycopy(data, start, bin, 0, length);
            }
            offset = start + length;
        }
        if (json == null || bin == null) {
            return new GlbModel(false);
        }

        Map<String, Object> root = Json.object(
                Json.parse(new String(json, StandardCharsets.UTF_8)));
        if (root == null) {
            return new GlbModel(false);
        }
        return build(root, bin);
    }

    private static boolean magic(byte[] data) {
        return data[0] == 'g' && data[1] == 'l' && data[2] == 'T' && data[3] == 'F';
    }

    private static int readInt(byte[] data, int at) {
        return (data[at] & 0xFF)
                | ((data[at + 1] & 0xFF) << 8)
                | ((data[at + 2] & 0xFF) << 16)
                | ((data[at + 3] & 0xFF) << 24);
    }

    // ------------------------------------------------------------------
    // Construccion de la escena
    // ------------------------------------------------------------------

    private static GlbModel build(Map<String, Object> root, byte[] bin) {
        List<Object> meshes = Json.array(root.get("meshes"));
        List<Object> accessors = Json.array(root.get("accessors"));
        List<Object> views = Json.array(root.get("bufferViews"));
        if (meshes == null || accessors == null || views == null) {
            return new GlbModel(false);
        }

        float[][] materialColors = readMaterialColors(root);

        GlbModel model = new GlbModel(true);
        for (int m = 0; m < meshes.size(); m++) {
            Map<String, Object> mesh = Json.object(meshes.get(m));
            List<Object> primitives = Json.array(mesh.get("primitives"));
            if (primitives == null) {
                return new GlbModel(false);
            }
            for (int p = 0; p < primitives.size(); p++) {
                Map<String, Object> prim = Json.object(primitives.get(p));

                // Solo triangulos (mode 4 es el unico que traen estos modelos).
                if (intOf(prim.get("mode"), 4) != 4) {
                    return new GlbModel(false);
                }
                Map<String, Object> attributes = Json.object(prim.get("attributes"));
                if (attributes == null
                        || !attributes.containsKey("POSITION")
                        || !attributes.containsKey("NORMAL")) {
                    return new GlbModel(false);
                }

                float[] positions = readVec3(accessors, views, bin,
                        intOf(attributes.get("POSITION"), -1), true);
                // Las normales:giran con la misma rotacion que las posiciones.
                // No se escalan porque una rotacion no cambia su longitud.
                float[] normals = readVec3(accessors, views, bin,
                        intOf(attributes.get("NORMAL"), -1), true);
                int[] indices = readIndices(accessors, views, bin, prim, positions.length / 3);

                int material = intOf(prim.get("material"), -1);
                float[] color = material >= 0 && materialColors != null
                        && material < materialColors.length
                        ? materialColors[material]
                        : new float[] { 0.8f, 0.8f, 0.8f };

                model.parts.add(new Part(positions, normals, indices,
                        color[0], color[1], color[2]));
            }
        }
        return model;
    }

    /**
     * Colores planos de cada material.
     *
     * @return null si algun material depende de una textura, porque este render
     *         solo pinta color plano y la dejaria gris.
     */
    private static float[][] readMaterialColors(Map<String, Object> root) {
        List<Object> materials = Json.array(root.get("materials"));
        if (materials == null) {
            return new float[0][];
        }
        float[][] out = new float[materials.size()][];
        for (int i = 0; i < materials.size(); i++) {
            Map<String, Object> material = Json.object(materials.get(i));
            Map<String, Object> pbr = material == null
                    ? null : Json.object(material.get("pbrMetallicRoughness"));
            if (pbr != null && pbr.containsKey("baseColorTexture")) {
                return null;
            }
            List<Object> factor = pbr == null ? null : Json.array(pbr.get("baseColorFactor"));
            if (factor == null || factor.size() < 3) {
                out[i] = new float[] { 0.8f, 0.8f, 0.8f };
            } else {
                out[i] = new float[] {
                        toSrgb(num(factor.get(0)).floatValue()),
                        toSrgb(num(factor.get(1)).floatValue()),
                        toSrgb(num(factor.get(2)).floatValue()) };
            }
        }
        return out;
    }

    /**
     * Convierte un color de lineal a sRGB.
     *
     * <p>El formato glTF guarda {@code baseColorFactor} en espacio lineal, pero
     * el motor no hace conversion de salida: las texturas se muestrean y se
     * muestran tal cual. Sin esta conversion, la madera de 0,09 lineal salia
     * casi negra frente a un muro de textura equivalente, porque 0,09 se
     * escribia como si ya fuera sRGB. El valor correcto en pantalla es
     * 0,09^(1/2,2) = 0,35, que es el tono que el autor del pack eligio.
     */
    private static float toSrgb(float linear) {
        if (linear <= 0f) {
            return 0f;
        }
        return (float) Math.pow(linear, 1.0 / 2.2);
    }

    private static float[] readVec3(List<Object> accessors, List<Object> views, byte[] bin,
                                    int accessorIndex, boolean scale) {
        Map<String, Object> accessor = Json.object(accessors.get(accessorIndex));
        Map<String, Object> view = Json.object(views.get(intOf(accessor.get("bufferView"), -1)));

        int stride = intOf(view.get("byteStride"), 12);
        int base = intOf(view.get("byteOffset"), 0) + intOf(accessor.get("byteOffset"), 0);
        int count = intOf(accessor.get("count"), 0);

        float[] out = new float[count * 3];
        ByteBuffer buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) {
            int at = base + i * stride;
            out[i * 3] = buffer.getFloat(at);
            out[i * 3 + 1] = buffer.getFloat(at + 4);
            out[i * 3 + 2] = buffer.getFloat(at + 8);
        }
        if (scale) {
            toMetersAndYUp(out);
        }
        return out;
    }

    /**
     * Pasa las coordenadas al sistema del juego: metros y altura en Y.
     *
     * <p>El juego mide en tiles de 2 m y crece en Y. Los modelos traen Z como
     * eje de altura, asi que se intercambian y se aplana el modelo sobre el
     * suelo, que es donde se apoya una planta o una mesa.
     *
     * <p>La transformacion es (x, y, z) -> (x, z, -y): giro de -90 grados
     * alrededor de X, que conserva la orientacion y el sentido de las normales
     * porque es una rotacion, no un espejo.
     */
    private static void toMetersAndYUp(float[] xyz) {
        for (int i = 0; i < xyz.length; i += 3) {
            float x = xyz[i];
            float y = xyz[i + 1];
            float z = xyz[i + 2];
            xyz[i] = x * UNIT_TO_METERS;
            xyz[i + 1] = z * UNIT_TO_METERS;
            xyz[i + 2] = -y * UNIT_TO_METERS;
        }
    }

    private static int[] readIndices(List<Object> accessors, List<Object> views, byte[] bin,
                                     Map<String, Object> prim, int vertexCount) {
        if (!prim.containsKey("indices")) {
            // Sin indices: un triangulo por cada grupo de tres vertices.
            int count = vertexCount - vertexCount % 3;
            int[] generated = new int[count];
            for (int i = 0; i < count; i++) {
                generated[i] = i;
            }
            return generated;
        }
        Map<String, Object> accessor = Json.object(accessors.get(intOf(prim.get("indices"), -1)));
        Map<String, Object> view = Json.object(views.get(intOf(accessor.get("bufferView"), -1)));

        int componentType = intOf(accessor.get("componentType"), 5123);
        int element = componentSize(componentType);
        int stride = intOf(view.get("byteStride"), 0);
        int base = intOf(view.get("byteOffset"), 0) + intOf(accessor.get("byteOffset"), 0);
        int count = intOf(accessor.get("count"), 0);

        int[] out = new int[count];
        ByteBuffer buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) {
            int at = base + (stride > 0 ? i * stride : i * element);
            out[i] = componentType == 5125
                    ? buffer.getInt(at)
                    : buffer.getShort(at) & 0xFFFF;
        }
        return out;
    }

    private static int componentSize(int componentType) {
        return componentType == 5125 ? 4 : 2;      // UNSIGNED_INT / UNSIGNED_SHORT
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static Double num(Object value) {
        return value instanceof Number ? (Double) value : Double.valueOf(0);
    }

    // ------------------------------------------------------------------
    // Transformaciones y subida a GPU
    // ------------------------------------------------------------------

    /**
     * Aplica una matriz a las posiciones de todas las partes, en sitio.
     *
     * <p>Las normales se dejan como vienen: estos modelos ya estan en el
     * espacio final y solo se les aplica giro uniforme alrededor de Y, que no
     * las deformaria aunque se hiciera.
     */
    public void applyTransform(Matrix4f transform) {
        if (transform == null) {
            return;
        }
        Vector3f v = new Vector3f();
        for (Part part : parts) {
            for (int i = 0; i < part.positions.length; i += 3) {
                v.set(part.positions[i], part.positions[i + 1], part.positions[i + 2]);
                transform.transformPosition(v);
                part.positions[i] = v.x;
                part.positions[i + 1] = v.y;
                part.positions[i + 2] = v.z;
            }
        }
    }

    /** Vértices de una parte, ya escalados a metros. */
    public float[] positionsOf(Part part) {
        return part.positions;
    }

    public float[] normalsOf(Part part) {
        return part.normals;
    }

    public int[] indicesOf(Part part) {
        return part.indices;
    }
}
