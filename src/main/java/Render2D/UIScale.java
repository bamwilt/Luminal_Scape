package Render2D;

/**
 * Escala de la interfaz: la IU se dibuja SIEMPRE en una resolución virtual
 * fija y el resultado se estira a la ventana.
 *
 * <p>El problema que resuelve: antes, texto, botones y minimapa se medían en
 * píxeles reales, así que en una ventana pequeña el texto se salía del borde y
 * los botones se solapaban, y en una grande quedaban diminutos en una esquina.
 * Con esta clase, Main calcula una escala a partir del tamaño real de la
 * ventana y dibuja todo en unidades virtuales ({@link #REF_W} x
 * {@link #REF_H}). Cada capa 2D (texto, batch de quads, botones) recibe las
 * medidas virtuales en su proyección, y por eso escala sin tocar una sola
 * posición de la interfaz.
 *
 * <p>La escala sale del lado que MÁS APRIETA, {@code min(ancho/REF_W,
 * alto/REF_H)}, para que nada se recorte nunca. El sobrante en el lado más
 * ancho se queda como margen, y como todo se centra sobre las medidas
 * virtuales, ese margen queda repartido y la interfaz sigue en el centro real
 * de la ventana.
 *
 * <p>Por debajo de {@link #MIN_SCALE} no se reduce más: en una ventana
 * diminuta una interfaz ilegible no gana nada, y estirarla hasta el infinito
 * solo agranda el pixel.
 */
public final class UIScale {

    /** Ancho de referencia en unidades de interfaz. */
    public static final int REF_W = 1280;

    /** Alto de referencia en unidades de interfaz. */
    public static final int REF_H = 720;

    /** Tope inferior: por debajo de esta escala la interfaz deja de leerse. */
    public static final float MIN_SCALE = 0.45f;

    private UIScale() {
    }

    /**
     * Factor de escala de la interfaz para una ventana de {@code w} x {@code h}.
     *
     * <p>Devuelve 1.0 justo a {@link #REF_W} x {@link #REF_H}, por lo que
     * cualquier resolución mayor o igual que la de referencia se dibuja
     * ligeramente más grande y la interfaz mantiene siempre las mismas
     * proporciones.
     */
    public static float of(int w, int h) {
        if (w <= 0 || h <= 0) {
            return 1f;
        }
        float escala = Math.min(w / (float) REF_W, h / (float) REF_H);
        return Math.max(MIN_SCALE, escala);
    }

    /**
     * Ancho en unidades virtuales de una ventana de ancho {@code w}.
     *
     * <p>Nunca baja de {@link #REF_W}: si la ventana es más estrecha que la
     * referencia, la interfaz se dibuja en un ancho mayor y se ve más pequeña,
     * que es justo lo que se busca.
     */
    public static int virtualW(int w, float escala) {
        return Math.max(1, Math.round(w / escala));
    }

    /** Alto en unidades virtuales de una ventana de alto {@code h}. */
    public static int virtualH(int h, float escala) {
        return Math.max(1, Math.round(h / escala));
    }
}
