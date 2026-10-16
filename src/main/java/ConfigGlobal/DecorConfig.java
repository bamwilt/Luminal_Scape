package ConfigGlobal;

/**
 * Reparto del catalogo de decoracion: probabilidad acumulada de que una celda
 * '#' saque una pieza de cada grupo, en el orden plantas / mesas / alfombra.
 *
 * <p>Los numeros son probabilidad acumulada, y el ultimo es mayor que 1 para
 * que el ultimo grupo cubra siempre el resto del rango.
 */
public final class DecorConfig {

    public static final float[] PROP_DECOR_GROUP_ENDS = { 0.55f, 0.85f, 1.01f };

    private DecorConfig() {
    }
}
