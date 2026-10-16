package Render2D.components;

import Render3D.map.DungeonManager;
import Render3D.map.MapConfig;
import java.util.ArrayDeque;
import org.joml.Vector3f;

/**
 * Minimapa en pantalla (abajo a la derecha): dibuja la grilla del nivel con
 * quads de color — celdas sólidas en azul celeste, pisos en gris celeste,
 * items restantes como puntos amarillos y el player como un triángulo cyan
 * que apunta a su dirección (usa el "front" de la cámara).
 *
 * El marcador del player se dibuja <b>encajado al centro de su celda</b> y solo
 * mira en 4 direcciones (múltiplos de 90°): se mueve "tosco", bloque a bloque,
 * como un mapa de RPG antiguo. El personaje en 3D sigue moviéndose fino; esto es
 * solo cómo se pinta el plano.
 *
 * <p>La celda donde cae el marcador no es sin más la que contiene al player: si
 * el centro queda un pelín metido en la celda del muro al caminar pegado a él, se
 * encaja a la celda de <b>piso transitable más cercana</b>. Así el triángulo nunca
 * se pinta sobre un muro ni sobre el aire.
 *
 * El tamaño no se elige a mano: sale de la pantalla (una fracción del lado
 * menor) para que ocupe siempre el mismo lugar visible, y por eso el botón de
 * tamaño que había en el menú de pausa ya no hace falta.
 *
 * No dibuja por sí mismo: añade primitivas al {@link QuadBatch} compartido que
 * GameLoop se encarga de renderizar una vez por frame.
 */
public class Minimap {

    private final DungeonManager dungeon;
    private boolean visible = false;

    /** Fracción del lado menor de la pantalla que ocupa el mapa. */
    private static final float SCREEN_RATIO = 0.26f;
    private static final float MIN_SIZE = 90f;
    private static final float MAX_SIZE = 300f;

    private final float margin = 12f;

    /**
     * Cuando es {@code true} se dibuja todo el nivel, no solo lo que ronda a
     * los artefactos. Lo activa {@link #revealAll()}, que es lo que pasa al
     * recoger el item de mapa (y al empezar en fácil, donde ya se trae el mapa
     * de serie).
     */
    private boolean revealedAll = false;

    /** Vecinos a explorar (8 direcciones) alrededor de cada item. */
    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Minimap(DungeonManager dungeon) {
        this.dungeon = dungeon;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Revela el plano completo y muestra el minimapa.
     *
     * <p>Un mapa sirve justo para eso: al recogerlo se ve la planta entera del
     * nivel, no un cacho alrededor de un punto. Va pegado con
     * {@link #setVisible(boolean)} porque reveals tiene sentido, pero un
     * minimapa escondido no aporta nada.
     */
    public void revealAll() {
        this.revealedAll = true;
        this.visible = true;
    }

    /** Si el plano completo está revelado (item de mapa o dificultad fácil). */
    public boolean isRevealedAll() {
        return revealedAll;
    }

    /**
     * Deja el minimapa como recién creado: apagado y sin nada revelado.
     *
     * <p>Se llama al cargar cada nivel, y no solo al empezar la partida. El
     * plano de un nivel no sirve de nada en el siguiente: las habitaciones son
     * distintas, y sin esto el mapa recogido en el nivel 2 se seguia viendo
     * entero en el 3 y el player se lo ahorraba. El item de mapa vale una vez
     * por nivel, igual que el tiempo.
     */
    public void reset() {
        this.revealedAll = false;
        this.visible = false;
    }

    /**
     * Lado del mapa en unidades de interfaz, para esta pantalla.
     *
     * <p>Se deriva del lado menor con {@link #SCREEN_RATIO} y se recorta entre
     * {@link #MIN_SIZE} y {@link #MAX_SIZE}: en una window baja el mapa
     * encoge con ella en vez de comerse medio escenario, y en una muy ancha no
     * se convierte en un mural.
     */
    private float sizePx(int screenWidth, int screenHeight) {
        float lado = Math.min(screenWidth, screenHeight) * SCREEN_RATIO;
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, lado));
    }

