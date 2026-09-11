package Render3D.map;

/**
 * UN solo lugar para TODO lo editable de la mazmorra: las constantes de
 * geometría (tamaño de celda, muros, aberturas, railings), la tabla de
 * símbolos con la que se escribe el nivel (ver {@link SymbolType}) y el nivel
 * de ejemplo ({@link #sampleLayout()}).
 *
 * Para modificar el mapa basta tocar este archivo (o {@code sampleLayout()}):
 * <ul>
 *   <li>Cambiar un símbolo del nivel  -> editar {@link SymbolType}.</li>
 *   <li>Alturas de ventanas/puertas   -> {@link #WINDOW_SILL_HEIGHT},
 *       {@link #WINDOW_HEADER_HEIGHT}, {@link #DOOR_HEADER_HEIGHT}.</li>
 *   <li>Grosor/altura de los railings -> {@link #RAILING_THICKNESS},
 *       {@link #RAILING_HEIGHT}.</li>
 *   <li>Geometría básica              -> {@link #CELL_SIZE},
 *       {@link #WALL_HEIGHT}, {@link #WALL_THICKNESS},
 *       {@link #WALL_JOIN_OVERLAP}, {@link #FLOOR_THICKNESS}.</li>
 * </ul>
 * El que CONSTRUYE la geometría es {@link DungeonManager}; esta clase solo
 * guarda la configuración.
 */
public final class MapConfig {

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

    // Raillings: un muro bajo, un poco por debajo de la cámara.
    public static final float RAILING_HEIGHT = 2.0f;
    // Los raillings son bastante más finos que los muros normales (1.0).
    public static final float RAILING_THICKNESS = 0.4f;

    private MapConfig() {
    }

    /**
     * Nivel de ejemplo: dos cuartos con piso y techo ('◫') unidos por un
     * puente de '◰' (piso SIN techo con dos muros bajos de 2.0 en sus bordes
     * largos). Sobre y bajo el puente hay vacío ('□'), así que queda
     * descubierto arriba. Cada cuarto tiene ventana ('▣'), puerta ('◧'), el
     * spawn ('▶' = mira al este) y items ('◈').
     */
    public static String[] sampleLayout() {
        return new String[] {
            "◙■▣■■□□□■■▣■◙",
            "▣◫◫◫◙□□□◙◫◫◫▣",
            "▣◫▶◫◧◰◰◰◧◫◈◫▣",
            "▣◫◫◫◙□◰□◙◫◫◫▣",
            "◙■▣■■■◧■■■▣■◙",
            "□□□□◙▒▒▒◙□□□□",
            "□□□□◙▒▒▒◙□□□□",
            "□□□□■■■■■□□□□",
        };
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
        FLOOR_ONLY('▒', false, false, null),
        RAILING('◰', true, false, null),
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

        /** Cualquier celda sobre la que se puede pisar (con o sin techo). */
        public boolean isWalkable() {
            return this == FLOOR_CEILING || this == FLOOR_ONLY || this == RAILING;
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