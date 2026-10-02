package Render3D.map;

import Render3D.collision.Aabb;
import Render3D.mesh.BlockType;
import Render3D.mesh.MeshBuilder;
import Render3D.mesh.WallBitmask;

import java.util.ArrayList;
import java.util.List;

/**
 * Autotiling de muros: recorre la matriz de texto, decide con los 4 vecinos
 * cardinales qué forma de bloque toca en cada celda y escribe la geometría
 * (pilar + conectores) en un {@link MeshBuilder}.
 *
 * <h2>Por qué se acaba el problema de intersecciones</h2>
 * No se fusionan tramos ni se recortan extremos "a ojo" contra la pared
 * perpendicular. Cada celda de muro genera SIEMPRE las mismas piezas:
 *
 * <ol>
 *   <li>un <b>pilar</b> central de {@code WALL_THICKNESS x WALL_HEIGHT x
 *       WALL_THICKNESS}, centrado en la casilla;</li>
 *   <li>un <b>conector</b> por cada vecino cardinal que también es muro,
 *       estirado desde la cara del pilar hasta la frontera de la casilla.</li>
 * </ol>
 *
 * La longitud del conector no depende del tipo de bloque: es siempre
 * {@code TILE_SIZE / 2 - WALL_THICKNESS / 2}. La casilla queda repartida en dos
 * mitades: la que va del centro a la frontera la cubre el conector y el
 * {@code WALL_THICKNESS / 2} central lo cubre el pilar. Dos casillas vecinas
 * aportan entonces {@code (TILE/2 + TILE/2) = TILE} exactos y se tocan justo
 * en la frontera compartida.
 *
 * <p>La condición de conexión es simétrica (la casilla A tiene conector Este
 * si y solo si B tiene conector Oeste, porque ambas preguntan por la misma
 * casilla), así que <b>ninguna unión puede quedar con huecos ni solapada</b>.
 * Las piezas de una misma celda tampoco se solapan entre sí: los conectores
 * son disjuntos por construcción y solo comparten la cara exacta con el pilar.
 *
 * <pre>
 *   casilla 4 x 4, grosor 1:
 *
 *        +-----+-----+    +-----+-----+
 *        |     |     |    |     |     |   conector N: 1.5 (largo) x 1
 *        |     |     |    |     |     |   pilar:      1 x 1
 *    +---+-----+-----+    +-----+-----+---+
 *    |   |#####|                |#####|   |   conector E: 1.5 (largo) x 1
 *    +---+-----+-----+    +-----+-----+---+
 *        |     |     |    |     |     |
 *        |     |     |    |     |     |
 *        +-----+-----+    +-----+-----+
 *
 *   celda A                    celda B (vecina al este)
 *   (pilar + conector Este)    (conector Oeste + pilar)
 * </pre>
 *
 * <h2>UVs</h2>
 * Todas las cajas se escriben en coordenadas de mundo con UV proyección
 * proporcional al tamaño de cada cara ({@link MeshBuilder#addBoxWorldUv}), de
 * modo que la textura mantiene la misma densidad en un pilar de 1x1 que en un
 * tramo largo y además queda <b>alineada entre cajas contiguas</b>: el pilar y
 * el conector que comparten cara continúan la misma textura sin costura.
 */
public final class WallAutotiler {

    /** Símbolos que continues hacia celda vecina: el muro debe llegar hasta su borde. */
    private static boolean connects(char glyph) {
        return switch (MapConfig.SymbolType.fromChar(glyph)) {
            case H_WALL, V_WALL, WINDOW, DOOR -> true;
            default -> false;
        };
    }

    /**
     * Símbolos cuya geometría dibuja ESTE autotiler. Las ventanas y las puertas
     * son "conectables" pero las dibuja {@link DungeonManager} con sus
     * plantillas (alféizar + dintel), así que aquí no se les emite nada: si se
     * emitiera, taparían su propio hueco.
     */
    private static boolean ownsGeometry(char glyph) {
        return switch (MapConfig.SymbolType.fromChar(glyph)) {
            case H_WALL, V_WALL -> true;
            default -> false;
        };
    }

    private final char[][] grid;
    private final int rows;
    private final int cols;
    private final float originX;
    private final float originZ;

    /** Semiespesor del muro. */
    private final float halfThickness;
    /** Distancia del centro de la casilla a su frontera. */
    private final float halfTile;
    /** Longitud de cada conector: de la cara del pilar a la frontera. */
    private final float reach;

    private final float uScale;
    private final float vScale;

    /** Un AABB por caja dibujada; se usan como colisionadores. */
    private final List<Aabb> colliders = new ArrayList<>();

    private final MeshBuilder builder = new MeshBuilder();

