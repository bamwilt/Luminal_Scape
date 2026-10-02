package Render2D;

import Render3D.map.DungeonManager;
import Render3D.map.MapConfig;
import org.joml.Vector3f;

import java.util.ArrayDeque;

/**
 * Minimapa en pantalla (abajo a la derecha): dibuja la grilla del nivel con
 * quads de color — celdas sólidas en azul celeste, pisos en gris celeste,
 * items restantes como puntos amarillos y el jugador como un triángulo cyan
 * que apunta a su dirección (usa el "front" de la cámara).
 *
 * El tamaño no se elige a mano: sale de la pantalla (una fracción del lado
 * menor) para que ocupe siempre el mismo lugar visible, y por eso el botón de
 * tamaño que había en el menú de pausa ya no hace falta.
 *
 * No dibuja por sí mismo: añade primitivas al {@link QuadBatch} compartido que
 * Main se encarga de renderizar una vez por frame.
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
     * entero en el 3 y el jugador se lo ahorraba. El item de mapa vale una vez
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
     * {@link #MIN_SIZE} y {@link #MAX_SIZE}: en una ventana baja el mapa
     * encoge con ella en vez de comerse medio escenario, y en una muy ancha no
     * se convierte en un mural.
     */
    private float sizePx(int screenWidth, int screenHeight) {
        float lado = Math.min(screenWidth, screenHeight) * SCREEN_RATIO;
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, lado));
    }

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
        boolean[][] revealed = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (revealedAll) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    revealed[r][c] = true;
                }
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
                if (!revealed[r][c]) {
                    continue;
                }
                MapConfig.SymbolType type = dungeon.getCellAt(r, c);
                float cr, cg, cb, ca;
                switch (type) {
                    case FLOOR_ONLY:
                        // Piso SIN techo: tono más claro y frío.
                        cr = 0.56f; cg = 0.63f; cb = 0.70f; ca = 0.65f;
                        break;
                    case RAILING:
                        cr = 0.28f; cg = 0.40f; cb = 0.55f; ca = 0.75f;
                        break;
                    case DOOR:
                        cr = 0.92f; cg = 0.65f; cb = 0.20f; ca = 0.9f;
                        break;
                    default:
                        // FLOOR_CEILING, spawns e items: gris celeste (suelo con
                        // techo).
                        cr = 0.44f; cg = 0.51f; cb = 0.56f; ca = 0.6f;
                        break;
                }
                batch.addQuad(px + c * cellPx, py + r * cellPx, cellPx, cellPx, cr, cg, cb, ca);
            }
        }

        // Items restantes: puntos amarillos brillantes.
        for (Vector3f item : dungeon.getItemPositions()) {
            float relR = (item.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE;
            float relC = (item.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE;
            batch.addQuadCentered(
                    px + (relC + 0.5f) * cellPx,
                    py + (relR + 0.5f) * cellPx,
                    cellPx * 0.35f, cellPx * 0.35f,
                    1f, 0.9f, 0.35f, 1f);
        }

        // Jugador: punto + flecha de dirección.
        float relR = (playerPos.z - dungeon.getOriginZ()) / MapConfig.TILE_SIZE;
        float relC = (playerPos.x - dungeon.getOriginX()) / MapConfig.TILE_SIZE;
        if (relR < -0.5f || relR > rows + 0.5f || relC < -0.5f || relC > cols + 0.5f) {
            return;
        }
        float cx = px + (relC + 0.5f) * cellPx;
        float cy = py + (relR + 0.5f) * cellPx;

        // "front" de la cámara (yaw 0 => +X). En pantalla (y abajo) la
        // dirección vertical es -Z, por eso se invierte la componente Z.
        float rad = (float) Math.toRadians(playerYaw);
        float dx = (float) Math.cos(rad);
        float dz = (float) Math.sin(rad);

        // Triángulo cyan centrado en el jugador: el centroide (frente 2x de
        // fondo) queda exactamente sobre la posición del jugador.
        float nlx = (float) Math.hypot(dx, dz);
        float ndx = dx / nlx;
        float ndz = dz / nlx;
        float bx = ndz;
        float bz = ndx;
        float half = cellPx * 0.75f;
        float back = cellPx * 0.375f;
        float headW = cellPx * 0.42f;
        batch.addTriangle(
                cx + ndx * half, cy - ndz * half,
                cx - ndx * back + bx * headW, cy + ndz * back + bz * headW,
                cx - ndx * back - bx * headW, cy + ndz * back - bz * headW,
                0.0f, 1f, 1f, 1f);
    }
}