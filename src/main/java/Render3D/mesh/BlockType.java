package Render3D.mesh;

/**
 * Tipo de bloque de muro que corresponde a cada una de las 16 máscaras de
 * {@link WallBitmask}. Es la tabla "autotiling": a partir de los vecinos
 * cardinales de una celda se decide aquí qué forma tiene el trozo de muro que
 * se dibuja en ella.
 *
 * <pre>
 *  máscara    tipo                          forma (M = pilar, | = conector)
 *  ------     --------------------------    --------------------------------
 *  0000 0     ISOLATED                  M con los 4 conectores = bloque suelto
 *  0001 1     CAP_NORTH                 M + | hacia el norte
 *  0010 2     CAP_SOUTH                 M + | hacia el sur
 *  0011 3     STRAIGHT_NS               M + | arriba y abajo  (muro vertical '◙')
 *  0100 4     CAP_EAST                  M + | hacia el este
 *  0101 5     CORNER_NE                 M + | arriba y a la derecha
 *  0110 6     CORNER_SE                 M + | abajo y a la derecha
 *  0111 7     TEE_NORTH                 M + | arriba, abajo y derecha
 *  1000 8     CAP_WEST                  M + | hacia el oeste
 *  1001 9     CORNER_NW                 M + | arriba y a la izquierda
 *  1010 10    CORNER_SW                 M + | abajo y a la izquierda
 *  1011 11    TEE_WEST                  M + | arriba, abajo y izquierda
 *  1100 12    STRAIGHT_EW               M + | a ambos lados    (muro horizontal '■')
 *  1101 13    TEE_SOUTH                 M + | abajo, izquierda y derecha
 *  1110 14    TEE_EAST                  M + | este, arriba y abajo
 *  1111 15    CROSS                     M + | en las 4 direcciones
 * </pre>
 *
 * <p>La nomenclatura sigue el conventionalismo de autotiling: las {@code TEE_X}
 * se nombran por la dirección hacia la que apunta el "tallo" (la pata que
 * sobresale de la barra), no por el lado abierto. La propia constante guarda su
 * máscara en {@link #mask()}, así que consultar qué lados tiene el tipo es
 * directo.
 */
public enum BlockType {

    ISOLATED(0b0000),
    CAP_NORTH(0b0001),
    CAP_SOUTH(0b0010),
    STRAIGHT_NS(0b0011),
    CAP_EAST(0b0100),
    CORNER_NE(0b0101),
    CORNER_SE(0b0110),
    TEE_NORTH(0b0111),
    CAP_WEST(0b1000),
    CORNER_NW(0b1001),
    CORNER_SW(0b1010),
    TEE_WEST(0b1011),
    STRAIGHT_EW(0b1100),
    TEE_SOUTH(0b1101),
    TEE_EAST(0b1110),
    CROSS(0b1111);

    /** La máscara de vecinos que produce este tipo. */
    private final int mask;

    BlockType(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    public boolean connectsNorth() {
        return WallBitmask.hasNorth(mask);
    }

    public boolean connectsSouth() {
        return WallBitmask.hasSouth(mask);
    }

    public boolean connectsEast() {
        return WallBitmask.hasEast(mask);
    }

    public boolean connectsWest() {
        return WallBitmask.hasWest(mask);
    }

    /** Cantidad de conectores que hay que dibujar además del pilar. */
    public int connectorCount() {
        return WallBitmask.count(mask);
    }

    /**
     * Una celda sin ningún vecino de muro se dibuja como un bloque macizo de
     * tamaño completo (pilar + los cuatro conectores), no como una columna
     * pelada: así un muro de una sola celda se sigue leyendo como un muro.
     */
    public boolean isIsolated() {
        return this == ISOLATED;
    }

    /** Muro recto: solo une dos lados opuestos. */
    public boolean isStraight() {
        return this == STRAIGHT_NS || this == STRAIGHT_EW;
    }

    /** Esquina en L: une dos lados adyacentes. */
    public boolean isCorner() {
        return this == CORNER_NE || this == CORNER_NW || this == CORNER_SE || this == CORNER_SW;
    }

    /** Intersección en T: une tres lados. */
    public boolean isTee() {
        return this == TEE_NORTH || this == TEE_SOUTH || this == TEE_EAST || this == TEE_WEST;
    }

    /** Intersección en cruz: une los cuatro lados. */
    public boolean isCross() {
        return this == CROSS;
    }

    /** Tabla máscara -> tipo, indexada por los 4 bits de la máscara. */
    private static final BlockType[] BY_MASK = new BlockType[WallBitmask.ALL + 1];

    static {
        for (BlockType type : values()) {
            BY_MASK[type.mask] = type;
        }
    }

    /** Tipo de bloque correspondiente a una máscara de vecinos. */
    public static BlockType fromMask(int mask) {
        return BY_MASK[mask & WallBitmask.ALL];
    }
}
