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
 *   <li>Geometría básica              -> {@link #TILE_SIZE},
 *       {@link #WALL_HEIGHT}, {@link #WALL_THICKNESS},
 *       {@link #FLOOR_THICKNESS}.</li>
 *   <li>Densidad de textura           -> {@link #UV_SCALE}.</li>
 *   <li>Vista y niebla                -> {@link #VIEW_DISTANCE_LOW},
 *       {@link #VIEW_DISTANCE_NORMAL}, {@link #VIEW_DISTANCE_HIGH},
 *       {@link #FOG_NEAR_RATIO}, {@link #CAMERA_HEIGHT}.</li>
 *   <li>Decoración                    -> {@link #PROP_BASE_Y},
 *       {@link #PROP_SCALE}, {@link #PROP_DENSITY}.</li>
 *   <li>Luces (techo con luz e items) -> {@link #LIGHT_CEILING_EMISSION},
 *       {@link #LIGHT_CEILING_INTENSITY}, {@link #ITEM_LIGHT_INTENSITY},
 *       {@link #LIGHT_ATTEN_LINEAR}, {@link #LIGHT_ATTEN_QUADRATIC},
 *       {@link #MAX_LIGHT_COUNT}.</li>
 * </ul>
 * El que CONSTRUYE la geometría es {@link DungeonManager} (y
 * {@link WallAutotiler} para los muros); esta clase solo guarda la
 * configuración.
 */
public final class MapConfig {

    // Geometría editable de la mazmorra.
    //
    // TILE_SIZE es el lado de una casilla en unidades del mundo. Todo lo demás
    // (muros, aberturas, texturas) se deriva de ella, así que cambiar esta
    // escala el mapa completo sin tocar ningún otro número.
    public static final float TILE_SIZE = 2.0f;

    /** Grosor del muro, en la dirección en la que se orienta. */
    public static final float WALL_THICKNESS = 0.2f;

    /** Altura de los muros, del suelo al techo. */
    public static final float WALL_HEIGHT = 4.0f;

    /** Los muros se apoyan en y=0, así que su centro queda a media altura. */
    public static final float WALL_CENTER_Y = WALL_HEIGHT / 2f;

    /**
     * Escala de la UV en world-space tiling: repeticiones de textura por unidad
     * de mundo. Con 1.0 la textura se repite una vez por unidad (una baldosa
     * de 4x4 muestra 4x4 repeticiones, igual que antes del refactor); con 0.25
     * una baldosa de 4x4 muestra la textura una sola vez.
     *
     * <p>NO es un factor sobre un rango (0,0)-(1,1): el rango de cada cara se
     * calcula con el tamaño real de esa cara (ver {@link
     * Render3D.mesh.MeshBuilder}), que es lo que evita que la textura se estire
     * en las caras pequeñas o largas.
     */
    public static final float UV_SCALE = 1.0f;

    public static final float FLOOR_THICKNESS = 0.4f;
    // El tamaño del item ya no es una constante de aqui: cada tipo de item
    // escala su propio modelo (ver ItemKey), y el cubo de 0,8 que usaba esto
    // desaparecio cuando la llave paso a ser un GLB.
    public static final float ITEM_FLOAT_HEIGHT = 1.6f; // altura de la cámara
    public static final float ITEM_SPIN_SPEED = 1.5f;   // rad/seg
    public static final float ITEM_PICKUP_RADIUS = 1.5f;

    // Aberturas: UN solo lugar para ajustar ventanas y puertas. Todas por
    // debajo de WALL_HEIGHT.
    public static final float WINDOW_SILL_HEIGHT = 1.0f;   // pared inferior de la ventana
    public static final float WINDOW_HEADER_HEIGHT = 3.0f; // pared superior de la ventana
    public static final float DOOR_HEADER_HEIGHT = 3.0f;   // hueco de la puerta: un poco sobre la cámara

    // Raillings: un muro bajo, un poco por debajo de la cámara.
    public static final float RAILING_HEIGHT = 1.1f;
    // Los raillings son bastante más finos que los muros normales (1.0).
    public static final float RAILING_THICKNESS = 0.1f;

    // ==================================================================
    // PANEL DE AJUSTES: vista, niebla y decoración.
    //
    // Todo lo que define la "sensación" del nivel está aquí, en un solo
    // sitio. Las distancias y alturas van en unidades de mundo (metros).
    // ==================================================================

    // ---- Vista y niebla --------------------------------------------------
    // Mas calidad, mas lejos: 5 / 10 / 20. La distancia es la de niebla total,
    // asi que subirla deja ver mas pasillo antes del fundido.
    public static final float VIEW_DISTANCE_LOW = 5.0f;
    public static final float VIEW_DISTANCE_NORMAL = 10.0f;
    public static final float VIEW_DISTANCE_HIGH = 20.0f;

    /**
     * Fracción de la distancia de vista en la que ARRANCA la niebla.
     *
     * <p>Con 0.30, los primeros metros quedan limpios y el fondo cierra de
     * golpe. Si se sube, hay más campo cercano nítido; si se baja a 0, la
     * niebla empieza pegada a la cámara.
     */
    public static final float FOG_NEAR_RATIO = 0.30f;

    /**
     * Giro de cámara con Q/E, en grados por segundo.
     *
     * <p>La cámara solo tiene yaw (no hay pitch), así que Q y E suman y restan
     * grados a ese mismo ángulo y el giro es en el plano del suelo, igual que
     * el del ratón. Con 120 un segundo entero da un cuarto de vuelta: se nota
     * que gira sin llegar a marear, y mantiene la misma sensación que mover el
     * ratón a velocidad media.
     */
    public static final float KEY_YAW_SPEED = 120.0f;

    /**
     * Altura de la cámara (los ojos del jugador).
     *
     * <p>El suelo visible es una losa de {@link #FLOOR_THICKNESS}, así que la
     * altura real del ojo sobre el piso es esta menos {@code FLOOR_THICKNESS}.
     * Conviene subirla si se quiere ver la estancia "desde más arriba".
     */
    public static final float CAMERA_HEIGHT = 1.6f;

    // ---- Decoración ------------------------------------------------------
    /**
     * Altura a la que se apoyan las decoraciones.
     *
     * <p>Por defecto {@link #FLOOR_THICKNESS}: el suelo es una losa que va de 0
     * a esa altura, así que su cara superior (la que se ve y se pisa) está en
     * {@code FLOOR_THICKNESS}. Con 0 las piezas quedaban medio enterradas y se
     * veían demasiado bajas. Súbelo para flotarlas un poco más.
     */
    public static final float PROP_BASE_Y = FLOOR_THICKNESS;

    /**
     * Multiplicador global del tamaño de las decoraciones.
     *
     * <p>Es un factor sobre la escala de cada pieza del catálogo (ver
     * {@code PropLayout}): 1.0 las deja como están, 1.5 las agranda y 0.75 las
     * achica. Es global; el ajuste fino por pieza vive en el catálogo.
     */
    public static final float PROP_SCALE = 1.0f;

    /**
     * Probabilidad (0..1) de que una celda con sitio reciba decoración.
     *
     * <p>Era 0.22 y con muchas piezas el nivel parecía amueblado de punta a
     * punta. Con 0.10 la decoración puntúa el recorrido en vez de llenarlo.
     */
    public static final float PROP_DENSITY = 0.10f;

    // ---- Luces locales ----------------------------------------------------
    //
    // El nivel tiene UNA luz global (GameConfig.LIGHT_*), la misma para todo el
    // mapa. Estas son las luces PUNTUALES que se encienden cerca del jugador: la
    // del techo con luz '◉' y la de cada item. Se acumulan al difuso global, con
    // caida por distancia, y solo se mandan las MAX_LIGHT_COUNT mas cercanas.
    /**
     * Cuántas luces puntuales se mandan al shader cada frame.
     *
     * <p>Es el tamaño del array de uniforms {@code u_lightPos}/{@code u_lightColor}
     * de los shaders de muro y de decoracion. Subirlo ilumina mas cosas a la vez
     * a costa de mas operaciones por fragmento; los niveles tienen menos luces
     * que este numero, asi que de momento solo truncan las de un mapa enorme.
     */
    public static final int MAX_LIGHT_COUNT = 8;

    /**
     * Caida de la luz por distancia: {@code 1 / (1 + LINEAR*d + QUADRATIC*d^2)}.
     *
     * <p>Con estos valores una luz se nota hasta unos cuatro o cinco metros y
     * al otro lado de una sala grande ya no llega. Subirlos para que la luz
     * alcance mas lejos; bajarlos para que se apague antes.
     */
    public static final float LIGHT_ATTEN_LINEAR = 0.7f;
    public static final float LIGHT_ATTEN_QUADRATIC = 0.25f;

    /**
     * A partir de esta distancia al jugador la luz se descarta y no entra en
     * el array. Solo filtra candidatos: aunque se pase, el shader la apaga por
     * la caida, asi que es solo una economia.
     */
    public static final float LIGHT_CULL_DISTANCE = 26.0f;

    // ---- Techo con luz ('◉') --------------------------------------------
    /**
     * Textura del panel luminoso. Es una baldosa del pack, la misma familia que
     * los techos normales, para que la placa encaje con el techo que la rodea y
     * no parezca un rectangulo pegado encima.
     */
    public static final String LIGHT_CEILING_TEXTURE = "textures/br_ceiling_tiles.png";

    /**
     * Cuanto brilla el panel por si mismo, por encima de la luz que recibe.
     *
     * <p>Es emisión pura: no depende de la luz ambiente ni de si hay un item
     * cerca, asi que un '◉' se ve igual de encendido en un cuarto a oscuras que
     * en un pasillo iluminado. A 0 el techo con luz deja de distinguirse.
     */
    public static final float LIGHT_CEILING_EMISSION = 0.55f;

    /**
     * Separación del foco bajo el techo.
     *
     * <p>El panel se dibuja en el techo (y=4), pero la luz se coloca un poco
     * por debajo: si se quedara en el plano del techo, el suelo recibiria la luz
     * casi de canto y la estancia no se aclararia.
     */
    public static final float LIGHT_CEILING_DROP = 0.35f;

    /**
     * Intensidad de la luz que cada '◉' deja caer en la habitacion. El color es
     * el de un fluorescente calido, no dorado como el del item, para que se
     * distingan de un vistazo a dos focos de la misma clase.
     */
    public static final float LIGHT_CEILING_INTENSITY = 0.85f;
    public static final float LIGHT_CEILING_R = 1.00f;
    public static final float LIGHT_CEILING_G = 0.96f;
    public static final float LIGHT_CEILING_B = 0.82f;

    // ---- Luz del item ('◈') ---------------------------------------------
    /**
     * La llave no se enciende a si misma: deja una aureola dorada tenue a su
     * alrededor, que ilumina el suelo y las paredes cercanas.
     *
     * <p>El item es de metal viejo y se ve mejor recortado contra un fondo
     * apenas iluminado; con emisión propia brillaba igual de lejos que el muro
     * que lo rodea y se perdia la sensacion de que la luz viene de el.
     */
    public static final float ITEM_LIGHT_INTENSITY = 0.60f;
    public static final float ITEM_LIGHT_R = 1.00f;
    public static final float ITEM_LIGHT_G = 0.80f;
    public static final float ITEM_LIGHT_B = 0.36f;

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
        LIT_CEILING('◉', false, false, null),
        ITEM('◈', false, false, null),
        /**
         * El mapa. No es un artefacto: no cuenta para el total ni da tiempo,
         * lo que hace es encender el minimapa y enseñar la planta entera.
         * Como el {@link #ITEM}, se pisa, no bloquea y lleva su propio panel de
         * suelo, y panel de techo si esta dentro de una zona techada.
         */
        MAP_ITEM('☑', false, false, null);

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
        /** Muro de verdade ('■' o '◙'): lo único que NO emite suelo propio. */
        public boolean isWall() {
            return this == H_WALL || this == V_WALL;
        }

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
            return this == FLOOR_CEILING || this == FLOOR_ONLY || this == RAILING
                    || this == LIT_CEILING;
        }

        /**
         * Si la celda lleva su propio panel de suelo y techo.
         *
         * <p>Es todo menos el vacio '□' y los muros. Se incluye a proposito lo que
         * no es {@link #isWalkable()}: ventanas, puertas, spawns e items se pisan
         * y necesitan panel propio, o solo recibirian el solape de una celda
         * vecina y quedarian con un cuarto de celda sin suelo en las esquinas de
         * los alféizares.
         *
         * <p>Los muros NO llevan panel: su geometria vertical ya tapa el hueco, y
         * darselo extendia el suelo por fuera de las habitaciones.
         */
        public boolean needsFloor() {
            return this != EMPTY && !isWall();
        }

        /**
         * Si la celda lleva su propio panel de techo.
         *
         * <p>Es el {@link #FLOOR_CEILING} de siempre, mas spawns e items, que se
         * pisan igual y dejaban un agujero de una celda en mitad del pasillo.
         *
         * <p><b>{@link #FLOOR_ONLY} y {@link #RAILING} NO llevan techo</b>, a
         * proposito: el rail se coloca justo en el borde de un hueco abierto,
         * con el vacio al otro lado, y un panel de techo encima taparia
         * precisamente la vista que el rail existe para dar.
         *
         * <p>Las aberturas quedan fuera porque su dintel de muro ya llega
         * arriba; darle panel ademas lo solaparia con el dintel.
         *
         * <p>{@link #LIT_CEILING} cuenta como techada: lleva panel propio
         * SIEMPRE (es la placa que se enciende) y ademas cuenta como techo
         * para sus vecinas, porque una placa encendida significa que ahi hay
         * techo y el hueco alrededor de la habitacion se cerraria igual que
         * con un {@link #FLOOR_CEILING}.
         */
        public boolean needsCeiling() {
            return this == FLOOR_CEILING || this == LIT_CEILING;
        }

        /**
         * Techo con luz: celda transitable cuyo panel de techo va a su propia
         * malla, se pinta emisivo y ademas enciende una luz puntual en la
         * habitacion (ver {@link #LIGHT_CEILING_EMISSION}).
         *
         * <p>El suelo es el de siempre, con su textura: lo unico que cambia
         * respecto a un {@link #FLOOR_CEILING} es la cara de arriba.
         */
        public boolean isLitCeiling() {
            return this == LIT_CEILING;
        }

        /**
         * ¿La celda es suelo techado, o es un spawn o item metido dentro de una
         * zona techada?
         *
         * <p>Los spawns y los items pisan el suelo como una celda cualquiera, y
         * por eso necesitan panel: si no, dejaban un agujero de una celda en
         * mitad del pasillo. Pero el panel solo tiene sentido si hay algo
         * alrededor. Un item suelto en el vacio, como el de la esquina del
         * nivel 3 junto a la barandilla, se quedaba con un panel de techo
         * flotando solo sobre el, que se ve desde cualquier lado como un
         * rectangulo colgado en el aire.
         *
         * <p>Por eso la pregunta ya no es "que simbolo es" sino "esta dentro de
         * una zona techada": basta con que tenga un vecino en cruz que sea
         * {@link #FLOOR_CEILING}. El panel se dibuja entero, sin recortar ni
         * partir, para que no se note el empalme con la zona techada.
         *
         * <p><b>{@link #FLOOR_ONLY} y {@link #RAILING} NO llevan techo</b>, a
         * proposito: el rail se coloca justo en el borde de un hueco abierto,
         * con el vacio al otro lado, y un panel de techo encima taparia
         * precisamente la vista que el rail existe para dar.
         *
         * <p>Las aberturas quedan fuera porque su dintel de muro ya llega
         * arriba; darle panel ademas lo solaparia con el dintel.
         */
        public boolean needsCeilingIn(char[][] layout, int r, int c) {
            if (needsCeiling()) {
                return true;
            }
            if (!needsFloor() || isOpening()) {
                return false;
            }
            int rows = layout.length;
            int cols = layout[0].length;
            int[][] lados = { { r - 1, c }, { r + 1, c }, { r, c - 1 }, { r, c + 1 } };
            for (int[] lado : lados) {
                int lr = lado[0];
                int lc = lado[1];
                if (lr < 0 || lr >= rows || lc < 0 || lc >= cols) {
                    continue;
                }
                if (SymbolType.fromChar(layout[lr][lc]).needsCeiling()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Celda cerrada que no lleva panel propio, de modo que el panel de la
         * celda de suelo vecina tiene que llegar hasta su cara.
         *
         * <p>Son los muros y las aberturas. Antes solo contaban los muros, y una
         * sala cerrada con ventanas o puertas se quedaba con un margen sin
         * suelo ni techo de {@code TILE_SIZE/2 - WALL_THICKNESS/2} alrededor.
         */
        public boolean needsPanelReach() {
            return isWall() || isOpening();
        }

        /**
         * Celda que suelta algo al pasar por encima: el artefacto ({@link #ITEM})
         * o el mapa ({@link #MAP_ITEM}).
         *
         * <p>Los dos se comportan igual en cuanto al suelo, al techo y a la
         * colision, asi que el resto del codigo pregunta por esto en vez de
         * repetir la comparacion de los dos simbolos.
         */
        public boolean isItem() {
            return this == ITEM || this == MAP_ITEM;
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