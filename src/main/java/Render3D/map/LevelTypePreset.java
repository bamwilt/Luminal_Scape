package Render3D.map;

import org.joml.Vector3f;

import main.GameConfig;

/**
 * Preset de tipo de nivel ({@code level_type}): la identidad visual de un
 * lugar. Se declara una vez en {@code level_types/<nombre>.txt} y lo heredan
 * todos los niveles que lo referencian con {@code level_type: <nombre>}, de
 * forma que cambiar el aspecto de un sitio es cambiar un archivo y no veinte
 * niveles.
 *
 * <p>Guarda las texturas de ambiente (suelo y techo) y los colores del cielo.
 * El cielo tiene dos colores porque el shader de cielo 3D interpola del
 * horizonte ({@code skyHorizonColor}) al cenit ({@code skyZenithColor}); el
 * color del horizonte es además el que se pasa como niebla, para que el fondo
 * y la niebla sean siempre el mismo color y no se note el corte.
 */
public final class LevelTypePreset {

    /** Perfil de cielo por defecto: el degradado lineal del shader. */
    public static final String SKY_TYPE_GRADIENT = "gradient";

    /**
     * Perfiles de cielo que el shader sabe pintar. Hoy solo existe el
     * degradado de dos colores; la clave queda en los datos para poder meter
     * mas perfiles sin tocar el formato de los archivos.
     */
    private static final java.util.Set<String> KNOWN_SKY_TYPES =
            java.util.Set.of(SKY_TYPE_GRADIENT);

    /** Si el perfil de cielo declarado existe en el shader. */
    public static boolean isKnownSkyType(String skyType) {
        return skyType != null && KNOWN_SKY_TYPES.contains(skyType.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private final String name;

    /** Textura del suelo; {@code null} usa la de {@link GameConfig}. */
    private final String floorTexture;
    /** Textura del techo; {@code null} usa la de {@link GameConfig}. */
    private final String ceilingTexture;
    /**
     * Textura del muro; {@code null} usa la de {@link GameConfig}.
     *
     * <p>Se dejo global mucho tiempo porque ningun preset necesitaba otra. Con
     * el paquete de los backrooms tiene sentido: el pasillo es hormigon, el
     * serpentario es caluroso y los backrooms son pintura amarilla, y con una
     * sola textura los tres niveles acababan con el mismo muro.
     */
    private final String wallTexture;

    private final String skyType;
    private final float skySpeed;
    private final Vector3f skyHorizonColor;
    private final Vector3f skyZenithColor;

    public LevelTypePreset(String name, String floorTexture, String ceilingTexture,
                           String wallTexture, String skyType, float skySpeed,
                           Vector3f skyHorizonColor, Vector3f skyZenithColor) {
        this.name = name;
        this.floorTexture = floorTexture;
        this.ceilingTexture = ceilingTexture;
        this.wallTexture = wallTexture;
        this.skyType = skyType;
        this.skySpeed = skySpeed;
        this.skyHorizonColor = skyHorizonColor;
        this.skyZenithColor = skyZenithColor;
    }

    /** Preset neutro: texturas por defecto y cielo del color de fondo actual. */
    public static LevelTypePreset defaultPreset(String name) {
        return new LevelTypePreset(name, null, null, null, SKY_TYPE_GRADIENT, 0.05f,
                new Vector3f(GameConfig.DEFAULT_SKY_HORIZON_R,
                        GameConfig.DEFAULT_SKY_HORIZON_G,
                        GameConfig.DEFAULT_SKY_HORIZON_B),
                new Vector3f(GameConfig.DEFAULT_SKY_ZENITH_R,
                        GameConfig.DEFAULT_SKY_ZENITH_G,
                        GameConfig.DEFAULT_SKY_ZENITH_B));
    }

    public String getName() {
        return name;
    }

    /** Textura del suelo, o {@code null} si el preset no la sobrescribe. */
    public String getFloorTexture() {
        return floorTexture;
    }

    /** Textura del techo, o {@code null} si el preset no la sobrescribe. */
    public String getCeilingTexture() {
        return ceilingTexture;
    }

    /** Textura del muro del preset, o {@code null} si no la fija. */
    public String getWallTexture() {
        return wallTexture;
    }

    public String getSkyType() {
        return skyType;
    }

    public float getSkySpeed() {
        return skySpeed;
    }

    public Vector3f getSkyHorizonColor() {
        return skyHorizonColor;
    }

    public Vector3f getSkyZenithColor() {
        return skyZenithColor;
    }

    @Override
    public String toString() {
        return "LevelTypePreset{" + name + ", cielo=" + skyType
                + " vel=" + skySpeed + "}";
    }
}
