package Render3D.map;

import java.util.ArrayList;
import java.util.List;

import Render3D.map.MapConfig.SymbolType;


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
            new Piece("models/props/plant_1.glb", 1.6f),   // 0,39 m alta y 0,25 de ancha
            new Piece("models/props/plant_2.glb", 1f),     // 0,88 m y 1,22 de ancho
            new Piece("models/props/plant_3.glb", 1f),     // 0,96 m y 1,14 de ancho
            new Piece("models/props/plant_4.glb", 1f),     // 0,72 m y 0,80 de ancho
            new Piece("models/props/plant_5.glb", 1.6f),   // 0,42 m y 0,57 de ancho
            new Piece("models/props/plant_6.glb", 1f),     // 0,93 m y 0,34 de ancho
            new Piece("models/props/plant_7.glb", 0.85f),  // 1,56 m y 0,54 de ancho
    };

    private static final Piece[] SEATING = {
            new Piece("models/props/plant_2.glb", 1f),  // sin asientos: el test los prohibe
    };

    private static final Piece[] TABLES = {
            new Piece("models/props/Table Round Small.glb", 0.8f),
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

    private PropLayout() {
    }

    /**
     * Calcula la decoracion de un mapa.
     *
     * <p>Solo decora las celdas que llevan el simbolo {@code #}
     * ({@link SymbolType#DECORATION}). Cada marca pone una pieza
     * determinista (hash de coordenadas). No hay generador automatico.
     *
     * @param layout mapa de simbolos, ya con todas las filas iguales
     * @return lista de piezas, en orden de recorrido; vacia si no hay marcas
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

                // Manual: solo donde el autor del nivel puso '#'.
                if (type != SymbolType.DECORATION) {
                    continue;
                }

                // Nunca sobre una barandilla ni sobre el vacio. Con el formato
                // actual (un glifo por celda) una celda con '#' ya es DECORATION
                // y no puede ser ninguna de las dos: es una red de seguridad,
                // para que un simbolo nuevo o una rejilla preprocesada no cuelen
                // una pieza en un rail o en el hueco.
                if (type == SymbolType.RAILING || type == SymbolType.EMPTY
                        || !type.isWalkable()) {
                    continue;
                }

                out.add(place(r, c));
            }
        }
        return out;
    }

    /**
     * Cuantas decoraciones caben a la vista, para juzgar si el mapa da juego.
     * Solo cuenta las celdas con '#'.
     */
    static int candidateCount(char[][] layout) {
        int total = 0;
        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < layout[0].length; c++) {
                if (SymbolType.fromChar(layout[r][c]) == SymbolType.DECORATION) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Hash entero a [0,1). Determinista entre ejecuciones y entre maquinas.
     *
     * <p>Es el mismo esquema de mezcla de los hash enteros habituales: sin
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

    /**
     * Coloca una pieza en una celda marcada con '#'. La pieza y el giro
     * se eligen de forma determinista por hash de coordenadas.
     */
    private static Placed place(int row, int col) {
        // El grupo y la pieza salen de hashes con offsets distintos, para que
        // dos celdas vecinas no saquen siempre lo mismo.
        int group = groupOf(hash(row + 7919, col - 104729));
        Piece[] pieces = GROUPS[group];
        Piece piece = pieces[index(row - 31, col + 6151, pieces.length)];

        // Giro en multiplos de 90 grados: mantiene la pieza dentro de
        // su celda y evita angulos raros que destaquen en una sala
        // ortogonal.
        float yaw = 90f * index(row + 31, col + 17, 4);

        return new Placed(piece.model, row, col, yaw, piece.scale);
    }

    /** Todos los modelos del catalogo, para comprobaciones y tests. */
    public static java.util.List<String> modelosDelCatalogo() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Piece[] group : GROUPS) {
            for (Piece p : group) {
                out.add(p.model);
            }
        }
        return out;
    }

    /** La escala con la que se coloca un modelo del catalogo. */
    public static float escalaDe(String model) {
        for (Piece[] group : GROUPS) {
            for (Piece p : group) {
                if (p.model.equals(model)) {
                    return p.scale;
                }
            }
        }
        return 1f;
    }

}
