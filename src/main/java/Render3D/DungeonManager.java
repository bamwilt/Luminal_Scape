package Render3D;

import UtilsRender.Shader;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * Renderizador de mazmorras a partir de un mapa de texto.
 *
 * El nivel se define con un array de strings, uno por fila. Editar un nivel es
 * solo cambiar los caracteres de esas líneas; el mapa se guarda de forma mutable
 * (ver {@link #applyLayout}) dejando abierta la puerta a futuras mecánicas de
 * cambio de escenario.
 *
 * Tabla de símbolos (ver {@link SymbolType}):
 *   '▓'  muro horizontal (se fusiona en patrones)
 *   '■'  muro vertical (se fusiona en patrones)
 *   '▣'  ventana (ver {@link Window})
 *   '◧'  puerta (ver {@link Door})
 *   '░'  piso y techo: dos paneles texturizados (abajo y arriba)
 *   '◈'  item: un cubo que flota a la altura de la cámara, rota y brilla
 *        ligeramente; desaparece si el jugador pasa por su punto
 *   '▵'/'▿'/'▹'/'◃'  spawn del jugador: la flecha indica hacia dónde mira
 *   '□'/' '  vacío: no se construye nada (fácil de editar)
 *
 * Las alturas de aberturas y el cruce de muros se ajustan desde las constantes
 * de esta clase ({@link #WINDOW_SILL_HEIGHT}, {@link #WINDOW_HEADER_HEIGHT},
 * {@link #DOOR_HEADER_HEIGHT}, {@link #WALL_JOIN_OVERLAP}).
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
 * Editar un símbolo = editar una línea de {@code SymbolType} (carácter).
 * Editar las alturas de ventanas/puertas = editar {@link Window} / {@link Door}.
 * Para cambiar el comportamiento de todos los símbolos se toca un solo punto.
 *
 * Intersección de muros: los extremos de cada muro se extienden hasta el eje
 * de la pared perpendicular para cerrar las esquinas sin huecos hacia afuera.
 * El cruce se limita a un solapamiento mínimo ({@link #WALL_JOIN_OVERLAP}) en
 * la cara de la pared perpendicular, de modo que no se noten ambos muros
 * atravesándose. Un muro horizontal, cuando no tiene otro '▓' a su costado,
 * verifica si hay un '■' abajo (o arriba) y se intersecta con él; un muro
 * vertical verifica arriba y abajo buscando un '▓' para intersectarse.
 */
public class DungeonManager {

    // Geometría editable de la mazmorra.
    public static final float CELL_SIZE = 4.0f;
    public static final float WALL_THICKNESS = 1.0f;
    public static final float WALL_HEIGHT = 8.0f;
    public static final float WALL_CENTER_Y = WALL_HEIGHT / 2f;
    public static final float TEXTURE_SCALE = 4.0f;

    public static final float FLOOR_THICKNESS = 0.4f;
    public static final float ITEM_SIZE = 0.8f;
    public static final float ITEM_FLOAT_HEIGHT = 3.0f; // altura de la cámara
    public static final float ITEM_SPIN_SPEED = 1.5f;   // rad/seg
    public static final float ITEM_PICKUP_RADIUS = 1.5f;
    public static final float SPAWN_HEIGHT = 3.0f;

    // Aberturas: UN solo lugar para ajustar ventanas y puertas.
    public static final float WINDOW_SILL_HEIGHT = 2.0f;   // pared inferior de la ventana
    public static final float WINDOW_HEADER_HEIGHT = 5.6f; // pared superior de la ventana
    public static final float DOOR_HEADER_HEIGHT = 5.6f;   // hueco de la puerta: un poco sobre la cámara

    // Cruce mínimo de muros en las esquinas: sella sin que se vean cruzados.
    public static final float WALL_JOIN_OVERLAP = 0.1f;

    private final List<Wall> walls = new ArrayList<>();
    private final List<Wall> windowPanels = new ArrayList<>();
    private final List<Wall> windowColliders = new ArrayList<>();
    private final List<Wall> doorPanels = new ArrayList<>();
    private final List<Wall> floorsAndCeilings = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
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
     * Nivel de ejemplo: un cuarto cerrado con una ventana '▣' en el muro norte,
     * una ventana vertical '▣' en el muro este y una puerta '◧' en el muro sur,
     * piso y techo ('░'), el spawn ('▿' = el jugador mira al sur) y un item
     * ('◈') que recoger.
     */
    public static String[] sampleLayout() {
        return new String[] {
            "■■▣▣■▣▣■◙■▣■◙■■◙",
            "◙◫◫◫◙◫◫◫◧◫◫◫◧◫◫◙",
            "◙▶◫◫▣◫◈◫◙■▣■◙◫◫◙",
            "◙◫◫◫◙◫◫◫◙□□□◙◫◫◙",
            "◙■◧■■■◧■◙■◧■◙◫◫◙",
            "◙◫◈◫◈◫◈◫◧◫◫◫◫◫◫◙",
            "◙■▣▣▣▣▣■◙■■■◙■▣■",
        };
    }

    private void build(char[][] layout, int wallTexture, int floorTexture, int ceilingTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        float originX = -((float) cols) * CELL_SIZE / 2f;
        float originZ = -((float) rows) * CELL_SIZE / 2f;

        buildHorizontalCells(layout, originX, originZ, wallTexture);
        buildVerticalCells(layout, originX, originZ, wallTexture);
        buildOpenings(layout, originX, originZ, wallTexture);
        buildRooms(layout, originX, originZ, floorTexture, ceilingTexture);
        buildItems(layout, originX, originZ, wallTexture);
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

    // Regiones cerradas de '░' que representan cuartos. Cada grupo de '░' se
    // conecta por sus 4 lados y se expande hasta que a la derecha o abajo ya
    // no hay '░'. El piso y el techo se pintan como un cuadrado entre el '░'
    // más alto e izquierdo y el '░' más bajo y derecho del grupo.
    private void buildRooms(char[][] layout, float originX, float originZ, int floorTexture, int ceilingTexture) {
        int rows = layout.length;
        int cols = layout[0].length;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (SymbolType.fromChar(layout[r][c]) != SymbolType.FLOOR_CEILING || visited[r][c]) {
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

                    visitNeighbor(layout, visited, queue, cr - 1, cc);
                    visitNeighbor(layout, visited, queue, cr + 1, cc);
                    visitNeighbor(layout, visited, queue, cr, cc - 1);
                    visitNeighbor(layout, visited, queue, cr, cc + 1);
                }

                addRoomFloorAndCeiling(left, right, top, bottom, originX, originZ, floorTexture, ceilingTexture);
            }
        }
    }

    private void visitNeighbor(char[][] layout, boolean[][] visited, ArrayDeque<int[]> queue, int r, int c) {
        if (r < 0 || r >= layout.length || c < 0 || c >= layout[0].length) {
            return;
        }
        if (SymbolType.fromChar(layout[r][c]) != SymbolType.FLOOR_CEILING || visited[r][c]) {
            return;
        }
        visited[r][c] = true;
        queue.add(new int[] {r, c});
    }

    private void addRoomFloorAndCeiling(int left, int right, int top, int bottom,
                                        float originX, float originZ, int floorTexture, int ceilingTexture) {
        int widthCells = right - left + 1;
        int depthCells = bottom - top + 1;
        float centerX = originX + (left + widthCells / 2f) * CELL_SIZE;
        float centerZ = originZ + (top + depthCells / 2f) * CELL_SIZE;

        // Se extiende media celda por cada lado para quedar oculto bajo los muros.
        float width = (widthCells + 1) * CELL_SIZE;
        float depth = (depthCells + 1) * CELL_SIZE;

        Wall floor = new Wall(width, FLOOR_THICKNESS, depth, floorTexture, true);
        floor.setPosition(centerX, FLOOR_THICKNESS / 2f, centerZ);
        floor.setTexture(floorTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
        floorsAndCeilings.add(floor);

        Wall ceiling = new Wall(width, FLOOR_THICKNESS, depth, ceilingTexture, true);
        ceiling.setPosition(centerX, WALL_HEIGHT - FLOOR_THICKNESS / 2f, centerZ);
        ceiling.setTexture(ceilingTexture, TEXTURE_SCALE, TEXTURE_SCALE, true);
        floorsAndCeilings.add(ceiling);
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
        for (Item item : items) {
            item.box.cleanup();
        }
        walls.clear();
        windowPanels.clear();
        windowColliders.clear();
        doorPanels.clear();
        floorsAndCeilings.clear();
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
        // Los items no colisionan: son transitables.
    }

    public void cleanup() {
        freeGeometry();
        grid = null;
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

    /**
     * ÚNICO lugar donde se declaran los símbolos del nivel: carácter, si es
     * sólido, si se fusiona en patrones de pared, y la plantilla de abertura
     * (ventana o puerta) si aplica. Las ventanas y puertas se alinean solas
     * comprobando sus laterales.
     */
    public enum SymbolType {
EMPTY('□', false, false, null),
        SPAWN_N('▲', false, false, null),
        SPAWN_S('▼', false, false, null),
        SPAWN_E('▶', false, false, null),
        SPAWN_W('◀', false, false, null),
        H_WALL('■', true, true, null),
        V_WALL('◙', true, true, null),
        WINDOW('▣', true, false, new Window()),
        DOOR('◧', true, false, new Door()),
        FLOOR_CEILING('◫', false, false, null),
        ITEM('◈', false, false, null);

        private final char glyph;
        private final boolean solid;
        private final boolean merging;
        private final Opening opening;

        SymbolType(char glyph, boolean solid, boolean merging, Opening opening) {
            this.glyph = glyph;
            this.solid = solid;
            this.merging = merging;
            this.opening = opening;
        }

        public char glyph() {
            return glyph;
        }

        public boolean isSolid() {
            return solid;
        }

        public boolean isMerging() {
            return merging;
        }

        /** Muro horizontal ('▓'); las aberturas se alinean solas. */
        public boolean isHorizontal() {
            return this == H_WALL;
        }

        /** Muro vertical ('■'); las aberturas se alinean solas. */
        public boolean isVertical() {
            return this == V_WALL;
        }

        public boolean isOpening() {
            return this == WINDOW || this == DOOR;
        }

        public boolean isSpawn() {
            return this == SPAWN_N || this == SPAWN_S || this == SPAWN_E || this == SPAWN_W;
        }

        public Opening opening() {
            return opening;
        }


        static final SymbolType[] ALL = values();

        public static SymbolType fromChar(char c) {
            for (SymbolType type : ALL) {
                if (type.glyph() == c) {
                    return type;
                }
            }
            return EMPTY;
        }
    }
}