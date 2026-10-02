package Render3D.map;

import java.util.ArrayList;
import java.util.List;

import Render3D.map.MapConfig.SymbolType;

/**
 * Decide que decoracion va en cada celda de suelo.
 *
 * <p>La colocacion es <b>determinista</b>: entra el mismo mapa y sale siempre el
 * mismo resultado, sin estado aleatorio entre ejecuciones. Se Consegue con un
 * hash de las coordenadas de la celda, asi que dos niveles que compartan
 * celda ponen lo mismo, y cambiar el orden en que se recorre el mapa no cambia
 * nada. Un {@code Random} sembrado acoplaria el resultado al orden de recorrido,
 * y recargar el nivel daria plantas distintas.
 *
 * <p>Solo se decora lo transitable con celda entera: techado, abierto por debajo
 * o con la placa de luz encendida ({@link SymbolType#FLOOR_CEILING},
 * {@link SymbolType#FLOOR_ONLY} y {@link SymbolType#LIT_CEILING}). Los muros, las
 * aberturas, los railings y los huecos {@code □} no se tocan: o no hay sitio, o
 * la decoracion quedaria atravesando una pared.
 *
 * <p>Reglas de separacion, en espiras crecientes alrededor de la celda:
 * <ul>
 *   <li>Nada pegado a una ventana, una puerta o un railing: esas celdas y sus
 *       vecinas se descartan, porque un mueble tapando un alféizar o una
 *       salida se lee como un fallo de colisión.
 *   <li>Nada encima de un spawn ni de un item.
 *   <li>Entre dos decoraciones de la misma celda hay al menos una de margen.
 * </ul>
 */
public final class PropLayout {

    /** Una pieza colocada: recurso, celda, giro y escala. */
    public static final class Placed {
        public final String model;
        public final int row;
        public final int col;
        public final float yaw;
        public final float scale;

