package Render3D.mesh;

/**
 * Bitmask de vecinos cardinales usada por el autotiling de muros.
 *
 * <p>Cada celda de muro mira sus 4 vecinos cardinales (Norte, Sur, Este, Oeste)
 * y guarda un bit por cada lado que sigue siendo muro. Con esos 4 bits
 * ({@link #ALL} = 16 combinaciones) queda descrito el tipo de bloque que le
 * toca a la celda: muro recto, esquina en L, Tee o cruz (ver
 * {@link BlockType}).
 *
 * <pre>
 *        N (bit 0 = 1)
 *            |
 *   O -------+------- E      mask = N | S | E | O
 *            |
 *        S (bit 1 = 2)
 * </pre>
 *
 * <p>La máscara es SIMÉTRICA por construcción: el bit E de una celda está
 * activo si y solo si la celda de su este es un muro, y esa celda tiene su bit
 * O activo. Esa simetría es la garantía de que dos cajas vecinas siempre se
 * tocan justo en la frontera de la casilla, sin huecos ni solapes
 * (ver {@link WallAutotiler}).
 */
public final class WallBitmask {

    public static final int NONE = 0;
    public static final int NORTH = 1;
    public static final int SOUTH = 1 << 1;
    public static final int EAST = 1 << 2;
    public static final int WEST = 1 << 3;
    public static final int ALL = NORTH | SOUTH | EAST | WEST;

    private WallBitmask() {
    }

    /** Construye la máscara a partir de los cuatro vecinos. */
    public static int of(boolean north, boolean south, boolean east, boolean west) {
        return (north ? NORTH : NONE)
                | (south ? SOUTH : NONE)
                | (east ? EAST : NONE)
                | (west ? WEST : NONE);
    }

    public static boolean has(int mask, int bit) {
        return (mask & bit) != 0;
    }

    public static boolean hasNorth(int mask) {
        return has(mask, NORTH);
    }

    public static boolean hasSouth(int mask) {
        return has(mask, SOUTH);
    }

    public static boolean hasEast(int mask) {
        return has(mask, EAST);
    }

    public static boolean hasWest(int mask) {
        return has(mask, WEST);
    }

    public static int count(int mask) {
        return Integer.bitCount(mask & ALL);
    }

    /** Nombre corto de la máscara, útil para depurar y para logs. */
    public static String describe(int mask) {
        if (mask == NONE) {
            return "ISOLATED";
        }
        return (hasNorth(mask) ? "N" : "")
                + (hasSouth(mask) ? "S" : "")
                + (hasEast(mask) ? "E" : "")
                + (hasWest(mask) ? "O" : "-");
    }
}
