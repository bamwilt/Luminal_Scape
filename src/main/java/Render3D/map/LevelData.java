package Render3D.map;

/**
 * Nivel ya leído y resuelto: no es solo la matriz de glifos, también lleva la
 * metadata con la que se juega (tiempo, luz ambiente, distancia de niebla) y el
 * {@link LevelTypePreset} del que la hereda.
 *
 * <p>La matriz se guarda como {@code char[][]} porque es la forma en la que la
 * consume {@link DungeonManager} y en la que se indexa por
 * {@code [fila][columna]}; {@link #toRows()} la convierte a {@code String[]}
 * para las partes que trabajan por filas.
 */
public final class LevelData {

    private final String path;
    private final String name;
    private final int timeSeconds;
    private final float ambientLight;
    private final float viewDistance;
    private final LevelTypePreset preset;
    private final char[][] map;
    private final String[] rows;

    public LevelData(String path, String name, int timeSeconds,
                     float ambientLight, float viewDistance,
                     LevelTypePreset preset, char[][] map) {
        this.path = path;
        this.name = name;
        this.timeSeconds = timeSeconds;
        this.ambientLight = ambientLight;
        this.viewDistance = viewDistance;
        this.preset = preset;
        this.map = map;
        this.rows = new String[map.length];
        for (int r = 0; r < map.length; r++) {
            this.rows[r] = new String(map[r]);
        }
    }

    /** Ruta del recurso del que se leyó el nivel. */
    public String getPath() {
        return path;
    }

    /** Título mostrado (directiva {@code name:}). */
    public String getName() {
        return name;
    }

    /** Tiempo de partida en segundos (directiva {@code time: mm:ss}). */
    public int getTimeSeconds() {
        return timeSeconds;
    }

    /** Luz ambiente ya traducida a número (0..1). */
    public float getAmbientLight() {
        return ambientLight;
    }

    /** Distancia de niebla ya traducida a número (unidades de mundo). */
    public float getViewDistance() {
        return viewDistance;
    }

    /** Preset heredado de {@code level_type:}. */
    public LevelTypePreset getPreset() {
        return preset;
    }

    /** La matriz del mapa, indexada como {@code [fila][columna]}. */
    public char[][] getMap() {
        return map;
    }

    /** Las filas del mapa como cadenas (para {@link DungeonManager}). */
    public String[] toRows() {
        return rows.clone();
    }

    @Override
    public String toString() {
        return "LevelData{" + name + ", " + timeSeconds + "s, luz=" + ambientLight
                + ", niebla=" + viewDistance + ", " + preset + "}";
    }
}
