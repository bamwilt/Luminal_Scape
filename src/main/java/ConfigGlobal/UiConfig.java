package ConfigGlobal;

/** Interfaz 2D: escalas de texto, colores y posicion del logo del titulo. */
public final class UiConfig {

    public static final float UI_ESCALA_TEXTO_TITULO = 1.0f;
    public static final float UI_ESCALA_TEXTO_NORMAL = 1.0f;

    /** Cian brillante: seleccion y elementos activos. */
    public static final float[] COLOR_UI_PRIMARIO = { 0.0f, 0.8f, 0.8f, 1.0f };
    /** Blanco suave: texto normal. */
    public static final float[] COLOR_UI_SECUNDARIO = { 0.9f, 0.9f, 0.9f, 1.0f };
    /** Fondo oscuro translucido: botones. */
    public static final float[] COLOR_UI_FONDO_BOTON = { 0.15f, 0.18f, 0.22f, 0.8f };

    /** Anchura del logo como fraccion de la ventana (la altura sale de la imagen). */
    public static final float LOGO_ANCHURA_FRACCION = 0.58f;
    public static final float LOGO_TOP_FRACCION = 0.06f;

    private UiConfig() {
    }
}
