package ConfigGlobal;

/**
 * Color del horizonte (tambien destino de la niebla y color de limpieza del
 * frame) y del cenit. El shader mezcla de uno a otro segun la altura de la
 * direccion de vista, asi que el cenit debe ser mas oscuro para que el degradado
 * lea como profundidad y no como luz plana. Cada nivel puede sobreescribirlos
 * desde su level_type.
 */
public final class SkyConfig {

    public static final float CIELO_HORIZONTE_R = 0.53f;
    public static final float CIELO_HORIZONTE_G = 0.48f;
    public static final float CIELO_HORIZONTE_B = 0.13f;
    public static final float CIELO_CENIT_R = 0.18f;
    public static final float CIELO_CENIT_G = 0.16f;
    public static final float CIELO_CENIT_B = 0.06f;

    private SkyConfig() {
    }
}
