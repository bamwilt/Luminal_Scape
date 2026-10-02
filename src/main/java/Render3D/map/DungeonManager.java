package Render3D.map;

import Render3D.collision.Aabb;
import Render3D.collision.CollisionManager;
import Render3D.graphics.GlbModel;
import Render3D.item.Item;
import Render3D.item.Item;
import Render3D.item.ItemKey;
import Render3D.item.ItemMap;
import Render3D.item.ItemTime;
import Render3D.graphics.PropMesh;
import Render3D.graphics.Wall;
import Render3D.mesh.BlockType;
import Render3D.mesh.Box;
import Render3D.mesh.Mesh;
import Render3D.mesh.MeshBuilder;
import UtilsRender.Shader;
import org.joml.Vector3f;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Render3D.map.MapConfig.SymbolType;
import static Render3D.map.MapConfig.TILE_SIZE;
import static Render3D.map.MapConfig.WALL_THICKNESS;
import static Render3D.map.MapConfig.WALL_HEIGHT;
import static Render3D.map.MapConfig.FLOOR_THICKNESS;
import static Render3D.map.MapConfig.UV_SCALE;
import static Render3D.map.MapConfig.ITEM_FLOAT_HEIGHT;
import static Render3D.map.MapConfig.ITEM_PICKUP_RADIUS;
import static Render3D.map.MapConfig.CAMERA_HEIGHT;
import static Render3D.map.MapConfig.PROP_BASE_Y;
import static Render3D.map.MapConfig.PROP_SCALE;
import static Render3D.map.MapConfig.RAILING_THICKNESS;
import static Render3D.map.MapConfig.LIGHT_CEILING_DROP;
import static Render3D.map.MapConfig.LIGHT_CEILING_EMISSION;
import static Render3D.map.MapConfig.LIGHT_CEILING_INTENSITY;
import static Render3D.map.MapConfig.LIGHT_CEILING_R;
import static Render3D.map.MapConfig.LIGHT_CEILING_G;
import static Render3D.map.MapConfig.LIGHT_CEILING_B;
import static Render3D.map.MapConfig.ITEM_LIGHT_INTENSITY;
import static Render3D.map.MapConfig.ITEM_LIGHT_R;
import static Render3D.map.MapConfig.ITEM_LIGHT_G;
import static Render3D.map.MapConfig.ITEM_LIGHT_B;
import static Render3D.map.MapConfig.LIGHT_CULL_DISTANCE;
import static Render3D.map.MapConfig.MAX_LIGHT_COUNT;

/**
 * Renderizador de mazmorras a partir de un mapa de texto.
 *
 * El nivel se define con un array de strings, uno por fila. Editar un nivel es
 * solo cambiar los caracteres de esas líneas; el mapa se guarda de forma mutable
 * (ver {@link #applyLayout}) dejando abierta la puerta a futuras mecánicas de
 * cambio de escenario.
 *
 * Todo lo editable de la mazmorra (constantes, símbolos y el nivel de ejemplo)
 * vive en UN solo lugar: {@link MapConfig}. <b>Para cambiar el mapa se toca
 * ahí</b>; esta clase solo construye la geometría a partir de esa
 * configuración.
 *
 * Tabla de símbolos (ver {@link MapConfig.SymbolType}):
 *   '■'  muro horizontal
 *   '◙'  muro vertical
 *   '▣'  ventana (ver {@link Window})
 *   '◧'  puerta (ver {@link Door})
 *   '◫'  piso y techo: dos paneles texturizados (abajo y arriba)
 *   '◉'  techo con luz: igual que '◫', pero el panel de arriba va a su propia
 *        malla, se pinta emisivo ({@link MapConfig#LIGHT_CEILING_EMISSION}) y
 *        enciende una luz puntual en la celda
 *   '▒'  piso sin techo: solo panel inferior (pasillos abiertos / puentes)
 *   '◰'  railing: tramo de piso SIN techo con DOS muros bajos y finos
 *        ({@link MapConfig#RAILING_THICKNESS}) en sus bordes largos: una barra
 *        continua por segmento, extendida hasta los muros/puertas, más un
 *        colisionador oculto a toda altura para que el jugador no se pase (ver
 *        {@link Railing}); sus colisiones van en lista aparte
 *   '◈'  item: la llave, un modelo que flota a la altura de la cámara, rota y
 *        desaparece si el jugador pasa por su punto. No brilla por si misma:
 *        deja una aureola dorada tenue alrededor (ver {@link
 *        MapConfig#ITEM_LIGHT_INTENSITY})
 *   '▲'/'▼'/'▶'/'◀'  spawn del jugador: la flecha indica hacia dónde mira
 *   '□'/' '  vacío: no se construye nada (fácil de editar)
 *
 * <h2>Muros: autotiling por bitmask</h2>
 * Los muros NO se fusionan en tramos ni se recortan contra la pared
 * perpendicular. Delegan en {@link WallAutotiler}, que para cada celda lee sus
 * 4 vecinos cardinales, decide el tipo de bloque ({@link BlockType}: recto,
 * esquina en L, Tee o cruz) y dibuja un pilar
 * central más un conector por cada vecino. Como la longitud del conector es
 * siempre {@code TILE_SIZE/2 - WALL_THICKNESS/2} y la condición de conexión es
 * simétrica, las uniones salen <b>exactamente a escuadra: sin huecos ni
 * solapes</b>. Toda la geometría de muros acaba en <b>un solo VAO</b>, con UV
 * proporcionales al tamaño de cada cara y <b>alineadas entre cajas</b>, así que
 * la textura mantiene la misma densidad en un pilar de 1x1 que en una pared de
 * 16 de largo y no se descuadra en las uniones.
 *
 * <h2>Pisos y techos inteligentes</h2>
 * Cada región conexa (celdas de '◫'/'▒' adyacentes por sus 4 lados, sin
 * importar los muros) calcula su propio primer y último; no hay un recuadro
 * global. El techo solo aparece sobre celdas de '◫', así que un pasillo o
 * puente de '▒'/'◰' entre dos cuartos queda abierto arriba.
 *
 * Alineación flexible de ventanas y puertas: se recorren los 4 vecinos (N, S,
 * E, O) y se usa la orientación del PRIMER muro encontrado ('■' -> horizontal,
 * '◙' -> vertical); si el vecino es otra ventana o puerta, se copia su
 * orientación. Sin ninguna referencia, la celda se convierte en un muro
 * estándar. Esta regla aplica a ventanas y puertas.
 *
 * Spawn único: si hay más de un spawn, se borra el anterior encontrado y se
 * cambia por piso ('◫'); permanece el último.
 *
 * Editar un símbolo = editar una línea de {@link MapConfig.SymbolType}.
 * Editar las alturas de ventanas/puertas = editar {@link Window} / {@link Door}.
 */
public class DungeonManager {

    /** Muros: una sola malla batcheada (coordenadas de mundo). */
    private Mesh wallMesh;

    /**
     * Toda la geometría estática del nivel se acumula en un {@link MeshBuilder}
     * por textura y se sube a un solo VAO al final: el mapa entero se dibuja
     * con TRES llamadas a glDrawElements (muros, pisos, techos) en lugar de
     * cientos. Los únicos objetos sueltos son los items, que rotan y necesitan
     * su propio {@link Wall} con UV en espacio local.
     */
    private final MeshBuilder wallGeometry = new MeshBuilder();
    private final MeshBuilder floorGeometry = new MeshBuilder();
    private final MeshBuilder ceilingGeometry = new MeshBuilder();