    /**
     * @param grid   matriz de caracteres del nivel
     * @param originX coordenada X del borde izquierdo del mapa
     * @param originZ coordenada Z del borde superior del mapa
     * @param uScale repeticiones de textura por unidad de mundo en U
     * @param vScale repeticiones de textura por unidad de mundo en V
     */
    public WallAutotiler(char[][] grid, float originX, float originZ, float uScale, float vScale) {
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;
        this.originX = originX;
        this.originZ = originZ;
        this.uScale = uScale;
        this.vScale = vScale;

        this.halfThickness = MapConfig.WALL_THICKNESS * 0.5f;
        this.halfTile = MapConfig.TILE_SIZE * 0.5f;
        // Si el muro fuese más grueso que media casilla el conector se
        // desaparecería; se recorta a cero en vez de generar cajas invertidas.
        this.reach = Math.max(0f, halfTile - halfThickness);
    }

    /** Recorre todo el mapa y escribe la geometría de los muros. */
    public void build() {
        builder.clear();
        colliders.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (ownsGeometry(grid[r][c])) {
                    emitCell(r, c);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Consulta de la máscara de vecinos
    // ------------------------------------------------------------------

    /** ¿La casilla conecta con los muros vecinos? (muro, ventana o puerta) */
    public boolean isWall(int r, int c) {
        return inBounds(r, c) && connects(grid[r][c]);
    }

    /** ¿La casilla tiene geometría propia? Las ventanas y puertas no. */
    public boolean ownsGeometry(int r, int c) {
        return inBounds(r, c) && ownsGeometry(grid[r][c]);
    }

    /**
     * Máscara de los 4 vecinos cardinales de la casilla. Fuera del mapa no hay
     * muro, así que un muro del borde del nivel no saca un conector hacia el
     * exterior (no hay nada a lo que llegar).
     */
    public int maskAt(int r, int c) {
        return WallBitmask.of(
                isWall(r - 1, c),
                isWall(r + 1, c),
                isWall(r, c + 1),
                isWall(r, c - 1));
    }

    /** Tipo de bloque que corresponde a la casilla. */
    public BlockType blockTypeAt(int r, int c) {
        return BlockType.fromMask(maskAt(r, c));
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    // ------------------------------------------------------------------
    // Emisión de la geometría
    // ------------------------------------------------------------------

    private void emitCell(int r, int c) {
        int mask = maskAt(r, c);
        BlockType block = BlockType.fromMask(mask);

        float cx = originX + (c + 0.5f) * MapConfig.TILE_SIZE;
        float cz = originZ + (r + 0.5f) * MapConfig.TILE_SIZE;

        // 1) Pilar central: presente en TODOS los tipos de bloque.
        float x0 = cx - halfThickness;
        float x1 = cx + halfThickness;
        float z0 = cz - halfThickness;
        float z1 = cz + halfThickness;
        emitBox(x0, z0, x1, z1);

        // 2) Conectores hacia los vecinos que también son muro. Una celda
        //    aislada (máscara 0) saca los cuatro: así un muro de una sola
        //    casilla se dibuja como un bloque macizo y no como una columna.
        boolean north = block.connectsNorth() || block.isIsolated();
        boolean south = block.connectsSouth() || block.isIsolated();
        boolean east = block.connectsEast() || block.isIsolated();
        boolean west = block.connectsWest() || block.isIsolated();

        if (north) {
            emitBox(x0, cz - halfTile, x1, z0);
        }
        if (south) {
            emitBox(x0, z1, x1, cz + halfTile);
        }
        if (east) {
            emitBox(x1, z0, cx + halfTile, z1);
        }
        if (west) {
            emitBox(cx - halfTile, z0, x0, z1);
        }
    }

    /** Añade una caja de altura completa y registra su colisionador. */
    private void emitBox(float minX, float minZ, float maxX, float maxZ) {
        if (maxX <= minX || maxZ <= minZ) {
            return;
        }
        builder.addBoxWorldUv(minX, 0f, minZ, maxX, MapConfig.WALL_HEIGHT, maxZ, uScale, vScale);
        colliders.add(new Aabb(minX, 0f, minZ, maxX, MapConfig.WALL_HEIGHT, maxZ));
    }

    // ------------------------------------------------------------------
    // Resultados
    // ------------------------------------------------------------------

    /**
     * Malla con todos los muros del mapa, en coordenadas de mundo. La geometría
     * ya está colocada, así que se dibuja con matriz {@code model} identidad.
     */
    public MeshBuilder geometry() {
        return builder;
    }

    /** Un AABB por caja de muro; la huella visual y la de colisión coinciden. */
    public List<Aabb> colliders() {
        return colliders;
    }

    public int colliderCount() {
        return colliders.size();
    }

    public int boxCount() {
        return builder.vertexCount() / VERTICES_PER_BOX;
    }

    /** 6 caras x 4 vértices, más los que se saltan las caras degeneradas. */
    private static final int VERTICES_PER_BOX = 24;
}
