package ConfigGlobal;

/**
 * Tiempo de partida: duracion, bonus por item y los umbrales de aviso del HUD.
 *
 * <p>Los avisos se marcan en dos escalones: parpadeo lento en ambar al entrar
 * en el primer umbral y rojo rapido en el ultimo. Los umbrales van como
 * fraccion del tiempo del nivel (para que un nivel corto no avise tarde) con un
 * tope en segundos.
 */
public final class TimeConfig {

    public static final int TIEMPO_TOTAL_SEGUNDOS = 60;
    public static final int SEGUNDOS_POR_ITEM = 10;

    public static final float AVISO_TIEMPO_FRACCION = 0.35f;
    public static final float CRITICO_TIEMPO_FRACCION = 0.15f;
    public static final int AVISO_TIEMPO_MAX_SEGUNDOS = 30;
    public static final int CRITICO_TIEMPO_MAX_SEGUNDOS = 10;
    /** Parpadeos por segundo en cada escalon del aviso. */
    public static final float AVISO_TIEMPO_PARPADEO_HZ = 2.5f;
    public static final float CRITICO_TIEMPO_PARPADEO_HZ = 6f;

    private TimeConfig() {
    }
}