    /**
     * Techo con luz ({@link SymbolType#LIT_CEILING}), en una malla aparte porque
     * necesita su propia textura y su propia emisión: si compartiera malla con
     * el techo normal, el brillo le saldria a todo el techo del nivel.
     */
    private final MeshBuilder lightCeilingGeometry = new MeshBuilder();

    private Mesh floorMesh;
    private Mesh ceilingMesh;
    private Mesh lightCeilingMesh;

    /**
     * Focos fijos del nivel: uno por celda {@code ◉}, en la posicion y con el
     * color que fijan {@link MapConfig}. Se calculan al construir la geometria y
     * no cambian; los de los items se anaden cada frame en {@link #collectLights}.
     */
    private final List<Vector3f> staticLightPositions = new ArrayList<>();
    private final List<Vector3f> staticLightColors = new ArrayList<>();

    /**
     * Colisiones. Cada lista guarda un AABB por caja dibujada, de modo que la
     * huella de colisión es exactamente la huella visible (salvo los
     * colisionadores invisibles de ventanas y railings, que no se dibujan).
     */
    private final List<Aabb> wallColliders = new ArrayList<>();
    private final List<Aabb> openingColliders = new ArrayList<>();
    private final List<Aabb> railingColliders = new ArrayList<>();

    /** Autotiler vivo del nivel actual: resuelve máscaras y footprints. */
    private WallAutotiler autotiler;

    private final List<Item> items = new ArrayList<>();

    /**
     * Si el '☑' deja tiempo en vez de mapa. Lo fija Main segun la dificultad
     * antes de construir el nivel.
     */
    private boolean mapItemGivesTime = false;

    /**
     * Que suelta cada celda de item. La regla va aqui, y no repartida dentro de
     * {@link #buildItems}, porque es lo unico que decide el juego de un item: el
     * modelo y la malla son solo como se dibuja.
     *
     * <p>Es estatico y no toca la GPU a proposito, para que la regla se pueda
     * comprobar sin ventana.
     */
    public enum ItemKind {
        /** El '◈': da tiempo y cuenta para el total de artefactos. */
        ARTEFACTO,
        /** El '☑': enciende y revela el minimapa, no da tiempo. */
        MAPA,
        /** El '☑' cuando el minimapa ya viene de serie: da tiempo. */
        TIEMPO;

        /**
         * Que suelta una celda de item.
         *
         * @param type             simbolo de la celda
         * @param mapItemGivesTime si el '☑' se ha cambiado por tiempo
         */
        public static ItemKind of(SymbolType type, boolean mapItemGivesTime) {
            if (type == SymbolType.MAP_ITEM) {
                return mapItemGivesTime ? TIEMPO : MAPA;
            }
            return ARTEFACTO;
        }
    }

    /**
     * Decoracion colocada en el suelo, indexada por recurso. Se cachean los
     * modelos entre niveles: cargar un GLB es parsear JSON y subir buffers, y
     * hay menos de una decena de modelos distintos para todo el juego.
     */
    private static final Map<String, PropMesh> PROP_CACHE = new HashMap<>();

    /** Instancias de decoracion del nivel: indice de modelo, celda y giro. */
    private final List<PropInstance> props = new ArrayList<>();
    private boolean hasAnyItem = false;
    private int totalKeys = 0;
    private int collectedKeys = 0;
    private int totalProps = 0;
    private Vector3f spawnPosition;
    private float spawnYaw = -90f;

    // Mapa mutable: deja abierta la puerta a futuros cambios de escenario.
    private char[][] grid;

    public DungeonManager(String[] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        this(layout, wallTexture, floorTexture, ceilingTexture, ceilingTexture);
    }

    /**
     * @param lightCeilingTexture textura del panel luminoso {@code ◉}; si se
     *                            omite se reutiliza la del techo normal
     */
    public DungeonManager(String[] layout, int wallTexture, int floorTexture, int ceilingTexture,
                          int lightCeilingTexture) {
        build(layout, wallTexture, floorTexture, ceilingTexture, lightCeilingTexture);
    }

    /**
     * Instancia vacía, sin geometría y sin tocar la GPU. Existe para que las
     * pruebas puedan inspeccionar la geometría estática de un nivel con
     * {@link #buildStaticGeometry} sin abrir una ventana de OpenGL; el juego
     * siempre usa el constructor público.
     */
    DungeonManager() {
        grid = new char[1][1];
    }

    private static char[][] toCharGrid(String[] rows) {
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        char[][] grid = new char[rows.length][width];
        for (int i = 0; i < rows.length; i++) {
            Arrays.fill(grid[i], ' ');
            rows[i].getChars(0, rows[i].length(), grid[i], 0);
        }
        return grid;
    }

    /**
     * Nivel de ejemplo (ver {@link MapConfig#sampleLayout()}).
     */
    public static String[] sampleLayout() {
        return MapConfig.sampleLayout();
    }

    /** Una pieza de decoracion ya colocada en el mundo. */
    private static final class PropInstance {
        final PropMesh mesh;
        final float x;
        final float y;
        final float z;
        final float yaw;
        final Matrix4f transform = new Matrix4f();

        PropInstance(PropMesh mesh, float x, float y, float z, float yaw, float scale) {
            this.mesh = mesh;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            // La escala va en la matriz y no en el modelo, porque es por pieza:
            // un mismo GLB puede aparecer a tamano real y a la mitad en celdas
            // distintas, y la malla de GPU se comparte entre ambas.
            this.transform.identity()
                    .translate(x, y, z)
                    .rotateY((float) Math.toRadians(yaw))
                    .scale(scale);
        }
    }

    /**
     * Levanta la decoracion del mapa.
     *
     * <p>El plan sale de {@link PropLayout#plan(char[][])}, que es
     * determinista: el mismo mapa produce siempre las mismas piezas en las
     * mismas celdas. Aqui solo se traduce cada celda a coordenadas de mundo y
     * se sube el modelo a GPU la primera vez que se usa.
     */
    private void buildProps(char[][] layout, float originX, float originZ) {
        for (PropLayout.Placed placed : PropLayout.plan(layout)) {
            PropMesh mesh = PROP_CACHE.get(placed.model);
            if (mesh == null) {
                // La subida a GPU necesita contexto; si no lo hay se omite en
                // vez de fallar, para que las pruebas sin ventana sigan.
                GlbModel model = GlbModel.load(placed.model);
                if (model == null || !model.isSupported()) {
                    PROP_CACHE.put(placed.model, null);
                    continue;
                }
                mesh = PropMesh.upload(model);
                PROP_CACHE.put(placed.model, mesh);
            }
            if (mesh == null) {
                continue;
            }
            float cx = originX + placed.col * TILE_SIZE + TILE_SIZE / 2f;
            float cz = originZ + placed.row * TILE_SIZE + TILE_SIZE / 2f;
            // Sobre la cara visible del suelo (PROP_BASE_Y) y con la escala
            // global del panel aplicada a la de la pieza.
            props.add(new PropInstance(mesh, cx, PROP_BASE_Y, cz, placed.yaw,
                    placed.scale * PROP_SCALE));
        }
        totalProps = props.size();
    }

    private void build(String[] layout, int wallTexture, int floorTexture, int ceilingTexture,
                      int lightCeilingTexture) {
        collectStaticGeometry(toCharGrid(layout));

        buildItems(grid, getOriginX(), getOriginZ(), wallTexture);
        buildProps(grid, getOriginX(), getOriginZ());

        wallMesh = wallGeometry.build(wallTexture);
        floorMesh = floorGeometry.build(floorTexture);
        ceilingMesh = ceilingGeometry.build(ceilingTexture);
        lightCeilingMesh = lightCeilingGeometry.build(lightCeilingTexture);

        hasAnyItem = !items.isEmpty();
    }

