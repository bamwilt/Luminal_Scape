package Render3D.map;

import java.util.Locale;

/**
 * Traduce las palabras clave cualitativas de los archivos de nivel a los
 * números exactos con los que trabaja el render.
 *
 * <p>Está separado del parser a proposito: los mapas se escriben con palabras
 * ({@code view_distance: high}) porque se leen mejor, pero la niebla y la luz
 * son magnitudes continuas y quien renderiza necesita un float. Los valores
 * viven aqui y en ningun otro sitio, para que ajustar la sensacion del juego sea
 * tocar una tabla.
 */
public final class Atmosphere {

    // Los valores concretos de vista y niebla viven en {@link MapConfig}, que
    // es el panel unico de ajustes. Aqui solo se traduce la palabra clave del
    // nivel al numero.

    /**
     * Distancia a la que empieza la niebla para una distancia de vista dada.
     *
     * <p>Se queda siempre por debajo de la mitad de {@code viewDistance} para no
     * cruzar los dos valores, que dejarian la rampa al reves.
     */
    public static float fogNear(float viewDistance) {
        if (viewDistance <= 0f) {
            return 0f;
        }
        return Math.min(viewDistance * MapConfig.FOG_NEAR_RATIO, viewDistance * 0.5f);
    }

    // ---- Luz ambiente ----------------------------------------------------
    public static final float AMBIENT_DARK = 0.08f;
    public static final float AMBIENT_DIM = 0.20f;
    public static final float AMBIENT_NORMAL = 0.45f;
    public static final float AMBIENT_BRIGHT = 0.75f;

    /** Valor por defecto de {@code view_distance}: "normal". */
    public static final float DEFAULT_VIEW_DISTANCE = MapConfig.VIEW_DISTANCE_NORMAL;
    /** Valor por defecto de {@code ambient_light}: "dim". */
    public static final float DEFAULT_AMBIENT_LIGHT = AMBIENT_DIM;

    private Atmosphere() {
    }

    /**
     * Traduce {@code view_distance}.
     *
     * <p>{@code low} 10.0, {@code normal} 15.0, {@code high} 20.0: mas calidad,
     * mas lejos. Cualquier otra cosa (incluido ausente) es {@code normal}. Si el
     * mapa escribe un numero crudo se respeta tal cual, para no encerrar el
     * formato.
     *
     * @return la distancia de niebla en unidades de mundo.
     */
    public static float viewDistance(String keyword) {
        if (keyword == null) {
            return DEFAULT_VIEW_DISTANCE;
        }
        switch (keyword.trim().toLowerCase(Locale.ROOT)) {
            case "low":
                return MapConfig.VIEW_DISTANCE_LOW;
            case "normal":
                return MapConfig.VIEW_DISTANCE_NORMAL;
            case "high":
                return MapConfig.VIEW_DISTANCE_HIGH;
            default:
                float raw = parseFloat(keyword);
                return raw > 0f ? raw : DEFAULT_VIEW_DISTANCE;
        }
    }

    /**
     * Traduce {@code ambient_light}.
     *
     * <p>{@code dark} 0.08, {@code dim} 0.20, {@code normal} 0.45,
     * {@code bright} 0.75. Cualquier otra cosa (incluido ausente) es
     * {@code dim}.
     *
     * @return la luz ambiente en 0..1.
     */
    public static float ambientLight(String keyword) {
        if (keyword == null) {
            return DEFAULT_AMBIENT_LIGHT;
        }
        switch (keyword.trim().toLowerCase(Locale.ROOT)) {
            case "dark":
                return AMBIENT_DARK;
            case "dim":
                return DEFAULT_AMBIENT_LIGHT;
            case "normal":
                return AMBIENT_NORMAL;
            case "bright":
                return AMBIENT_BRIGHT;
            default:
                float raw = parseFloat(keyword);
                return raw >= 0f ? raw : DEFAULT_AMBIENT_LIGHT;
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return -1f;
        }
    }
}