    /**
     * Índice de la celda que contiene una coordenada de mundo.
     *
     * <p>El marcador del minimapa se dibuja encajado a la celda —avanza bloque a
     * bloque, como un mapa de RPG—, y para eso hace falta saber en qué celda está
     * el player aunque en 3D se mueva fino.
     */
    public static int cellIndex(float worldCoord, float origin) {
        return (int) Math.floor((worldCoord - origin) / MapConfig.TILE_SIZE);
    }

    /**
     * Encaja un ángulo a la dirección cardinal más cercana (múltiplos de 90°) y
     * lo normaliza a [0, 360).
     *
     * <p>La flecha del minimapa solo apunta en 4 direcciones: nunca se ve en
     * diagonal. El personaje en 3D sigue girando con el ratón como siempre; esto
     * es solo cómo se dibuja la flecha del plano.
     */
    public static float snapYaw90(float yaw) {
        float snapped = Math.round(yaw / 90f) * 90f;
        return ((snapped % 360f) + 360f) % 360f;
    }

    /**
     * Consulta el tipo de una celda del nivel.
     *
     * <p>Es una interfaz mínima para poder comprobar el encaje del marcador sin
     * abrir una ventana ni construir un {@link DungeonManager} (que necesita
     * GPU): en los tests basta con responder desde una matriz.
     */
    @FunctionalInterface
    public interface CellQuery {
        MapConfig.SymbolType at(int r, int c);
    }

    /**
     * Si una celda es piso sobre el que se puede estar de pie.
     *
     * <p>Es suelo con o sin techo, luz, decoración, spawn o item: todo lo que
     * tiene panel de suelo y no bloquea. Quedan fuera muros, ventanas,
     * barandillas y puertas ({@code isSolid}) y el vacío. Justo lo que el usuario
     * pidió: el triángulo, sobre piso; nunca sobre un muro ni sobre el aire.
     */
    public static boolean esPiso(MapConfig.SymbolType type) {
        return type != null && type.needsFloor() && !type.isSolid();
    }

    /**
     * La celda de piso más cercana a una posición de mundo, en coordenadas de
     * celda (con parte fraccionaria).
     *
     * <p>Se recorre todo el nivel y se elige el centro de celda pisable más
     * próximo: es barato (el grid es pequeño) y exacto, sin depender de anillos.
     *
     * @param relR fila del player en celdas ({@code (z - originZ) / TILE_SIZE})
     * @param relC columna del player en celdas ({@code (x - originX) / TILE_SIZE})
     * @return {@code {fila, columna}} o {@code null} si no hay ningún piso
     */
    public static int[] nearestFloorCell(float relR, float relC, int rows, int cols, CellQuery cells) {
        int bestR = -1;
        int bestC = -1;
        float bestDist = Float.MAX_VALUE;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!esPiso(cells.at(r, c))) {
                    continue;
                }
                float dr = (r + 0.5f) - relR;
                float dc = (c + 0.5f) - relC;
                float dist = dr * dr + dc * dc;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestR = r;
                    bestC = c;
                }
            }
        }
        return bestR < 0 ? null : new int[] {bestR, bestC};
    }

    /**
 * Color con el que se pinta una celda concreta.
 *
 * <p>La regla que no se ve a ojo: una celda de item solo lleva el color de la
 * llave si el item sigue en el nivel ({@code occupied}). Al recogerlo, su
 * símbolo sigue escrito en el mapa, así que sin el segundo argumento el plano
 * seguiría enseñando llaves que ya no existen.
 *
 * <p>Se separa de {@link #colorDe(MapConfig.SymbolType)} para poder comprobarlo
 * sin GPU ni ventana.
 */
public static float[] colorDeCelda(MapConfig.SymbolType type, boolean occupied) {
    if (isItem(type) && !occupied) {
        // El hueco que dejó el item se ve como suelo, no como una mancha
        // dorada flotando sobre el plano.
        return colorDe(MapConfig.SymbolType.FLOOR_CEILING);
    }
    return colorDe(type);
}

/**
 * Si el símbolo es un item (llave '◈' o mapa '☑').
 *
 * <p>Se separa del resto porque estas celdas se pueden quedar vacías: el item
 * se recoge y desaparece del mundo, pero su símbolo sigue escrito en el mapa.
 */