    /**
     * Rellena los tres {@link MeshBuilder} y las listas de colisión. No toca la
     * GPU, así que la geometría estática se puede inspeccionar en pruebas sin
     * contexto de OpenGL (ver {@link #buildStaticGeometry}).
     */
    private void collectStaticGeometry(char[][] layout) {
        clearStaticGeometry();
        grid = layout;
        float originX = getOriginX();
        float originZ = getOriginZ();

        // Primero se fija la orientacion de cada abertura: las que no tienen
        // ningun vecino del que deducirla pasan a ser muro macizo, de modo que
        // el autotiler sea el UNICO dueño de la geometria de los muros y no
        // queden dos piezas compitiendo por la misma casilla.
        resolveOpeningsInPlace(layout);

        // Y despues se resuelve el spawn, que ademas NORMALIZA la matriz: si
        // hay mas de una flecha, las anteriores se borran y se cambian por
        // piso, y solo sobrevive la ultima. Tiene que pasar ANTES de construir
        // los pisos: las regiones agrupan celdas de piso, asi que normalizar
        // despues dejaria un agujero de 4x4 bajo cada spawn descartado.
        spawnPosition = findSpawn(layout, originX, originZ);

        buildWalls(layout, originX, originZ);
        buildRailings(layout, originX, originZ);
        buildOpenings(layout, originX, originZ);
        buildFloorsAndCeilings(layout, originX, originZ);
    }

    /** Vacía los builders y las listas de colisión de la geometría estática. */
    private void clearStaticGeometry() {
        wallGeometry.clear();
        floorGeometry.clear();
        ceilingGeometry.clear();
        lightCeilingGeometry.clear();
        wallColliders.clear();
        openingColliders.clear();
        railingColliders.clear();
        // Los focos fijos se vuelven a registrar al recorrer el mapa: si no se
        // limpian aqui, un cambio de nivel acumularia los del anterior.
        staticLightPositions.clear();
        staticLightColors.clear();
        autotiler = null;
    }

    /**
     * Construye SOLO la geometría estática de un nivel, sin subirla a la GPU y
     * sin items. Existe para poder verificar geometría, UVs y colisiones en
     * pruebas sin abrir una ventana de OpenGL; el juego nunca lo llama.
     */
    void buildStaticGeometry(String[] layout) {
        collectStaticGeometry(toCharGrid(layout));
    }

    /** Muros accumulate (muros, paneles de aberturas y barras de railing). */
    MeshBuilder wallGeometryView() {
        return wallGeometry;
    }

    MeshBuilder floorGeometryView() {
        return floorGeometry;
    }

    MeshBuilder ceilingGeometryView() {
        return ceilingGeometry;
    }

    MeshBuilder lightCeilingGeometryView() {
        return lightCeilingGeometry;
    }

    List<Aabb> wallCollidersView() {
        return wallColliders;
    }

    List<Aabb> openingCollidersView() {
        return openingColliders;
    }

    List<Aabb> railingCollidersView() {
        return railingColliders;
    }

    /**
     * Muros: el autotiler recorre la matriz, calcula la máscara de vecinos de
     * cada celda y escribe pilar + conectores en una única malla.
     */
    private void buildWalls(char[][] layout, float originX, float originZ) {
        autotiler = new WallAutotiler(layout, originX, originZ, UV_SCALE, UV_SCALE);
        autotiler.build();
        wallGeometry.appendFrom(autotiler.geometry());
        wallColliders.clear();
        wallColliders.addAll(autotiler.colliders());
    }

    /**
     * Añade una caja en coordenadas de mundo a la malla batcheada de muros y
     * registra su colisionador: la misma caja produce lo que se ve y lo que
     * bloquea, así que no pueden separarse.
     */
    private void addWallBox(Box box) {
        wallGeometry.addBox(box, UV_SCALE, UV_SCALE);
        wallColliders.add(box.toAabb());
    }