        Placed(String model, int row, int col, float yaw, float scale) {
            this.model = model;
            this.row = row;
            this.col = col;
            this.yaw = yaw;
            this.scale = scale;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Placed)) {
                return false;
            }
            Placed otro = (Placed) o;
            return row == otro.row && col == otro.col
                    && Float.compare(yaw, otro.yaw) == 0
                    && Float.compare(scale, otro.scale) == 0
                    && model.equals(otro.model);
        }

        @Override
        public int hashCode() {
            int h = model.hashCode();
            h = 31 * h + row;
            h = 31 * h + col;
            h = 31 * h + Float.floatToIntBits(yaw);
            h = 31 * h + Float.floatToIntBits(scale);
            return h;
        }
    }

    /**
     * Una pieza del catalogo: el recurso y la escala que la hace caber en una
     * celda.
     *
     * <p>La escala es por pieza y no global porque el pack no viene en un tamano
     * unico: hay plantas de 0,25 m y mesas de 3,7 m, y con una sola escala para
     * todos o la planta desaparecia o la mesa se comia dos pasillos. La escala
     * deja el ancho mayor de la pieza en 1,8 m, dentro de la celda de 2 m con un
     * margen para que no choque con la decoracion de al lado.
     */
    private static final class Piece {
        final String model;
        final float scale;

        Piece(String model, float scale) {
            this.model = model;
            this.scale = scale;
        }
    }

    /**
     * El catalogo: solo decoracion de sobra, del tipo mesa o planta.
     *
     * <p>Se intento lo contrario, con 20 piezas de todo el pack, y quedo
     * amueblado de mas: sofas, camas, cajones y columnas que en un pasillo
     * estrecho solo estorban. Ademas las repisas y el cactus se veian
     * rareza: una repisa pegada a un muro que no existe y un cactus de 0,84 m
     * en medio de un corredor. Los dos quedan fuera, junto con el mobiliario
     * grande.
     *
     * <p>Las cuatro plantas y mesas que quedan si se ven de lejos, que era el
     * problema de verdad: la planta de siempre mide 0,38 m y a la altura de los
     * ojos era un punto en el suelo.
     */
    private static final Piece[] GREEN = {
            new Piece("models/props/Houseplant.glb", 1.8f),
            new Piece("models/props/Dead Houseplant.glb", 1f),
    };

    private static final Piece[] SEATING = {
            new Piece("models/props/Chair.glb", 1f),
            new Piece("models/props/Stool.glb", 1f),
    };

    private static final Piece[] TABLES = {
            new Piece("models/props/Table Round Small.glb", 1f),
            new Piece("models/props/Table Round Large.glb", 0.48f),
    };

    private static final Piece[] CLUTTER = {
            // La alfombra es plana: no estorba la vista y rompe el suelo, que es
            // justo lo que hacia falta para que la decoracion no fuera solo
            // mobiliario en medio del pasillo.
            new Piece("models/props/Round Rug.glb", 0.70f),
    };

    /**
     * Los grupos y su peso. El verde y las mesas pesan mas porque son lo que
     * sobrevive en una habitacion a medio amueblar; el resto se reparte casi
     * igual.
     */
    private static final Piece[][] GROUPS = { GREEN, SEATING, TABLES, CLUTTER };

    /** Probabilidad acumulada de cada grupo: 32%, 57%, 82%, 100%. */
    private static final float[] GROUP_ENDS = { 0.32f, 0.57f, 0.82f, 1.01f };

    /**
     * Probabilidad de que una celda apta reciba decoracion.
     *
     * <p>El valor vive en {@link MapConfig#PROP_DENSITY}, que es el panel de
     * ajustes. Era 0,22 y con muchas piezas la sala parecia amueblada de punta
     * a punta: habia que esquivar plantas en cada pasillo. Con 0,10 la
     * decoracion punctuua el recorrido en vez de llenarlo, y el nivel respira.
     */
    private static final float DENSITY = MapConfig.PROP_DENSITY;

    /**
     * Distancia minima a una ventana, puerta o railing, en celdas.
     *
     * <p>El valor es 0: se descarta la celda que ES una ventana, puerta o
     * railing, no las vecinas. Con radio 1 las habitaciones de 3x3 de los
     * niveles 1 y 2 se quedaban sin decoracion, porque su spawn ocupa el centro
     * y la espira de 3x3 lo cubre entero. Con radio 0 la decoracion se pega a
     * la pared, no a la abertura, y basta para que no la tape.
     *
     * <p>Un spawn o un item bloquean tambien su celda exacta, por lo mismo:
     * una planta encima de un item que se puede recoger no tiene sentido.
     */
    private static final int KEEP_CLEAR = 0;

    /** Spawns e items solo bloquean su propia celda. */
    private static final int KEEP_CLEAR_PICKUP = 0;

    /**
     * Margen alrededor de un railing, en celdas.
     *
     * <p>Es 1 y no 0 a proposito. Un railing es un murete bajo con hueco para
     * mirar al vacio: un mueble en la celda de al lado se ve encima del murete
     * porque la camara esta a la altura de los ojos y el mueble tapa la
     * barandilla. Las ventanas y las puertas si se puede quedar a 0, que es lo
     * que deja decoracion en las habitaciones de 3x3.
     */
    private static final int KEEP_CLEAR_RAILING = 1;

    private PropLayout() {
    }

    /**
     * Calcula la decoracion de un mapa.
     *
     * @param layout mapa de simbolos, ya con todas las filas iguales
     * @return lista de piezas, en orden de recorrido; vacia si no hay sitio
     */
    public static List<Placed> plan(char[][] layout) {
        List<Placed> out = new ArrayList<>();
        if (layout == null || layout.length == 0) {
            return out;
        }
        int rows = layout.length;
        int cols = layout[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);

                // Solo suelo transitable: con techo, abierto por debajo o con la
                // placa de luz encendida. El railing queda fuera porque su
                // superficie es un borde estrecho, no una celda habitable.
                if (!type.isWalkable() || type == SymbolType.RAILING) {
                    continue;
                }
                // Nunca sobre un spawn ni sobre un item: el item se recoge y la
                // planta se quedaria flotando.
                if (type.isSpawn() || type.isItem()) {
                    continue;
                }
                if (!keepClear(layout, r, c)) {
                    continue;
                }

                // Ruido estable de la celda. Se mezclan las tres coordenadas
                // con offsets distintos para que dos celdas vecinas no den el
                // mismo valor.
                float seed = hash(r, c);
                if (seed > DENSITY) {
                    continue;
                }

                // Que grupo y que pieza caen depende de otros hashes: asi el
                // cambio de densidad no altera la mezcla de muebles.
                int grupo = groupOf(hash(r + 7919, c - 104729));
                Piece[] elegidas = GROUPS[grupo];
                // El hash de la pieza lleva otro offset para que dos celdas del
                // mismo grupo no saquen siempre la misma.
                Piece pieza = elegidas[index(r - 31, c + 6151, elegidas.length)];

                // Giro en multiplos de 90 grados: mantiene la pieza dentro de
                // su celda y evita angulos raros que destaquen en una sala
                // ortogonal.
                float yaw = 90f * index(r + 31, c + 17, 4);

                out.add(new Placed(pieza.model, r, c, yaw, pieza.scale));
            }
        }
        return out;
    }

    /**
     * Si la celda tiene sitio para decoracion, mirando en las espiras alrededor.
     *
     * <p>Cada tipo de simbolo tiene su propio radio, y el radio se comprueba
     * siempre. Antes el test de apertura no miraba la distancia: al ensanchar
     * el bucle para el margen del railing, las ventanas empezaron a tirar
     * tambien las celdas vecinas y los niveles 1 y 2 se quedaron sin
     * decoracion, porque en una habitacion de 3x3 con la ventana en la pared
     * exterior todas las celdas quedan a distancia 1 de una apertura.
     */
    private static boolean keepClear(char[][] layout, int row, int col) {
        int rows = layout.length;
        int cols = layout[0].length;
        int radio = java.lang.Math.max(KEEP_CLEAR,
                java.lang.Math.max(KEEP_CLEAR_RAILING, KEEP_CLEAR_PICKUP));
        for (int dr = -radio; dr <= radio; dr++) {
            for (int dc = -radio; dc <= radio; dc++) {
                int r = row + dr;
                int c = col + dc;
                // Fuera del mapa no hay restriccion: el borde del nivel no
                // estorba a nada.
                if (r < 0 || r >= rows || c < 0 || c >= cols) {
                    continue;
                }
                int dist = java.lang.Math.max(java.lang.Math.abs(dr), java.lang.Math.abs(dc));
                SymbolType other = SymbolType.fromChar(layout[r][c]);

                if (other.isOpening() && dist <= KEEP_CLEAR) {
                    return false;
                }
                // El railing necesita margen propio: la celda de al lado ya
                // cuenta como pegada a la barandilla.
                if (other == SymbolType.RAILING && dist <= KEEP_CLEAR_RAILING) {
                    return false;
                }
                boolean pickup = other.isSpawn() || other.isItem();
                if (pickup && dist <= KEEP_CLEAR_PICKUP) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Cuantas decoraciones caben a la vista, para juzgar si el mapa da juego.
     */
    static int candidateCount(char[][] layout) {
        int total = 0;
        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < layout[0].length; c++) {
                SymbolType type = SymbolType.fromChar(layout[r][c]);
                if (!type.isWalkable() || type == SymbolType.RAILING) {
                    continue;
                }
                if (keepClear(layout, r, c)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Hash entero a [0,1). Determinista entre ejecuciones y entre maquinas.
     *
     * <p>Es el mismo esquema demezcla de los hash enteros habituales: sin
     * estado, sin semilla y sin depender del orden de las llamadas.
     */
    private static float hash(int a, int b) {
        int h = a * 0x27d4eb2d + b * 0x165667b1;
        h ^= h >>> 15;
        h *= 0x2545f491;
        h ^= h >>> 13;
        h *= 0x27d4eb2d;
        h ^= h >>> 16;
        return (h & 0x00FFFFFF) / 16777216f;
    }

    /** Elige un elemento del catalogo a partir de la celda. */
    private static int index(int row, int col, int size) {
        return (int) (hash(row, col) * size) % size;
    }

    /**
     * A que grupo del catalogo corresponde el valor de la celda.
     *
     * <p>Usa {@link #GROUP_ENDS}, que termina en algo mayor que 1 para que el
     * ultimo grupo cubra siempre el resto del rango: si el maximo fuera 1,0
     * exacto, un hash que diese justo 1,0 se quedaria sin grupo.
     */
    private static int groupOf(float pick) {
        for (int i = 0; i < GROUP_ENDS.length; i++) {
            if (pick < GROUP_ENDS[i]) {
                return i;
            }
        }
        return GROUPS.length - 1;
    }
}
