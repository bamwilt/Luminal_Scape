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
    public static final int TOTAL_TIME_SECONDS = 5 * 60;
    public static final int TARGET_FPS = 60;

    // Recursos
    public static final String WALL_TEXTURE = "textures/backWall.jpg";
    public static final String FLOOR_TEXTURE = "textures/Floor.jpg";
    public static final String CEILING_TEXTURE = "textures/Floor2.jpg";
    public static final String FONT_PATH = "fonts/Roboto-Bold.ttf";
    public static final int FONT_SIZE = 28;

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

    // Color de fondo (exterior): amarillo oscuro, se ve por ventanas y puertas.
    public static final float CLEAR_R = 0.53f;
    public static final float CLEAR_G = 0.48f;
    public static final float CLEAR_B = 0.13f;
}