static boolean isItem(MapConfig.SymbolType type) {
    return type == MapConfig.SymbolType.ITEM
            || type == MapConfig.SymbolType.MAP_ITEM;
}

/**
 * Si el símbolo es parte de la silueta de una habitación (muro, ventana,
 * puerta), o sea algo que se pinta al borde del suelo revelado aunque no se
 * pueda recorrer.
 */
public static boolean isStructure(MapConfig.SymbolType type) {
    return type == MapConfig.SymbolType.H_WALL
            || type == MapConfig.SymbolType.V_WALL
            || type == MapConfig.SymbolType.WINDOW
            || type == MapConfig.SymbolType.DOOR;
}

/**
 * Color del minimapa para un tipo de celda, como {r, g, b, alfa}.
 *
 * <p>Un color por tipo, tomado de {@link MapConfig}: muro, ventana, puerta,
 * barandilla, suelo con techo y suelo sin techo se distinguen de un vistazo.
 * Los valores viven en MapConfig porque son configuración del juego, no
 * detalle de dibujo.
 */
public static float[] colorDe(MapConfig.SymbolType type) {
    switch (type) {
        case H_WALL:
        case V_WALL:
            return new float[] {MapConfig.MINIMAP_WALL_R, MapConfig.MINIMAP_WALL_G,
                    MapConfig.MINIMAP_WALL_B, MapConfig.MINIMAP_WALL_A};
        case WINDOW:
            return new float[] {MapConfig.MINIMAP_WINDOW_R, MapConfig.MINIMAP_WINDOW_G,
                    MapConfig.MINIMAP_WINDOW_B, MapConfig.MINIMAP_WINDOW_A};
        case DOOR:
            return new float[] {MapConfig.MINIMAP_DOOR_R, MapConfig.MINIMAP_DOOR_G,
                    MapConfig.MINIMAP_DOOR_B, MapConfig.MINIMAP_DOOR_A};
        case RAILING:
            return new float[] {MapConfig.MINIMAP_RAILING_R, MapConfig.MINIMAP_RAILING_G,
                    MapConfig.MINIMAP_RAILING_B, MapConfig.MINIMAP_RAILING_A};
        case ITEM:
        case MAP_ITEM:
            return new float[] {MapConfig.MINIMAP_ITEM_R, MapConfig.MINIMAP_ITEM_G,
                    MapConfig.MINIMAP_ITEM_B, MapConfig.MINIMAP_ITEM_A};
        case FLOOR_ONLY:
            return new float[] {MapConfig.MINIMAP_FLOOR_ONLY_R, MapConfig.MINIMAP_FLOOR_ONLY_G,
                    MapConfig.MINIMAP_FLOOR_ONLY_B, MapConfig.MINIMAP_FLOOR_ONLY_A};
        default:
            // FLOOR_CEILING, LIT_CEILING y spawns: gris neutro, es el fondo.
            return new float[] {MapConfig.MINIMAP_FLOOR_R, MapConfig.MINIMAP_FLOOR_G,
                    MapConfig.MINIMAP_FLOOR_B, MapConfig.MINIMAP_FLOOR_A};
    }
}

