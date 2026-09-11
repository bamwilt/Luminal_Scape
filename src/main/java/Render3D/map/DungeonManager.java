package Render3D.map;

import Render3D.collision.CollisionManager;
import Render3D.graphics.Wall;
import UtilsRender.Shader;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

import static Render3D.map.MapConfig.SymbolType;
import static Render3D.map.MapConfig.CELL_SIZE;
import static Render3D.map.MapConfig.WALL_THICKNESS;
import static Render3D.map.MapConfig.WALL_HEIGHT;
import static Render3D.map.MapConfig.WALL_CENTER_Y;
import static Render3D.map.MapConfig.WALL_JOIN_OVERLAP;
import static Render3D.map.MapConfig.FLOOR_THICKNESS;
import static Render3D.map.MapConfig.TEXTURE_SCALE;
import static Render3D.map.MapConfig.ITEM_SIZE;
import static Render3D.map.MapConfig.ITEM_FLOAT_HEIGHT;
import static Render3D.map.MapConfig.ITEM_SPIN_SPEED;
import static Render3D.map.MapConfig.ITEM_PICKUP_RADIUS;
import static Render3D.map.MapConfig.SPAWN_HEIGHT;
import static Render3D.map.MapConfig.WINDOW_SILL_HEIGHT;
import static Render3D.map.MapConfig.WINDOW_HEADER_HEIGHT;
import static Render3D.map.MapConfig.DOOR_HEADER_HEIGHT;
import static Render3D.map.MapConfig.RAILING_HEIGHT;
import static Render3D.map.MapConfig.RAILING_THICKNESS;

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
 *   '■'  muro horizontal (se fusiona en patrones)
 *   '◙'  muro vertical (se fusiona en patrones)
 *   '▣'  ventana (ver {@link Window})
 *   '◧'  puerta (ver {@link Door})
 *   '◫'  piso y techo: dos paneles texturizados (abajo y arriba)
 *   '▒'  piso sin techo: solo panel inferior (pasillos abiertos / puentes)
 *   '◰'  railing: tramo de piso SIN techo con DOS muros bajos y finos
 *        ({@link MapConfig#RAILING_THICKNESS}) en sus bordes largos: una barra
 *        continua por segmento, extendida hasta los muros/puertas, más un
 *        colisionador oculto a toda altura para que el jugador no se pase (ver
 *        {@link Railing}); sus colisiones van en lista aparte
 *   '◈'  item: un cubo que flota a la altura de la cámara, rota y brilla
 *        ligeramente; desaparece si el jugador pasa por su punto
 *   '▲'/'▼'/'▶'/'◀'  spawn del jugador: la flecha indica hacia dónde mira
 *   '□'/' '  vacío: no se construye nada (fácil de editar)
 *
 * Pisos y techos inteligentes: cada región conexa (celdas de '◫'/'▒'
 * adyacentes por sus 4 lados, sin importar los muros) calcula su propio primer
 * y último; no hay un recuadro global. El techo solo aparece sobre celdas de
 * '◫', así que un pasillo o puente de '▒'/'◰' entre dos cuartos queda abierto
 * arriba.
 *
 * Las alturas de aberturas y el cruce de muros se ajustan desde las constantes
 * de {@link MapConfig} ({@link MapConfig#WINDOW_SILL_HEIGHT},
 * {@link MapConfig#WINDOW_HEADER_HEIGHT}, {@link MapConfig#DOOR_HEADER_HEIGHT},
 * {@link MapConfig#WALL_JOIN_OVERLAP}).
 *
 * Alineación flexible de ventanas y puertas: se recorren los 4 vecinos (N, S,
 * E, O) y se usa la orientación del PRIMER muro encontrado ('▓' -> horizontal,
 * '■' -> vertical); si el vecino es otra ventana o puerta, se copia su
 * orientación. Sin ninguna referencia, la celda se convierte en un muro
 * estándar (horizontal). Esta regla aplica a ventanas y puertas.
 *
 * Spawn único: si hay más de un spawn, se borra el anterior encontrado y se
 * cambia por piso ('░'); permanece el último.
 *
 * Editar un símbolo = editar una línea de {@link MapConfig.SymbolType}.
 * Editar las alturas de ventanas/puertas = editar {@link Window} / {@link Door}.
 * Para cambiar el comportamiento de todos los símbolos se toca un solo punto.
 *
 * Intersección de muros: los extremos de cada muro se extienden hasta el eje
 * de la pared perpendicular para cerrar las esquinas sin huecos hacia afuera.
 * El cruce se limita a un solapamiento mínimo ({@link MapConfig#WALL_JOIN_OVERLAP}) en
 * la cara de la pared perpendicular, de modo que no se noten ambos muros
 * atravesándose. Un muro horizontal, cuando no tiene otro '▓' a su costado,
 * verifica si hay un '■' abajo (o arriba) y se intersecta con él; un muro
 * vertical verifica arriba y abajo buscando un '▓' para intersectarse.
 */
public class DungeonManager {

    private final List<Wall> walls = new ArrayList<>();
    private final List<Wall> windowPanels = new ArrayList<>();
    private final List<Wall> windowColliders = new ArrayList<>();
    private final List<Wall> doorPanels = new ArrayList<>();
    private final List<Wall> floorsAndCeilings = new ArrayList<>();
    private final List<Railing> railings = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private int totalItems = 0;
    private Vector3f spawnPosition;
    private float spawnYaw = -90f;

    // Mapa mutable: deja abierta la puerta a futuros cambios de escenario.
    private char[][] grid;

    public DungeonManager(String[] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        grid = toCharGrid(layout);
        build(grid, wallTexture, floorTexture, ceilingTexture);
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

    private void build(char[][] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        float originX = -((float) cols) * CELL_SIZE / 2f;
        float originZ = -((float) rows) * CELL_SIZE / 2f;

        buildHorizontalCells(layout, originX, originZ, wallTexture);
        buildVerticalCells(layout, originX, originZ, wallTexture);
        buildRailings(layout, originX, originZ, wallTexture);
        buildOpenings(layout, originX, originZ, wallTexture);
        buildFloorsAndCeilings(layout, originX, originZ, floorTexture, ceilingTexture);
        buildRailingFloors(layout, originX, originZ, floorTexture);
        buildItems(layout, originX, originZ, wallTexture);
        totalItems = items.size();
        spawnPosition = findSpawn(layout, originX, originZ);
    }

    // Celdas horizontales fila por fila. Los muros '▓' consecutivos se fusionan
    // en UNA pared.
    private void buildHorizontalCells(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                if (SymbolType.fromChar(layout[r][c]) != SymbolType.H_WALL) {
                    c++;
                    continue;
                }
                int start = c;
                while (c < cols && SymbolType.fromChar(layout[r][c]) == SymbolType.H_WALL) {
                    c++;
                }
                addHorizontalWall(layout, r, start, c, originX, originZ, wallTexture);
            }
        }
    }

    // Celdas verticales columna por columna. Igual que la pasada horizontal
    // pero girando 90°, e intersectando con '▓' arriba/abajo.
    private void buildVerticalCells(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int c = 0; c < cols; c++) {
            int r = 0;
            while (r < rows) {
                if (SymbolType.fromChar(layout[r][c]) != SymbolType.V_WALL) {
                    r++;
                    continue;
                }
                int start = r;
                while (r < rows && SymbolType.fromChar(layout[r][c]) == SymbolType.V_WALL) {
                    r++;
                }
                float zT = originZ + start * CELL_SIZE;
                float zB = originZ + r * CELL_SIZE;
                if (start - 1 >= 0 && cellNearestRowIsHorizontal(layout, start - 1, c)) {
                    zT = originZ + (start - 0.5f) * CELL_SIZE + WALL_THICKNESS / 2f + WALL_JOIN_OVERLAP;
                }
                if (r < rows && cellNearestRowIsHorizontal(layout, r, c)) {
                    zB = originZ + (r + 0.5f) * CELL_SIZE - WALL_THICKNESS / 2f - WALL_JOIN_OVERLAP;
                }
                addSolidWall(WALL_THICKNESS, zB - zT, originX + (c + 0.5f) * CELL_SIZE, (zT + zB) / 2f, wallTexture);
            }
        }
    }

    // Un muro horizontal, en el extremo de su recorrido (donde no hay otro
    // a su lado), se intersecta con un '■' directamente abajo o arriba.
    private void addHorizontalWall(char[][] layout, int r, int start, int endExclusive,
                                   float originX, float originZ, int wallTexture) {
        float xL = originX + start * CELL_SIZE;
        float xR = originX + endExclusive * CELL_SIZE;
        if (start - 1 >= 0 && cellNearestColumnIsVertical(layout, r, start - 1)) {
            xL = originX + (start - 0.5f) * CELL_SIZE + WALL_THICKNESS / 2f + WALL_JOIN_OVERLAP;
        }
        if (endExclusive < layout[0].length && cellNearestColumnIsVertical(layout, r, endExclusive)) {
            xR = originX + (endExclusive + 0.5f) * CELL_SIZE - WALL_THICKNESS / 2f - WALL_JOIN_OVERLAP;
        }
        addSolidWall(xR - xL, WALL_THICKNESS, (xL + xR) / 2f, originZ + (r + 0.5f) * CELL_SIZE, wallTexture);
    }

    // Ventanas y puertas NO llevan orientación fija: toman la del primer muro
    // (o abertura) vecino que encuentren (ver resolveOrientation).
    private void buildOpenings(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isOpening()) {
                    continue;
                }
                CellAxis axis = resolveOrientation(layout, r, c);
                if (axis == null) {
                    addHorizontalWall(layout, r, c, c + 1, originX, originZ, wallTexture);
                    continue;
                }
                float width = axis == CellAxis.HORIZONTAL ? CELL_SIZE : WALL_THICKNESS;
                float depth = axis == CellAxis.HORIZONTAL ? WALL_THICKNESS : CELL_SIZE;
                addOpeningCell(type, width, depth, r, c, originX, originZ, wallTexture);
            }
        }
    }

    /** Orientación de una abertura resuelta a partir de las 4 direcciones. */
    private enum CellAxis { HORIZONTAL, VERTICAL }

    /**
     * Orientación flexible: se recorren los 4 vecinos (N, S, E, O) y se usa la
     * orientación del PRIMER muro encontrado ('▓' -> horizontal, '■' ->
     * vertical). Si el vecino es otra abertura, se copia su orientación
     * (resuelta de forma recursiva, con guarda anticiclos). Sin ninguna
     * referencia -> abertura sin orientación (muro estándar).
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

    private boolean cellNearestColumnIsVertical(char[][] layout, int r, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            int rr = r + dr;
            if (rr < 0 || rr >= layout.length) {
                continue;
            }
            if (SymbolType.fromChar(layout[rr][col]).isVertical()) {
                return true;
            }
        }
        return false;
    }

    private boolean cellNearestRowIsHorizontal(char[][] layout, int row, int c) {
        for (int dc = -1; dc <= 1; dc++) {
            int cc = c + dc;
            if (cc < 0 || cc >= layout[0].length) {
                continue;
            }
            if (SymbolType.fromChar(layout[row][cc]).isHorizontal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pisos y techos inteligentes: dos pasadas independientes sobre regiones
     * conexas (celdas adyacentes por sus 4 lados, sin importar si hay muro,
     * agrupando solo celdas del mismo grupo de piso):
     *
     *   - PISOS: agrupa '◫' y '▒' (cualquier celda transitable EXCEPTO '◰').
     *     Cada región dibuja UN panel inferior con su caja contenedora exacta.
     *     El '◰' se excluye porque su piso es por celda ({@link
     *     #buildRailingFloors}), independiente de la región, para no tapar los
     *     huecos '□' que lo rodean.
     *   - TECHOS: agrupa solo '◫' (piso con techo). Las celdas de '▒'/'◰' (piso
     *     sin techo) NUNCA suman a un techo: un pasillo o puente entre dos
     *     cuartos queda abierto arriba.
     *
     * Así cada cuarto calcula su propio primer/último (expansión conexa) y no
     * hay un único recuadro global para todo el mapa.
     */
    private void buildFloorsAndCeilings(char[][] layout, float originX, float originZ,
                                        int floorTexture, int ceilingTexture) {
        buildRegionQuads(layout, originX, originZ, floorTexture, ceilingTexture,
                type -> type.isWalkable() && type != SymbolType.RAILING, true, false);
        buildRegionQuads(layout, originX, originZ, floorTexture, ceilingTexture,
                type -> type == SymbolType.FLOOR_CEILING, false, true);
    }

    /**
     * Expande una región conexa (4 vecinos) de celdas que cumplen {@code member}
     * y construye un panel por región. La expansión se hace solo con celdas
     * directamente adyacentes del mismo miembro (no depende de muros), como el
     * recorrido "serpiente" clásico pero en una sola pasada y con el resultado
     * exacto: el primer/último de cada habitación se obtiene con una BFS que
     * toca solo las celdas de la región.
     */
    private void buildRegionQuads(char[][] layout, float originX, float originZ,
                                  int floorTexture, int ceilingTexture,
                                  java.util.function.Predicate<SymbolType> member,
                                  boolean emitFloor, boolean emitCeiling) {
        int rows = layout.length;
        int cols = layout[0].length;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!member.test(SymbolType.fromChar(layout[r][c])) || visited[r][c]) {
                    continue;
                }
                int top = r, bottom = r, left = c, right = c;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                visited[r][c] = true;
                queue.add(new int[] {r, c});

                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    int cr = cell[0];
                    int cc = cell[1];
                    top = Math.min(top, cr);
                    bottom = Math.max(bottom, cr);
                    left = Math.min(left, cc);
                    right = Math.max(right, cc);

                    visitRegionNeighbor(layout, visited, queue, cr - 1, cc, member);
                    visitRegionNeighbor(layout, visited, queue, cr + 1, cc, member);
                    visitRegionNeighbor(layout, visited, queue, cr, cc - 1, member);
                    visitRegionNeighbor(layout, visited, queue, cr, cc + 1, member);
                }

                addRegionFloorAndCeiling(left, right, top, bottom, originX, originZ,
                        floorTexture, ceilingTexture, emitFloor, emitCeiling);
            }
        }
    }

    private void visitRegionNeighbor(char[][] layout, boolean[][] visited, ArrayDeque<int[]> queue,
                                     int r, int c, java.util.function.Predicate<SymbolType> member) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return;
        }
        SymbolType type = SymbolType.fromChar(layout[r][c]);
        if (!member.test(type) || visited[r][c]) {
            return;
        }
        visited[r][c] = true;
        queue.add(new int[] {r, c});
    }

    private void addRegionFloorAndCeiling(int left, int right, int top, int bottom,
                                          float originX, float originZ, int floorTexture, int ceilingTexture,
                                          boolean emitFloor, boolean emitCeiling) {
        int widthCells = right - left + 1;
        int depthCells = bottom - top + 1;
        float centerX = originX + (left + widthCells / 2f) * CELL_SIZE;
        float centerZ = originZ + (top + depthCells / 2f) * CELL_SIZE;

        // Se extiende media celda por cada lado para quedar oculto bajo los muros.
        float width = (widthCells + 1) * CELL_SIZE;
        float depth = (depthCells + 1) * CELL_SIZE;

        if (emitFloor) {
            Wall floor = new Wall(width, FLOOR_THICKNESS, depth, floorTexture, true);
            floor.setPosition(centerX, FLOOR_THICKNESS / 2f, centerZ);
            floor.setTexture(floorTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
            floorsAndCeilings.add(floor);
        }
        if (emitCeiling) {
            Wall ceiling = new Wall(width, FLOOR_THICKNESS, depth, ceilingTexture, true);
            ceiling.setPosition(centerX, WALL_HEIGHT - FLOOR_THICKNESS / 2f, centerZ);
            ceiling.setTexture(ceilingTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
            floorsAndCeilings.add(ceiling);
        }
    }

    /**
     * Piso de cada railing '◰': UN panel por CELDA, nunca un recuadro de
     * región. Así los huecos '□' que rodean al railing quedan vacíos (no los
     * cubre un panel global) y cada tramo se comporta de forma independiente;
     * los tramos contiguos se tocan borde con borde, sin solaparse.
     *
     * La celda estira media celda SOLO hacia un vecino sólido (muro, ventana o
     * puerta), para esconder el borde bajo él y salvar los umbrales de las
     * puertas; nunca hacia un vacío '□', ni hacia otro '◰' (ese lado ya lo
     * cubre el piso de la celda vecina) ni hacia el exterior del mapa.
     */
    private void buildRailingFloors(char[][] layout, float originX, float originZ, int floorTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isRailingSurface(layout, r, c)) {
                    continue;
                }
                float xL = originX + c * CELL_SIZE;
                float xR = originX + (c + 1) * CELL_SIZE;
                float zT = originZ + r * CELL_SIZE;
                float zB = originZ + (r + 1) * CELL_SIZE;
                if (isSolidNeighbor(layout, r, c - 1)) xL -= CELL_SIZE / 2f;
                if (isSolidNeighbor(layout, r, c + 1)) xR += CELL_SIZE / 2f;
                if (isSolidNeighbor(layout, r - 1, c)) zT -= CELL_SIZE / 2f;
                if (isSolidNeighbor(layout, r + 1, c)) zB += CELL_SIZE / 2f;

                Wall floor = new Wall(xR - xL, FLOOR_THICKNESS, zB - zT, floorTexture, true);
                floor.setPosition((xL + xR) / 2f, FLOOR_THICKNESS / 2f, (zT + zB) / 2f);
                floor.setTexture(floorTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
                floorsAndCeilings.add(floor);
            }
        }
    }

    // Un vecino sólido (muro, ventana o puerta) cubre el borde del piso del
    // railing y merece la estirada de media celda; un '□' abierto u otro '◰'
    // (su piso ya está al lado) no debe recibirla.
    private boolean isSolidNeighbor(char[][] layout, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return false;
        }
        SymbolType type = SymbolType.fromChar(layout[r][c]);
        return type.isSolid() && type != SymbolType.RAILING;
    }

    /**
     * Railing ('◰'): tramo de piso SIN techo (el piso se pinta por celda, ver
     * {@link #buildRailingFloors}) con un muro bajo de 2.0 en CADA lado que da
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
    private void buildRailings(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;

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
                    float xL = originX + segStart * CELL_SIZE;
                    float xR = originX + c * CELL_SIZE;
                    if (extendsHorizAt(layout, b, segStart - 1)) {
                        xL -= CELL_SIZE / 2f;
                    }
                    if (extendsHorizAt(layout, b, c)) {
                        xR += CELL_SIZE / 2f;
                    }
                    addRailingBar((xL + xR) / 2f, originZ + b * CELL_SIZE,
                            xR - xL, RAILING_THICKNESS, wallTexture);
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
                    float zT = originZ + segStart * CELL_SIZE;
                    float zB = originZ + r * CELL_SIZE;
                    if (extendsVertAt(layout, segStart - 1, d)) {
                        zT -= CELL_SIZE / 2f;
                    }
                    if (extendsVertAt(layout, r, d)) {
                        zB += CELL_SIZE / 2f;
                    }
                    addRailingBar(originX + d * CELL_SIZE, (zT + zB) / 2f,
                            RAILING_THICKNESS, zB - zT, wallTexture);
                    segStart = -1;
                }
            }
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
                || type == SymbolType.ITEM
                || type.isSpawn();
    }

    private void addRailingBar(float centerX, float centerZ, float width, float depth, int wallTexture) {
        railings.add(new Railing(width, depth, centerX, centerZ, wallTexture));
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
        if (SymbolType.fromChar(glyph) != SymbolType.ITEM) {
            return false;
        }
        return isRailing(layout, r, c - 1) || isRailing(layout, r, c + 1)
                || isRailing(layout, r - 1, c) || isRailing(layout, r + 1, c);
    }

    // Items: un cubo que flota a la altura de la cámara.
    private void buildItems(char[][] layout, float originX, float originZ, int wallTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (SymbolType.fromChar(layout[r][c]) == SymbolType.ITEM) {
                    Vector3f center = new Vector3f(
                            originX + (c + 0.5f) * CELL_SIZE,
                            ITEM_FLOAT_HEIGHT,
                            originZ + (r + 0.5f) * CELL_SIZE);
                    Wall cube = new Wall(ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, wallTexture, true);
                    cube.setPosition(center.x, center.y, center.z);
                    cube.setTexture(wallTexture, 1.0f, 1.0f, true);
                    items.add(new Item(cube, center));
                }
            }
        }
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
                originX + (lastC + 0.5f) * CELL_SIZE,
                SPAWN_HEIGHT,
                originZ + (lastR + 0.5f) * CELL_SIZE);
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
                                float originX, float originZ, int wallTexture) {
        if (type.opening() == null) {
            return;
        }
        float centerX = originX + (c + 0.5f) * CELL_SIZE;
        float centerZ = originZ + (r + 0.5f) * CELL_SIZE;
        for (Wall panel : type.opening().buildPanels(width, depth, centerX, centerZ,
                wallTexture, WALL_HEIGHT, TEXTURE_SCALE)) {
            addPanel(type, panel);
        }
        // Las ventanas NUNCA se traspasan: el hueco visual queda, pero se
        // registra un colisionador invisible a toda altura para que el jugador
        // no pase por el boquete (las puertas sí son transitables agachándose).
        if (type.opening() instanceof Window) {
            Wall blocker = new Wall(width, WALL_HEIGHT, depth, wallTexture, true);
            blocker.setPosition(centerX, WALL_CENTER_Y, centerZ);
            blocker.setTexture(wallTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
            windowColliders.add(blocker);
        }
    }

    // Los paneles se separan solo por plantilla: puertas a su lista (para
    // poder registrarlas en la colisión sin mezclarlas con las ventanas).
    private void addPanel(SymbolType type, Wall panel) {
        if (type.opening() instanceof Door) {
            doorPanels.add(panel);
        } else {
            windowPanels.add(panel);
        }
    }

    private void addSolidWall(float width, float depth, float x, float z, int wallTexture) {
        Wall wall = new Wall(width, WALL_HEIGHT, depth, wallTexture, true);
        wall.setPosition(x, WALL_CENTER_Y, z);
        wall.setTexture(wallTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
        walls.add(wall);
    }

    /**
     * Reemplaza el nivel completo y reconstruye toda la geometría. Pensado para
     * futuras mecánicas de cambio de escenario: tras llamarlo hay que volver a
     * registrar las colisiones y reposicionar al jugador en el nuevo spawn.
     */
    public void applyLayout(String[] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        freeGeometry();
        grid = toCharGrid(layout);
        build(grid, wallTexture, floorTexture, ceilingTexture);
    }

    private void freeGeometry() {
        for (Wall wall : walls) {
            wall.cleanup();
        }
        for (Wall panel : windowPanels) {
            panel.cleanup();
        }
        for (Wall collider : windowColliders) {
            collider.cleanup();
        }
        for (Wall panel : doorPanels) {
            panel.cleanup();
        }
        for (Wall floorOrCeiling : floorsAndCeilings) {
            floorOrCeiling.cleanup();
        }
        for (Railing railing : railings) {
            railing.cleanup();
        }
        for (Item item : items) {
            item.box.cleanup();
        }
        walls.clear();
        windowPanels.clear();
        windowColliders.clear();
        doorPanels.clear();
        floorsAndCeilings.clear();
        railings.clear();
        items.clear();
        spawnPosition = null;
    }

    /**
     * Actualiza los items: rotan en el aire y los que estén en el mismo punto
     * que el jugador desaparecen. Devuelve {@code true} si se recogió alguno.
     */
    public boolean update(Vector3f playerPos, float deltaTime) {
        for (Item item : items) {
            item.box.rotate(0f, ITEM_SPIN_SPEED * deltaTime, 0f);
        }
        int before = items.size();
        items.removeIf(item -> {
            float dx = item.center.x - playerPos.x;
            float dy = item.center.y - playerPos.y;
            float dz = item.center.z - playerPos.z;
            boolean pickedUp = dx * dx + dy * dy + dz * dz < ITEM_PICKUP_RADIUS * ITEM_PICKUP_RADIUS;
            if (pickedUp) {
                item.box.cleanup();
            }
            return pickedUp;
        });
        return items.size() < before;
    }

    public void render(Shader shader) {
        for (Wall wall : walls) {
            wall.render(shader);
        }
        for (Wall panel : windowPanels) {
            panel.render(shader);
        }
        for (Wall panel : doorPanels) {
            panel.render(shader);
        }
        for (Wall floorOrCeiling : floorsAndCeilings) {
            floorOrCeiling.render(shader);
        }
        for (Railing railing : railings) {
            railing.getWall().render(shader);
        }
    }

    /** Renderiza los items con su shader propio (brillo ligero). */
    public void renderItems(Shader itemShader, float time) {
        for (Item item : items) {
            itemShader.setFloat("time", time);
            item.box.render(itemShader);
        }
    }

    public void registerCollisions(CollisionManager collisionManager) {
        for (Wall wall : walls) {
            collisionManager.addCollision(wall);
        }
        for (Wall panel : windowPanels) {
            collisionManager.addCollision(panel);
        }
        // Ventanas: bloqueo total a toda altura (no se traspasan).
        for (Wall collider : windowColliders) {
            collisionManager.addCollision(collider);
        }
        // Las puertas también colisionan en su parte sólida (el dintel): de pie
        // bloquean, agachándose se pasa por debajo. El hueco de la puerta queda
        // libre de colisión.
        for (Wall panel : doorPanels) {
            collisionManager.addCollision(panel);
        }
        // Railings: bloquean el paso como un muro (colisionador oculto a toda
        // altura). Sus colisiones van en una lista aparte de los muros.
        for (Railing railing : railings) {
            collisionManager.addRailingCollision(railing.getCollider());
        }
        // Los items no colisionan: son transitables.
    }

    public void cleanup() {
        freeGeometry();
        grid = null;
    }

    /** Total de items que tenía el nivel al construirse. */
    public int getTotalItems() {
        return totalItems;
    }

    /** Items recogidos hasta ahora = total - los que quedan en el mapa. */
    public int getCollectedItems() {
        return totalItems - items.size();
    }

    /** Posiciones (mundo) de los items que aún quedan en el mapa. */
    public List<Vector3f> getItemPositions() {
        List<Vector3f> positions = new ArrayList<>(items.size());
        for (Item item : items) {
            positions.add(new Vector3f(item.center));
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
        return -((float) getCols()) * CELL_SIZE / 2f;
    }

    /** Z del borde superior del mapa en el mundo. */
    public float getOriginZ() {
        return -((float) getRows()) * CELL_SIZE / 2f;
    }

    public Vector3f getSpawnPosition() {
        return spawnPosition == null ? null : new Vector3f(spawnPosition);
    }

    /** Yaw inicial de la cámara según la flecha de spawn (0 = +X, 90 = +Z). */
    public float getSpawnYaw() {
        return spawnYaw;
    }

    private static class Item {
        final Wall box;
        final Vector3f center;

        Item(Wall box, Vector3f center) {
            this.box = box;
            this.center = center;
        }
    }
}