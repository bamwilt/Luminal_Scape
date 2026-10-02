package main;

/**
 * Configuración global del juego. Centraliza constantes que estaban
 * dispersas en Main, facilitando ajustes sin tocar la lógica.
 */
public final class GameConfig {

    private GameConfig() {
    }

    // Ventana
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final String WINDOW_TITLE = "Luminal Scape";

    // Tiempo de partida
    public static final int TOTAL_TIME_SECONDS = 60;
    public static final int ITEM_BONUS_SECONDS = 10;
    public static final int TARGET_FPS = 60;

    // Aviso de tiempo scarce en el HUD. Se marca en dos escalones: primero
    // parpadeo lento en ambar al entrar en el primer umbral, y rojo rapido en
    // el ultimo. Los umbrales se toman como fraccion del tiempo del nivel (para
    // que un nivel corto no avise demasiado tarde) con un tope en segundos.
    public static final float TIME_WARNING_FRACTION = 0.35f;
    public static final float TIME_CRITICAL_FRACTION = 0.15f;
    public static final int TIME_WARNING_MAX_SECONDS = 30;
    public static final int TIME_CRITICAL_MAX_SECONDS = 10;
    /** Parpadeos por segundo en cada escalon del aviso. */
    public static final float TIME_WARNING_BLINK_HZ = 2.5f;
    public static final float TIME_CRITICAL_BLINK_HZ = 6f;

    // Recursos
    public static final String WALL_TEXTURE = "textures/backWall.jpg";
    public static final String FLOOR_TEXTURE = "textures/Floor.jpg";
    public static final String CEILING_TEXTURE = "textures/br_ceiling_tiles.png";
    /** Logo de la pantalla de título, sustituye al texto grande "LUMINAL SCAPE". */
    public static final String TITLE_LOGO_TEXTURE = "textures/title_liminal.png";
    /** Anchura del logo como fracción de la ventana (la altura sale de la imagen). */
    public static final float TITLE_LOGO_WIDTH_RATIO = 0.58f;
    public static final float TITLE_LOGO_TOP_RATIO = 0.06f;
    public static final String FONT_PATH = "fonts/Roboto-Bold.ttf";
    public static final int FONT_SIZE = 28;

    // Mensajes grandes de visión
    public static final int VISION_FONT_SIZE = 64;

    // Iluminación
    public static final float LIGHT_X = 0.0f;
    public static final float LIGHT_Y = 5.0f;
    public static final float LIGHT_Z = 0.0f;
    public static final float LIGHT_R = 0.3f;
    public static final float LIGHT_G = 0.3f;
    public static final float LIGHT_B = 0.1f;
    public static final float EMISSION_STRENGTH = 0.2f;
    public static final float GLOW_INTENSITY = 0.2f;

    // Cámara inicial
    public static final float CAMERA_START_X = 0.0f;
    public static final float CAMERA_START_Y = 3.0f;
    public static final float CAMERA_START_Z = 3.0f;

    // Vista de proyección
    public static final float FOV_DEGREES = 45.0f;
    public static final float NEAR_PLANE = 0.1f;
    public static final float FAR_PLANE = 100.0f;

    // Cielo: color del horizonte (tambien el destino de la niebla y el color de
    // limpieza del frame) y del cenit. El shader mezcla de uno a otro segun la
    // altura de la direccion de vista, asi que el cenit debe ser mas oscuro
    // para que el degradado lea como profundidad y no como luz plana.
    // Cada nivel puede sobreescribirlos desde su level_type.
    public static final float DEFAULT_SKY_HORIZON_R = 0.53f;
    public static final float DEFAULT_SKY_HORIZON_G = 0.48f;
    public static final float DEFAULT_SKY_HORIZON_B = 0.13f;
    public static final float DEFAULT_SKY_ZENITH_R = 0.18f;
    public static final float DEFAULT_SKY_ZENITH_G = 0.16f;
    public static final float DEFAULT_SKY_ZENITH_B = 0.06f;
}