package ConfigGlobal;

/**
 * Ajustes del jugador: movimiento, raton, cuerpo/agachado, colision por
 * substeps y head bob (balanceo de camara al caminar).
 */
public final class PlayerConfig {

    /** Velocidad andando, en unidades de mundo por segundo. */
    public static final float VELOCIDAD_MOVIMIENTO = 20.0f;
    /** Velocidad con Shift, en unidades de mundo por segundo. */
    public static final float VELOCIDAD_CARRERA = 40.0f;
    /** Sensibilidad del raton, en grados de yaw por pixel. */
    public static final float SENSIBILIDAD_MOUSE = 0.1f;

    // ---- Cuerpo y agachado ----------------------------------------------
    /** Cuanto baja la camara al agacharse, en unidades de mundo. */
    public static final float AGACHADO_ALTURA = 1.0f;
    public static final float JUGADOR_ANCHO = 1.0f;
    public static final float JUGADOR_PROFUNDIDAD = 1.0f;
    /** Altura del cuerpo de pie (la camara va mas arriba; ver MapConfig.CAMERA_HEIGHT). */
    public static final float JUGADOR_ALTURA = 1.8f;
    /** Velocidad de la transicion de agacharse/levantarse, en 1/s. */
    public static final float AGACHADO_VELOCIDAD = 5.0f;

    // ---- Colision por substeps ------------------------------------------
    public static final float MAX_STEP = 0.2f;
    public static final int MAX_SUBSTEPS = 64;

    // ---- Head bob --------------------------------------------------------
    /** Fase de balanceo por unidad recorrida (0.5 -> ~1.6 pasos/s andando). */
    public static final float BOB_FRECUENCIA = 0.5f;
    /** Altura del balanceo, en unidades de mundo. */
    public static final float BOB_AMPLITUD = 0.1f;
    /** Velocidad de entrada/salida del balanceo (rampa al pulsar/soltar). */
    public static final float BOB_ENTRADA_VELOCIDAD = 3.0f;

    private PlayerConfig() {
    }
}
