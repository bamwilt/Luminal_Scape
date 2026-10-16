package ConfigGlobal;

/** Rutas de recursos (texturas y fuentes) y tamanos de fuente. */
public final class ResourceConfig {

    public static final String TEXTURA_PARED = "textures/backWall.jpg";
    public static final String TEXTURA_SUELO = "textures/Floor.jpg";
    public static final String TEXTURA_TECHO = "textures/br_ceiling_tiles.png";
    /** Logo de la pantalla de titulo, sustituye al texto grande. */
    public static final String TEXTURA_LOGO = "textures/title_liminal.png";

    public static final String RUTA_FUENTE = "fonts/Roboto-Bold.ttf";
    public static final int TAMANO_FUENTE = 28;
    /** Mensajes grandes de vision. */
    public static final int TAMANO_FUENTE_VISION = 64;

    private ResourceConfig() {
    }
}
