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
 * El tamaño máximo del mapa es configurable ({@link #getSizePx()} /
 * {@link #setSizePx}) desde un menú (botones [-]/[+]).
 *
 * No dibuja por sí mismo: añade primitivas al {@link QuadBatch} compartido que
 * Main se encarga de renderizar una vez por frame.
 */
public class Minimap {

    private final DungeonManager dungeon;
    private boolean visible = false;

    private float sizePx = 260f;
    private static final float MIN_SIZE = 90f;
    private static final float MAX_SIZE = 260f;
    private static final float STEP = 15f;

    private final float margin = 12f;

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

    public void toggle() {
        visible = !visible;
    }

    public float getSizePx() {
        return sizePx;
    }

    public void setSizePx(float sizePx) {
        this.sizePx = Math.max(MIN_SIZE, Math.min(MAX_SIZE, sizePx));
    }

    public void grow() {
        setSizePx(sizePx + STEP);
    }

    public void shrink() {
        setSizePx(sizePx - STEP);
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
        for (Vector3f item : dungeon.getItemPositions()) {
            int ir = (int) Math.floor((item.z - dungeon.getOriginZ()) / MapConfig.CELL_SIZE);
            int ic = (int) Math.floor((item.x - dungeon.getOriginX()) / MapConfig.CELL_SIZE);
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
            float relR = (item.z - dungeon.getOriginZ()) / MapConfig.CELL_SIZE;
            float relC = (item.x - dungeon.getOriginX()) / MapConfig.CELL_SIZE;
            batch.addQuadCentered(
                    px + (relC + 0.5f) * cellPx,
                    py + (relR + 0.5f) * cellPx,
                    cellPx * 0.35f, cellPx * 0.35f,
                    1f, 0.9f, 0.35f, 1f);
        }

        // Jugador: punto + flecha de dirección.
        float relR = (playerPos.z - dungeon.getOriginZ()) / MapConfig.CELL_SIZE;
        float relC = (playerPos.x - dungeon.getOriginX()) / MapConfig.CELL_SIZE;
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