    /**
     * Normaliza las aberturas sin orientación: si una ventana o puerta no tiene
     * ningún vecino muro del que deducir si es horizontal o vertical, se
     * convierte en '■' (muro horizontal macizo). Se hace ANTES de construir la
     * geometría para que el autotiler la emita y no quede una casilla con dos
     * piezas superpuestas.
     */
    private void resolveOpeningsInPlace(char[][] layout) {
        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < layout[0].length; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isOpening()) {
                    continue;
                }
                if (resolveOrientation(layout, r, c) == null) {
                    layout[r][c] = SymbolType.H_WALL.glyph();
                }
            }
        }
    }

    /**
     * Tipo de bloque que el autotiling le asigna a una celda, ya resuelto con
     * sus 4 vecinos. Se recalcula sobre el mapa actual, así que refleja el
     * layout vigente.
     */
    public BlockType blockTypeAt(int r, int c) {
        if (autotiler == null) {
            return BlockType.ISOLATED;
        }
        return autotiler.blockTypeAt(r, c);
    }

    /** Máscara de vecinos cardinales de una celda. */
    public int wallMaskAt(int r, int c) {
        return autotiler == null ? 0 : autotiler.maskAt(r, c);
    }

    // Ventanas y puertas NO llevan orientación fija: toman la del primer muro
    // (o abertura) vecino que encuentren (ver resolveOrientation).
    private void buildOpenings(char[][] layout, float originX, float originZ) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isOpening()) {
                    continue;
                }
                // resolveOpeningsInPlace ya habia convertido en muro las
                // aberturas sin orientación; el null solo es defensivo.
                // resolveOpeningsInPlace ya habia convertido en muro macizo
                // toda abertura sin vecino del que deducir la orientacion.
                CellAxis axis = resolveOrientation(layout, r, c);
                if (axis == null) {
                    continue;
                }
                float width = axis == CellAxis.HORIZONTAL ? TILE_SIZE : WALL_THICKNESS;
                float depth = axis == CellAxis.HORIZONTAL ? WALL_THICKNESS : TILE_SIZE;
                addOpeningCell(type, width, depth, r, c, originX, originZ);
            }
        }
    }

    /** Orientación de una abertura resuelta a partir de las 4 direcciones. */
    private enum CellAxis { HORIZONTAL, VERTICAL }

    /**
     * Orientación flexible: se recorren los 4 vecinos (N, S, E, O) y se usa la
     * orientación del PRIMER muro encontrado ('■' -> horizontal, '◙' ->
     * vertical). Si el vecino es otra abertura, se copia su orientación
     * (resuelta de forma recursiva, con guarda anticiclos). Sin ninguna
     * referencia -> abertura sin orientación.
     */
    private CellAxis resolveOrientation(char[][] layout, int r, int c) {
        return resolveOrientation(layout, r, c, new boolean[layout.length][layout[0].length]);
    }

    private CellAxis resolveOrientation(char[][] layout, int r, int c, boolean[][] visiting) {
        visiting[r][c] = true;
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
        for (int[] d : directions) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr < 0 || nr >= layout.length || nc < 0 || nc >= layout[0].length) {
                continue;
            }
            SymbolType type = SymbolType.fromChar(layout[nr][nc]);
            if (type == SymbolType.H_WALL) {
                return CellAxis.HORIZONTAL;
            }
            if (type == SymbolType.V_WALL) {
                return CellAxis.VERTICAL;
            }
            if (type.isOpening() && !visiting[nr][nc]) {
                CellAxis axis = resolveOrientation(layout, nr, nc, visiting);
                if (axis != null) {
                    return axis;
                }
            }
        }
        return null;
    }

    /**
     * Pisos y techos inteligentes: dos pasadas independientes sobre regiones
     * conexas (celdas adyacentes por sus 4 lados, sin importar si hay muro,
     * agrupando solo celdas del mismo grupo de piso):
     *
     *   - PISOS: agrupa '◫' y '▒' (cualquier celda transitable EXCEPTO '◰').
     *     Cada región dibuja UN panel inferior con su caja contenedora exacta.
     *     El '◰' se excluye porque su piso es por celda ({@link
     *     #buildFloorsAndCeilings}), independiente de la región, para no tapar los
     *     huecos '□' que lo rodean.
     *   - TECHOS: agrupa solo '◫' (piso con techo). Las celdas de '▒'/'◰' (piso
     *     sin techo) NUNCA suman a un techo: un pasillo o puente entre dos
     *     cuartos queda abierto arriba.
     *
     * Así cada cuarto calcula su propio primer/último (expansión conexa) y no
     * hay un único recuadro global para todo el mapa.
     */
    /**
     * Piso y techo por CELDA, y el uno el espejo del otro: mismo calculo, misma
     * forma y misma huella; lo unico que cambia es la Y y la textura (por eso el
     * bloque de dimensiones se calcula una vez y se emite dos veces). Un '▒' se
     * queda sin techo a proposito, pero donde hay techo su huella es exactamente
     * la del suelo de esa celda.
     *
     * <p>El techo con luz '◉' sale de aqui con la MISMA forma que el normal: lo
     * unico que cambia es a que builder va, para que pueda llevar su textura y
     * su brillo, y que ademas se le registra un foco.
     *
     * <p>Cada celda se dibuja como un panel EXACTO de su tile, y por lado:
     * <ul>
     *   <li><b>Con suelo a ambos lados</b>: la celda ocupa su tile entero y los
     *       dos paneles se tocan borde con borde, sin hueco ni solape. Es el caso
     *       de los pasillos, las habitosnes abiertas y el interior de las salas.
     *       <p>
     *   <li><b>Con una celda cerrada al lado</b> (muro, ventana o puerta): esa
     *       celda se queda SIN panel propio y es el panel de la celda de suelo
     *       el que llega hasta su cara ({@code TILE_SIZE/2 - WALL_THICKNESS/2}
     *       por dentro de la celda cerrada). Asi la cara queda apoyada en el
     *       suelo, sin hueco detras, y sin que el panel se pase de la esquina.
     *       <p>Estoincludes las ventanas y las puertas: antes solo se alargaba
     *       hacia los muros, y una sala cerrada con aberturas se quedaba con un
     *       margen de 0.9 sin suelo ni techo alrededor de cada ventana.
     *       <p>
     *   <li><b>Con vacio o borde del mapa al lado</b>: el panel se para en la
     *       frontera de la celda, nunca se alarga. Esto es lo que evita el suelo
     *       desbordado por fuera de las habitaciones y los caminos.
     * </ul>
     *
     * <p>La pared se queda sin panel a proposito: es geometria vertical y ya
     * tapa el hueco. Si se le diera panel propio, el suelo apareceria en el
     * margen de 0.9 que queda entre la cara del muro y la frontera de su celda.
     * Ese margen es justamente el hueco que hay que tapar, pero taparlo desde la
     * celda de suelo lo hace sin ensuciar de geometria las paredes que no dan
     * a ninguna habitacion.
     */
    private void buildFloorsAndCeilings(char[][] layout, float originX, float originZ) {
        int rows = layout.length;
        int cols = layout[0].length;
        // Media celda menos media pared: justo hasta donde empieza el pilar.
        final float reach = TILE_SIZE / 2f - WALL_THICKNESS / 2f;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.needsFloor()) {
                    continue;
                }

                float xL = originX + c * TILE_SIZE;
                float xR = xL + TILE_SIZE;
                float zT = originZ + r * TILE_SIZE;
                float zB = zT + TILE_SIZE;

                if (needsReachAt(layout, r, c - 1)) xL -= reach;
                if (needsReachAt(layout, r, c + 1)) xR += reach;
                if (needsReachAt(layout, r - 1, c)) zT -= reach;
                if (needsReachAt(layout, r + 1, c)) zB += reach;

                float centerX = (xL + xR) / 2f;
                float centerZ = (zT + zB) / 2f;
                float width = xR - xL;
                float depth = zB - zT;

                // Suelo y techo: MISMA huella, distinta Y y distinta textura. Las
                // UV van en coordenadas de mundo, asi que la textura continua de
                // celda en celda sin cortes.
                floorGeometry.addBoxOnFloor(centerX, 0f, centerZ,
                        width, FLOOR_THICKNESS, depth, UV_SCALE, UV_SCALE);

                if (type.needsCeilingIn(layout, r, c)) {
                    // El techo con luz va a SU malla: es lo que le permite
                    // llevar otra textura y brillar por su cuenta sin que se
                    // le enclose al techo normal. La forma es exactamente la
                    // misma, asi que la union con el techo de al lado no se nota.
                    MeshBuilder destino = type.isLitCeiling() ? lightCeilingGeometry : ceilingGeometry;
                    destino.addBoxOnFloor(centerX, WALL_HEIGHT - FLOOR_THICKNESS, centerZ,
                            width, FLOOR_THICKNESS, depth, UV_SCALE, UV_SCALE);
                    if (type.isLitCeiling()) {
                        // El foco va un poco por debajo del panel: en el plano
                        // del techo la luz llegaria al suelo casi de canto y la
                        // habitacion no se aclararia.
                        staticLightPositions.add(new Vector3f(centerX,
                                WALL_HEIGHT - FLOOR_THICKNESS - LIGHT_CEILING_DROP, centerZ));
                        staticLightColors.add(new Vector3f(
                                LIGHT_CEILING_R * LIGHT_CEILING_INTENSITY,
                                LIGHT_CEILING_G * LIGHT_CEILING_INTENSITY,
                                LIGHT_CEILING_B * LIGHT_CEILING_INTENSITY));
                    }
                }
            }
        }
    }

    /**
     * ¿La celda indicada está cerrada y sin panel propio, de modo que el panel
     * vecino tiene que llegar hasta su cara? Fuera del mapa no hay nada que
     * tapar.
     */
    private boolean needsReachAt(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        return SymbolType.fromChar(layout[r][c]).needsPanelReach();
    }

    /**
     * Railing ('◰'): tramo de piso SIN techo (el piso se pinta por celda, ver
     * {@link #buildFloorsAndCeilings}) con un muro bajo de 2.0 en CADA lado que da
     * a vacío, muro o ventana. El lado se ABRE (sin barra) cuando la celda
     * contigua es otra superficie de railing, una puerta, un piso (con o sin
     * techo), un spawn o un artefacto; se CIERRA (con barra) contra el vacío
     * '□', los muros '■◙', las ventanas '▣' y el exterior del mapa.
     *
     * Las barras se generan por LÍNEA de borde y las celdas contiguas se
     * fusionan en un solo Railing. Cada extremo de una barra se estira media
     * celda hacia un piso o una puerta (el lateral se abre ahí y la barra debe
     * conectar con la habitación sin dejar huecos); hacia otro '◰' el borde
     * termina EXACTO en el límite compartido, sin solapes.
     */
    private void buildRailings(char[][] layout, float originX, float originZ) {
        int rows = layout.length;
        int cols = layout[0].length;
        List<RailingBar> bars = new ArrayList<>();

        // Bordes horizontales: cada línea entre la fila b-1 y la fila b. La cara
        // sur de la fila b-1 y la cara norte de la fila b coinciden en la misma
        // línea; nunca hay dos barras a la vez porque entre dos '◰' el lado abre.
        for (int b = 0; b <= rows; b++) {
            int segStart = -1;
            for (int c = 0; c <= cols; c++) {
                boolean bar = c < cols && horizontalBarAt(layout, b, c);
                if (bar && segStart < 0) {
                    segStart = c;
                }
                if (segStart >= 0 && !bar) {
                    float xL = originX + segStart * TILE_SIZE;
                    float xR = originX + c * TILE_SIZE;
                    if (extendsHorizAt(layout, b, segStart - 1)) {
                        xL -= TILE_SIZE / 2f;
                    }
                    if (extendsHorizAt(layout, b, c)) {
                        xR += TILE_SIZE / 2f;
                    }
                    addRailingBar(bars, (xL + xR) / 2f, originZ + b * TILE_SIZE,
                            xR - xL, RAILING_THICKNESS);
                    segStart = -1;
                }
            }
        }

        // Bordes verticales: cada línea entre la columna d-1 y la columna d.
        for (int d = 0; d <= cols; d++) {
            int segStart = -1;
            for (int r = 0; r <= rows; r++) {
                boolean bar = r < rows && verticalBarAt(layout, r, d);
                if (bar && segStart < 0) {
                    segStart = r;
                }
                if (segStart >= 0 && !bar) {
                    float zT = originZ + segStart * TILE_SIZE;
                    float zB = originZ + r * TILE_SIZE;
                    if (extendsVertAt(layout, segStart - 1, d)) {
                        zT -= TILE_SIZE / 2f;
                    }
                    if (extendsVertAt(layout, r, d)) {
                        zB += TILE_SIZE / 2f;
                    }
                    addRailingBar(bars, originX + d * TILE_SIZE, (zT + zB) / 2f,
                            RAILING_THICKNESS, zB - zT);
                    segStart = -1;
                }
            }
        }

        mergeRailingCorners(bars);
        for (RailingBar bar : bars) {
            emitRailingBar(bar);
        }
    }

    // ¿Barra en el borde horizontal entre la fila b-1 y la b, en la columna c?
    // Cierra si la celda railing de un lado se enfrenta a algo que NO abre el
    // borde (vacío, muro, ventana o exterior del mapa).
    private boolean horizontalBarAt(char[][] layout, int b, int c) {
        return (isRailingSurface(layout, b - 1, c) && !railingOpens(layout, b, c))
                || (isRailingSurface(layout, b, c) && !railingOpens(layout, b - 1, c));
    }

    // ¿Barra en el borde vertical entre la columna d-1 y la d, en la fila r?
    private boolean verticalBarAt(char[][] layout, int r, int d) {
        return (isRailingSurface(layout, r, d - 1) && !railingOpens(layout, r, d))
                || (isRailingSurface(layout, r, d) && !railingOpens(layout, r, d - 1));
    }

    // ¿Estirar media celda el extremo de la barra horizontal de la línea b hacia
    // la columna c (justo fuera del tramo)? Solo hacia un PISO (con o sin techo)
    // o una PUERTA, donde el lateral se ABRE: así la barra llega a la habitación
    // sin dejar huecos. Si en c hay otro '◰', vacío, muro, ventana o exterior,
    // el borde queda EXACTO en el límite (sin solapes entre railings vecinos).
    private boolean extendsHorizAt(char[][] layout, int b, int c) {
        if (isRailingSurface(layout, b - 1, c) || isRailingSurface(layout, b, c)) {
            return false;
        }
        return isOpeningSurface(layout, b - 1, c) || isOpeningSurface(layout, b, c);
    }

    // ¿Estirar media celda el extremo de la barra vertical de la línea d hacia
    // la fila r (justo fuera del tramo)? Misma regla que {@link #extendsHorizAt}.
    private boolean extendsVertAt(char[][] layout, int r, int d) {
        if (isRailingSurface(layout, r, d - 1) || isRailingSurface(layout, r, d)) {
            return false;
        }
        return isOpeningSurface(layout, r, d - 1) || isOpeningSurface(layout, r, d);
    }

    // ¿La celda merece la estirada de media celda de un extremo de barra? Solo
    // un piso ('◫'/'▒') o una puerta ('◧'): son los lados que se ABREN y donde
    // la barra debe conectar con la habitación. Contra vacío, muro, ventana,
    // exterior u otra superficie de railing el extremo queda exacto en su límite.
    private boolean isOpeningSurface(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        SymbolType type = SymbolType.fromChar(layout[r][c]);
        return type == SymbolType.DOOR
                || type == SymbolType.FLOOR_CEILING
                || type == SymbolType.FLOOR_ONLY;
    }

    // Una dirección del railing se ABRE (sin barra) si la celda vecina es otra
    // superficie de railing, una puerta, un piso (con/sin techo), un spawn o un
    // artefacto; el vacío, el muro, la ventana y el exterior del mapa la cierran.
    private boolean railingOpens(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        SymbolType type = SymbolType.fromChar(layout[r][c]);
        return type == SymbolType.RAILING
                || type == SymbolType.DOOR
                || type == SymbolType.FLOOR_CEILING
                || type == SymbolType.FLOOR_ONLY
                || type.isItem()
                || type.isSpawn();
    }

    /**
     * Tramo de railing en planta. Se guardan los extremos en vez de emitir la
     * caja directamente porque las esquinas se resuelven DESPUES de saber todos
     * los tramos (ver {@link #mergeRailingCorners}).
     */
    private static final class RailingBar {
        final boolean horizontal;
        float xL;
        float xR;
        float zT;
        float zB;

        RailingBar(boolean horizontal, float xL, float xR, float zT, float zB) {
            this.horizontal = horizontal;
            this.xL = xL;
            this.xR = xR;
            this.zT = zT;
            this.zB = zB;
        }

        float centerX() {
            return (xL + xR) * 0.5f;
        }

        float centerZ() {
            return (zT + zB) * 0.5f;
        }

        float width() {
            return xR - xL;
        }

        float depth() {
            return zB - zT;
        }
    }

    private void addRailingBar(List<RailingBar> bars, float centerX, float centerZ,
                               float width, float depth) {
        boolean horizontal = width >= depth;
        bars.add(new RailingBar(horizontal,
                centerX - width * 0.5f, centerX + width * 0.5f,
                centerZ - depth * 0.5f, centerZ + depth * 0.5f));
    }

    /**
     * Resuelve las ESQUINAS de railing, donde un tramo horizontal y otro
     * vertical se cruzan.
     *
     * <p>Sin este paso los dos tramos se pisan a medias: el horizontal llega
     * hasta el centro de la linea vertical y el vertical arranca en el centro
     * de la horizontal, dejando un hueco de medio grosor (0.2) en una esquina y
     * un solape del mismo tamaño en la opuesta. Aqui la CORRIDA se queda con la
     * esquina: crece hasta la cara lejana de la perpendicular, y la
     * perpendicular se recorta contra la cara lejana de la corrida. El resultado
     * es una union en L sin huecos ni solapes.
     */
    private void mergeRailingCorners(List<RailingBar> bars) {
        float eps = 1e-3f;
        float thickness = RAILING_THICKNESS;
        for (RailingBar run : bars) {
            if (!run.horizontal) {
                continue;
            }
            for (RailingBar cross : bars) {
                if (cross.horizontal) {
                    continue;
                }
                // Las bandas finas tienen que cruzarse de verdad.
                if (run.zT >= cross.zB - eps || run.zB <= cross.zT + eps) {
                    continue;
                }
                // Y los tramos tienen que tocarse o pisarse a lo largo de X.
                if (cross.xL >= run.xR + thickness - eps || cross.xR <= run.xL - thickness + eps) {
                    continue;
                }
                if (cross.xL < run.xL - eps) {
                    run.xL = cross.xL;
                }
                if (cross.xR > run.xR + eps) {
                    run.xR = cross.xR;
                }
                // La perpendicular se detiene en la cara lejana de la corrida.
                if (cross.zT < run.zT + eps) {
                    if (run.zT - cross.zT > thickness * 0.25f) {
                        cross.zB = run.zT;
                    }
                } else if (run.zB - cross.zT > thickness * 0.25f) {
                    cross.zT = run.zB;
                }
            }
        }
    }

    /** Emite un tramo ya resuelto: barra visible baja + colisionador invisible. */
    private void emitRailingBar(RailingBar bar) {
        // La barra visible es tan baja que el jugador la atraviesa por encima,
        // asi que NO aporta colision; el bloqueo real es el colisionador
        // invisible de abajo, a toda altura y con el mismo contorno en planta.
        wallGeometry.addBox(Railing.barBox(bar.width(), bar.depth(), bar.centerX(), bar.centerZ()),
                UV_SCALE, UV_SCALE);
        railingColliders.add(
                Railing.colliderBox(bar.width(), bar.depth(), bar.centerX(), bar.centerZ()).toAabb());
    }

    private boolean isRailing(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        return SymbolType.fromChar(layout[r][c]) == SymbolType.RAILING;
    }

    /**
     * Trazo de railing: una celda '◰' o un item '◈' colocado SOBRE un railing
     * (con al menos un vecino '◰'). El item se trata como parte del railing en
     * la construcción del piso y las barras, para que no quede un hueco en el
     * puente ni se corte la barandilla bajo el artefacto.
     */
    private boolean isRailingSurface(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        char glyph = layout[r][c];
        if (SymbolType.fromChar(glyph) == SymbolType.RAILING) {
            return true;
        }
        if (!SymbolType.fromChar(glyph).isItem()) {
            return false;
        }
        return isRailing(layout, r, c - 1) || isRailing(layout, r, c + 1)
                || isRailing(layout, r - 1, c) || isRailing(layout, r + 1, c);
    }

    // Items: un modelo que flota a la altura de la cámara. Que celda suelte que
    // lo decide el simbolo y, para el mapa, la dificultad (ver mapItemGivesTime).
    private void buildItems(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isItem()) {
                    continue;
                }
                Vector3f center = new Vector3f(
                        originX + (c + 0.5f) * TILE_SIZE,
                        ITEM_FLOAT_HEIGHT,
                        originZ + (r + 0.5f) * TILE_SIZE);

                PropMesh malla;
                Item item;
                // La regla esta en ItemKind.of: aqui solo se monta lo que
                // salio. Si un dia aparece un premio nuevo, se anade su rama y
                // el enum, y el decision sigue siendo una sola linea.
                ItemKind kind = ItemKind.of(type, mapItemGivesTime);
                switch (kind) {
                    case MAPA:
                        malla = mallaDe(ItemMap.MODEL);
                        item = malla == null ? null : new ItemMap(malla, center);
                        break;
                    case TIEMPO:
                        malla = mallaDe(ItemTime.MODEL);
                        item = malla == null ? null : new ItemTime(malla, center);
                        break;
                    default:
                        malla = mallaDe(ItemKey.MODEL);
                        item = malla == null ? null : new ItemKey(malla, center);
                        break;
                }
                if (item != null) {
                    items.add(item);
                    // El total se cuenta aqui, y no antes, por una razon que
                    // importa: si el GLB de un artefacto no se puede cargar, la
                    // celda no crea Item y no habria nada que recoger. Sumar
                    // la celda igualmente dejaria una llave en el contador que
                    // no existe en el nivel, y el nivel seria imposible de
                    // terminar (el jugador se queda atrapado buscando una
                    // llave que no esta). Solo se cuenta lo que existe.
                    if (kind == ItemKind.ARTEFACTO) {
                        // Las llaves son lo unico que hace falta para pasar de
                        // nivel. El mapa y el tiempo son ayudas: se pueden
                        // recoger o no, pero nunca son requisito.
                        totalKeys++;
                    }
                }
            }
        }
    }

    /**
     * Sube un GLB de item a GPU, o devuelve {@code null} si no se puede leer.
     *
     * <p>Va por la cache compartida con la decoracion, asi que un modelo repetido
     * se sube una sola vez. Si el GLB faltara, se devuelve {@code null} y quien
     * llama se salta ese item: el nivel sigue siendo jugable, solo falta el
     * objeto en la mesa.
     */
    private static PropMesh mallaDe(String modelo) {
        PropMesh malla = PROP_CACHE.get(modelo);
        if (malla == null) {
            GlbModel glb = GlbModel.load(modelo);
            malla = glb == null ? null : PropMesh.upload(glb);
            PROP_CACHE.put(modelo, malla);
        }
        return malla;
    }

    // Busca el ÚLTIMO spawn; los anteriores se borran y se cambian por piso.
    private Vector3f findSpawn(char[][] layout, float originX, float originZ) {
        int rows = layout.length;
        int cols = layout[0].length;
        int lastR = -1, lastC = -1;
        SymbolType lastType = null;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isSpawn()) {
                    continue;
                }
                if (lastR >= 0) {
                    layout[lastR][lastC] = SymbolType.FLOOR_CEILING.glyph();
                }
                lastR = r;
                lastC = c;
                lastType = type;
            }
        }

        if (lastR < 0) {
            spawnYaw = -90f;
            return null;
        }

        spawnYaw = spawnYawFor(lastType);
        return new Vector3f(
                originX + (lastC + 0.5f) * TILE_SIZE,
                CAMERA_HEIGHT,
                originZ + (lastR + 0.5f) * TILE_SIZE);
    }

    /** Yaw de la dirección de la flecha del spawn (norte = -90°, default). */
    private static float spawnYawFor(SymbolType type) {
        switch (type) {
            case SPAWN_S: return 90f;
            case SPAWN_E: return 0f;
            case SPAWN_W: return 180f;
            default:      return -90f; // SPAWN_N
        }
    }

    // Ventanas y puertas usan sus plantillas (Window/Door) con alturas
    // ajustables; ambas colisionan solo en el muro, el hueco queda libre.
    private void addOpeningCell(SymbolType type, float width, float depth, int r, int c,
                                float originX, float originZ) {
        if (type.opening() == null) {
            return;
        }
        float centerX = originX + (c + 0.5f) * TILE_SIZE;
        float centerZ = originZ + (r + 0.5f) * TILE_SIZE;
        for (Box panel : type.opening().buildBoxes(width, depth, centerX, centerZ, WALL_HEIGHT)) {
            addWallBox(panel);
        }
        // Las ventanas NUNCA se traspasan: el hueco visual queda, pero se
        // registra un colisionador invisible a toda altura para que el jugador
        // no pase por el boquete (las puertas sí son transitables agachándose).
        // Este colisionador no se dibuja: solo aporta su AABB.
        if (type.opening() instanceof Window) {
            openingColliders.add(new Aabb(centerX - width * 0.5f, 0f, centerZ - depth * 0.5f,
                    centerX + width * 0.5f, WALL_HEIGHT, centerZ + depth * 0.5f));
        }
        // Las puertas solo colisionan en su parte solida (el dintel), que ya se
        // registro arriba como caja normal: agachandose se pasa por debajo.
    }

    /**
     * Reemplaza el nivel completo y reconstruye toda la geometría. Pensado para
     * futuras mecánicas de cambio de escenario: tras llamarlo hay que volver a
     * registrar las colisiones y reposicionar al jugador en el nuevo spawn.
     */
    public void applyLayout(String[] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        applyLayout(layout, wallTexture, floorTexture, ceilingTexture, ceilingTexture);
    }

    /** Como {@link #applyLayout(String[], int, int, int)}, con la textura de la placa. */
    public void applyLayout(String[] layout, int wallTexture, int floorTexture, int ceilingTexture,
                            int lightCeilingTexture) {
        freeGeometry();
        build(layout, wallTexture, floorTexture, ceilingTexture, lightCeilingTexture);
    }

    private void freeGeometry() {
        if (wallMesh != null) {
            wallMesh.cleanup();
            wallMesh = null;
        }
        if (floorMesh != null) {
            floorMesh.cleanup();
            floorMesh = null;
        }
        if (ceilingMesh != null) {
            ceilingMesh.cleanup();
            ceilingMesh = null;
        }
        if (lightCeilingMesh != null) {
            lightCeilingMesh.cleanup();
            lightCeilingMesh = null;
        }
        clearStaticGeometry();
        // Los items no liberan su malla: comparten la cache de modelos con la
        // decoracion, que se libera una vez al cerrar el juego.
        items.clear();
        // Los contadores de llaves son POR NIVEL y se ponen a cero aqui, que es
        // donde se vacia la lista de items. Si se dejaran como estaban, cada
        // vez que se carga un nivel sumarian las llaves del anterior encima de
        // las suyas: el HUD mostraria un total mayor que las llaves que hay en
        // el mapa y, con ellas, el nivel seria imposible de completar (el
        // jugador se quedaria atrapado sin poder llegar nunca al final). El
        // collectedKeys tambien se reinicia porque arrastrarlo haria que un
        // nivel apareciera con llaves ya cogidas que no son suyas.
        totalKeys = 0;
        collectedKeys = 0;
        spawnPosition = null;
    }

    /**
     * Actualiza los items y devuelve el que se acaba de recoger, o
     * {@code null}.
     *
     * <p>Antes devolvia un {@code boolean} y Main respondia siempre igual,
     * como si cualquier item fuera un artefacto. Devolviendo el item, quien
     * llama puede distinguir el mapa (que enciende el minimapa) del tiempo (que
     * suma segundos) sin tener que compararse con el numero de celdas.
     *
     * <p>Al recogerse no hay nada que liberar: la malla la comparte la cache
     * de modelos con el resto de la decoracion del nivel.
     */
    public Item update(Vector3f playerPos, float deltaTime) {
        Item recogido = null;
        for (Item item : items) {
            if (recogido == null && item.isWithin(playerPos, ITEM_PICKUP_RADIUS)) {
                recogido = item;
            }
        }
        if (recogido != null) {
            if (recogido instanceof ItemKey) {
                collectedKeys++;
            }
            items.remove(recogido);
        }
        return recogido;
    }

    public void render(Shader shader) {
        // Todo el nivel estatico en CUATRO llamadas: muros, pisos, techos y la
        // placa de luz. La geometria esta en coordenadas de mundo, asi que la
        // matriz model va en identidad y la textura es continua entre celdas,
        // aberturas y pisos.
        //
        // La emision se pone a cero antes de cada malla y a mano antes de la
        // del techo con luz: es un uniform del shader, asi que se queda en el
        // valor del ultimo dibujado si no se reponte.
        shader.setFloat("u_emission", 0f);
        if (wallMesh != null) {
            wallMesh.render(shader);
        }
        if (floorMesh != null) {
            floorMesh.render(shader);
        }
        if (ceilingMesh != null) {
            ceilingMesh.render(shader);
        }
        if (lightCeilingMesh != null) {
            shader.setFloat("u_emission", LIGHT_CEILING_EMISSION);
            lightCeilingMesh.render(shader);
            shader.setFloat("u_emission", 0f);
        }
    }

    /**
     * Renderiza la decoracion con su shader propio.
     *
     * <p>Las props llevan matriz propia (posicion y giro), a diferencia del
     * escenario, que va en coordenadas de mundo con la identidad.
     */
    public void renderProps(Shader propShader) {
        for (PropInstance prop : props) {
            prop.mesh.render(propShader, prop.transform);
        }
    }

    /**
     * Renderiza los items.
     *
     * <p>Cada item lleva su propia matriz y su propio comportamiento, asi que
     * aqui no hay nada especifico de la llave: solo se les dice que se animen y
     * se dibujen.
     */
    public void renderItems(Shader itemShader, float time) {
        for (Item item : items) {
            item.animate(time);
            item.render(itemShader);
        }
    }

    // ---- Luces puntuales ---------------------------------------------------

    /**
     * Color que deja la llave alrededor: dorado y tenue.
     *
     * <p>Es un color MAS, no una emision del propio item: la llave no se
     * enciende, ilumina lo que tiene cerca.
     */
    private static final Vector3f ITEM_LIGHT_COLOR = new Vector3f(
            ITEM_LIGHT_R * ITEM_LIGHT_INTENSITY,
            ITEM_LIGHT_G * ITEM_LIGHT_INTENSITY,
            ITEM_LIGHT_B * ITEM_LIGHT_INTENSITY);

    /** Buffers de trabajo de {@link #collectLights}, crecidos a demanda. */
    private float[] lightScratchPos = new float[0];
    private float[] lightScratchCol = new float[0];
    private float[] lightScratchDist = new float[0];
    private boolean[] lightScratchUsed = new boolean[0];

    /**
     * Llena {@code positions} y {@code colors} con las luces puntuales que
     * alcanza a ver el jugador, y devuelve cuantas hay.
     *
     * <p>Los tres grupos de focos (las placas '◉' del nivel y las llaves) se
     * juntan en una sola lista y se mandan las {@code maxLights} MAS CERCANAS A
     * LA CAMARA. Ordenar por la camara y no por el fragmento es una aproximacion
     * barata: la diferencia solo se nota en una pared lejana a la que le llega
     * mas luz desde el otro lado del mapa, y esa pared ya esta medio fundida con
     * la niebla ahi.
     *
     * <p>Los buffers son del llamante y se reutilizan entre frames (ver
     * {@code lightPosBuffer} en {@code Main}), porque esto corre una vez por
     * frame y no puede crear un array por luz y frame.
     *
     * @param positions destino, tres floats por luz
     * @param colors    destino, tres floats por luz (rgb ya con la intensidad)
     * @return numero de luces escritas, como mucho {@code maxLights}
     */
    public int collectLights(Vector3f cameraPos, float[] positions, float[] colors, int maxLights) {
        int slots = Math.min(maxLights, MAX_LIGHT_COUNT);
        if (slots <= 0) {
            return 0;
        }
        int candidates = staticLightPositions.size() + items.size();
        if (candidates == 0) {
            return 0;
        }
        ensureLightScratch(candidates);

        int n = 0;
        for (int i = 0; i < staticLightPositions.size(); i++) {
            Vector3f p = staticLightPositions.get(i);
            if (pastCullDistance(cameraPos, p)) {
                continue;
            }
            pushLight(n, p.x, p.y, p.z, staticLightColors.get(i), cameraPos);
            n++;
        }
        for (Item item : items) {
            Vector3f p = item.getCenter();
            if (pastCullDistance(cameraPos, p)) {
                continue;
            }
            pushLight(n, p.x, p.y, p.z, ITEM_LIGHT_COLOR, cameraPos);
            n++;
        }

        // Seleccion repetida del mas cercano: con unas pocas luces por frame es
        // mas barato que ordenar, y aqui el numero de candidatos suele ser pequeno.
        int chosen = 0;
        for (int slot = 0; slot < slots && chosen < n; slot++) {
            int best = -1;
            float bestDist = Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!lightScratchUsed[i] && lightScratchDist[i] < bestDist) {
                    bestDist = lightScratchDist[i];
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            lightScratchUsed[best] = true;
            System.arraycopy(lightScratchPos, best * 3, positions, slot * 3, 3);
            System.arraycopy(lightScratchCol, best * 3, colors, slot * 3, 3);
            chosen++;
        }
        // Se devuelven los candidatos al filtro de la proxima llamada.
        Arrays.fill(lightScratchUsed, 0, n, false);
        return chosen;
    }

    /** Anade un foco candidato al buffer de trabajo y anota su distancia. */
    private void pushLight(int index, float x, float y, float z, Vector3f color, Vector3f cameraPos) {
        lightScratchPos[index * 3] = x;
        lightScratchPos[index * 3 + 1] = y;
        lightScratchPos[index * 3 + 2] = z;
        lightScratchCol[index * 3] = color.x;
        lightScratchCol[index * 3 + 1] = color.y;
        lightScratchCol[index * 3 + 2] = color.z;
        float dx = x - cameraPos.x;
        float dy = y - cameraPos.y;
        float dz = z - cameraPos.z;
        lightScratchDist[index] = dx * dx + dy * dy + dz * dz;
    }

    /** Foco tan lejos de la camara que su caida ya lo deja sin contribucion. */
    private static boolean pastCullDistance(Vector3f cameraPos, Vector3f lightPos) {
        float dx = lightPos.x - cameraPos.x;
        float dy = lightPos.y - cameraPos.y;
        float dz = lightPos.z - cameraPos.z;
        float max = LIGHT_CULL_DISTANCE;
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    /** Crece el buffer de trabajo si el mapa tiene mas focos que su capacidad. */
    private void ensureLightScratch(int candidates) {
        if (lightScratchPos.length >= candidates * 3) {
            return;
        }
        lightScratchPos = new float[candidates * 3];
        lightScratchCol = new float[candidates * 3];
        lightScratchDist = new float[candidates];
        lightScratchUsed = new boolean[candidates];
    }

    public void registerCollisions(CollisionManager collisionManager) {
        // Muros, paneles de aberturas y dinteles: un AABB por caja dibujada,
        // generado junto con la geometria, asi que la huella de colision es
        // exactamente la huella visible.
        for (Aabb box : wallColliders) {
            collisionManager.addCollision(box);
        }
        // Ventanas: bloqueo invisible a toda altura (no se traspasan). Los
        // dinteles de las puertas ya estan en wallColliders, asi que de pie
        // bloquean y agachandose se pasa por debajo.
        for (Aabb box : openingColliders) {
            collisionManager.addCollision(box);
        }
        // Railings: bloquean el paso como un muro (colisionador oculto a toda
        // altura). Sus colisiones van en una lista aparte de los muros.
        for (Aabb box : railingColliders) {
            collisionManager.addRailingCollision(box);
        }
        // Los items no colisionan: son transitables.
    }

    public void cleanup() {
        freeGeometry();
        grid = null;
    }

    /**
     * Libera los modelos de decoracion compartidos.
     *
     * <p>No se hace en {@link #cleanup()} porque los niveles se recargan y las
     * mallas viven en una cache comun: si se liberaran en cada cambio de nivel,
     * volver a entrar al mismo nivel las volveria a subir. Se llama una sola
     * vez, al cerrar el juego.
     */
    public static void clearPropCache() {
        for (PropMesh mesh : PROP_CACHE.values()) {
            if (mesh != null) {
                mesh.cleanup();
            }
        }
        PROP_CACHE.clear();
    }

    /**
     * Si el nivel tiene algun item que dibujar.
     *
     * <p>Es una pregunta de si/no, no un contador, a proposito: el mapa no
     * lleva la cuenta de nada. Lo unico que se cuenta en el nivel son las
     * llaves ({@link #getTotalKeys()}); de los demas items solo se necesita
     * saber si hay algo que pintar.
     */
    public boolean hasAnyItem() {
        return hasAnyItem;
    }

    /** Llaves ('◈') que tenia el nivel: el numero al que hay que llegar. */
    public int getTotalKeys() {
        return totalKeys;
    }

    /** Llaves recogidas hasta ahora. El mapa y el tiempo no cuentan. */
    public int getCollectedKeys() {
        return collectedKeys;
    }

    /** Numero de piezas de decoracion colocadas en este nivel. */
    public int getTotalProps() {
        return totalProps;
    }

    /** Celdas con techo con luz ({@code ◉}) que encienden un foco propio. */
    public int getLitCeilingCount() {
        return staticLightPositions.size();
    }

    /** Focos fijos del nivel (uno por celda {@code ◉}), en orden de construccion. */
    public List<Vector3f> getStaticLightPositions() {
        return List.copyOf(staticLightPositions);
    }

    /** Posiciones (mundo) de los items que aún quedan en el mapa. */
    /**
     * En fácil el '☑' da tiempo en lugar de mapa, porque el minimapa ya viene
     * activado: un mapa que no revela nada no vale nada. Se declara ANTES de
     * construir el nivel, no al recoger, para que el modelo que flota encima
     * sea el que de verdad se va a coger.
     */
    public void setMapItemGivesTime(boolean daTiempo) {
        this.mapItemGivesTime = daTiempo;
    }

    /**
     * Los items que quedan en el nivel, para poder distinguirlos.
     *
     * <p>Devuelve la lista tal cual y no una copia para no crear una por frame
     * (o por test). Es de solo lectura: quien la recibe no puede meter ni quitar
     * items, y los que se recogen desaparecen de ella.
     */
    public List<Item> getItems() {
        return items;
    }

    public List<Vector3f> getItemPositions() {
        List<Vector3f> positions = new ArrayList<>(items.size());
        for (Item item : items) {
            positions.add(new Vector3f(item.getCenter()));
        }
        return positions;
    }

    public int getRows() {
        return grid.length;
    }

    public int getCols() {
        return grid[0].length;
    }

    /** Tipo de celda (solo lectura). */
    public SymbolType getCellAt(int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length) {
            return SymbolType.EMPTY;
        }
        return SymbolType.fromChar(grid[r][c]);
    }

    /** X del borde izquierdo del mapa en el mundo. */
    public float getOriginX() {
        return -((float) getCols()) * TILE_SIZE / 2f;
    }

    /** Z del borde superior del mapa en el mundo. */
    public float getOriginZ() {
        return -((float) getRows()) * TILE_SIZE / 2f;
    }

    public Vector3f getSpawnPosition() {
        return spawnPosition == null ? null : new Vector3f(spawnPosition);
    }

    /** Yaw inicial de la cámara según la flecha de spawn (0 = +X, 90 = +Z). */
    public float getSpawnYaw() {
        return spawnYaw;
    }

}