/**
     * Vértices de la flecha del player, en coordenadas de pantalla.
     *
     * <p>Es un método aparte y estático a propósito: así la geometría se puede
     * comprobar sin abrir una window (ver {@code Render2D.MinimapArrowChecks}).
     * Devuelve {@code x0,y0, x1,y1, x2,y2} con la punta primero.
     *
     * <p>El vector de frente ({@code fwdX},{@code fwdY}) debe venir ya en
     * pantalla: {@code x} hacia la derecha y {@code y} hacia abajo, el mismo
     * criterio con el que se dibujan las celdas. No se invierte ningún eje
     * aquí dentro; invertirlo rompe la flecha.
     *
     * <p>La perpendicular es (-fwdY, fwdX): girada 90°, no el intercambio de
     * componentes, que solo es perpendicular cuando el ángulo es de 45°.
     *
     * <p>La punta avanza {@code LENGTH} y la base retrocede {@code LENGTH/2},
     * así el centroide del triángulo cae justo en (cx,cy), que es la posición
     * del player.
     */
    public static float[] arrowVertices(float cx, float cy, float cellPx,
                                 float fwdX, float fwdY) {
        float len = (float) Math.hypot(fwdX, fwdY);
        float nx = len < 1e-4f ? 1f : fwdX / len;
        float ny = len < 1e-4f ? 0f : fwdY / len;
        // Perpendicular a la flecha: la base queda siempre de canto, sin importar
        // el ángulo.
        float perpX = -ny;
        float perpY = nx;

        float length = cellPx * ARROW_LENGTH;
        float halfWidth = cellPx * ARROW_HALF_WIDTH;
        float back = length / 2f;

        return new float[] {
                // Punta.
                cx + nx * length, cy + ny * length,
                // Base: centro retranqueado, ensanchado a los dos lados.
                cx - nx * back + perpX * halfWidth,
                cy - ny * back + perpY * halfWidth,
                cx - nx * back - perpX * halfWidth,
                cy - ny * back - perpY * halfWidth
        };
    }

    /** Punta de la flecha respecto al player, en múltiplos de celda. */
    private static final float ARROW_LENGTH = 0.75f;
    /** Media anchura de la base de la flecha, en múltiplos de celda. */
    private static final float ARROW_HALF_WIDTH = 0.42f;

    public void draw(QuadBatch batch, int screenWidth, int screenHeight,
                     Vector3f playerPos, float playerYaw) {
        if (!visible || playerPos == null) {
            return;
        }
        int rows = dungeon.getRows();
        int cols = dungeon.getCols();
        if (rows <= 0 || cols <= 0) {
            return;
        }

        float sizePx = sizePx(screenWidth, screenHeight);
        float cellPx = Math.min(sizePx / cols, sizePx / rows);
        float mapW = cols * cellPx;
        float mapH = rows * cellPx;
        float px = screenWidth - margin - mapW;
        float py = screenHeight - margin - mapH;

        // Panel de fondo con borde celeste.
        batch.addQuad(px - 6f, py - 6f, mapW + 12f, mapH + 12f, 0.02f, 0.05f, 0.10f, 0.62f);
        batch.addQuad(px - 6f, py - 6f, mapW + 12f, 2f, 0.35f, 0.60f, 0.85f, 0.4f);
        batch.addQuad(px - 6f, py + mapH + 4f, mapW + 12f, 2f, 0.35f, 0.60f, 0.85f, 0.4f);
        batch.addQuad(px - 6f, py - 6f, 2f, mapH + 12f, 0.35f, 0.60f, 0.85f, 0.4f);
        batch.addQuad(px + mapW + 4f, py - 6f, 2f, mapH + 12f, 0.35f, 0.60f, 0.85f, 0.4f);

        // Celdas reveladas: cada item explora su alrededor y pinta el suelo que
        // encuentre (piso con techo, sin techo o railing). Los muros y el vacío
        // no se pintan y cortan la búsqueda; si junto a un item solo hay otro
        // item, la búsqueda continúa desde ese segundo item.
        //
        // 'revealed' es el suelo que se ha explorado. Los muros, ventanas y
        // puertas NO se exploran (siguen cortando la búsqueda), pero se pintan
        // igualmente en una segunda pasada si tocan suelo ya revelado: son la
        // silueta de la habitación que se está viendo. Por eso son dos
        // matrices y no una.
        boolean[][] revealed = new boolean[rows][cols];
        boolean[][] drawn = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (revealedAll) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    revealed[r][c] = true;
                }
            }
        }
        // Celdas donde HAY un item ahora mismo, o sea los que el player todavía
        // puede recoger. El grid guarda el símbolo '◈' aunque la llave ya se haya
        // recogido, así que sin esto el minimapa seguía enseñando llaves que no
        // existen. Se compara contra la lista viva de items, no contra el mapa.
        boolean[][] occupied = new boolean[rows][cols];
        for (Vector3f item : dungeon.getItemPositions()) {
            int ir = (int) Math.floor((item.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE);
            int ic = (int) Math.floor((item.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE);
            if (ir >= 0 && ir < rows && ic >= 0 && ic < cols) {
                occupied[ir][ic] = true;
            }
        }

        for (Vector3f item : dungeon.getItemPositions()) {
            int ir = (int) Math.floor((item.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE);
            int ic = (int) Math.floor((item.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE);
            if (ir >= 0 && ir < rows && ic >= 0 && ic < cols && !revealed[ir][ic]) {
                revealed[ir][ic] = true;
                queue.add(new int[] {ir, ic});
            }
        }
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int[] d : DIRS) {
                int nr = cell[0] + d[0];
                int nc = cell[1] + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || revealed[nr][nc]) {
                    continue;
                }
                MapConfig.SymbolType t = dungeon.getCellAt(nr, nc);
                if (t == null || t == MapConfig.SymbolType.EMPTY) {
                    continue;
                }
                if (t == MapConfig.SymbolType.H_WALL || t == MapConfig.SymbolType.V_WALL
                        || t == MapConfig.SymbolType.WINDOW) {
                    continue;
                }
                revealed[nr][nc] = true;
                queue.add(new int[] {nr, nc});
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (revealed[r][c]) {
                    drawn[r][c] = true;
                }
            }
        }
        // Segunda pasada: la estructura que rodea lo ya explorado se pinta sin
        // abrirla, para que el plano enseñe las formas de las habitaciones y no
        // solo manchas de suelo.
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (drawn[r][c] || !isStructure(dungeon.getCellAt(r, c))) {
                    continue;
                }
                for (int[] d : DIRS) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && revealed[nr][nc]) {
                        drawn[r][c] = true;
                        break;
                    }
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!drawn[r][c]) {
                    continue;
                }
                MapConfig.SymbolType type = dungeon.getCellAt(r, c);
                float[] color = colorDeCelda(type, occupied[r][c]);
                batch.addQuad(px + c * cellPx, py + r * cellPx, cellPx, cellPx,
                        color[0], color[1], color[2], color[3]);
            }
        }

        // Items restantes: puntos dorados, con el mismo color que la celda de
        // llave para que se lea como "llave" y no como un punto suelto. Solo
        // quedan los que siguen en el nivel, así que recoger uno lo hace
        // desaparecer del plano.
        for (Vector3f item : dungeon.getItemPositions()) {
            float relR = (item.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE;
            float relC = (item.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE;
            batch.addQuadCentered(
                    px + (relC + 0.5f) * cellPx,
                    py + (relR + 0.5f) * cellPx,
                    cellPx * 0.35f, cellPx * 0.35f,
                    MapConfig.MINIMAP_ITEM_R, MapConfig.MINIMAP_ITEM_G,
                    MapConfig.MINIMAP_ITEM_B, MapConfig.MINIMAP_ITEM_A);
        }

        // Player: punto + flecha de dirección. El marcador se encaja al CENTRO
        // de su celda (avanza bloque a bloque, como un mapa de RPG), pero a la
        // celda de piso más cercana: si el centro queda metido en el muro al ir
        // pegado a él, el triángulo no se pinta sobre el muro ni sobre el aire.
        // La flecha solo mira en 4 direcciones (giro a 90°). El personaje en 3D
        // sigue fino; esto es solo cómo se dibuja el plano.
        float relR = (playerPos.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE;
        float relC = (playerPos.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE;
        int[] floor = nearestFloorCell(relR, relC, rows, cols, dungeon::getCellAt);
        if (floor == null) {
            return;
        }
        float cx = px + (floor[1] + 0.5f) * cellPx;
        float cy = py + (floor[0] + 0.5f) * cellPx;

        // Flecha: el "front" de la cámara (yaw 0 => +X, yaw 90 => +Z) se
        // translate tal cual a pantalla. OJO: aquí NO se invierte la Z. El
        // minimapa crece hacia abajo con Z (cy = py + (relR + 0.5) * cellPx), de
        // modo que +Z ya apunta ABAJO en pantalla. Invertirla hacía que la punta
        // fuera al lado contrario de la base y la flecha señalase hacia atrás.
        float yaw = snapYaw90(playerYaw);
        float rad = (float) Math.toRadians(yaw);
        float fwdX = (float) Math.cos(rad);
        float fwdY = (float) Math.sin(rad);
        float[] arrow = arrowVertices(cx, cy, cellPx, fwdX, fwdY);
        batch.addTriangle(
                arrow[0], arrow[1], arrow[2], arrow[3], arrow[4], arrow[5],
                0.0f, 1f, 1f, 1f);
    }
}