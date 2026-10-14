package Core;

/**
 * Ajustes de vista que el jugador mueve en Configuracion.
 *
 * <p>Son variables (no constantes) porque cambian en caliente: el shader del
 * escenario lee {@link #brillo} y {@link #contraste} en cada frame y los aplica
 * sobre la superficie antes de la niebla, de modo que el cambio se ve al
 * instante y el fondo sigue fundiendose con el color del horizonte del cielo.
 */
public final class VideoSettings {

    private VideoSettings() {
    }

    /** Brillo global de la escena (1.0 = neutro; rango 0.5..1.5). */
    public static float brillo = 1.0f;

    /** Contraste de la escena (1.0 = neutro; rango 0.5..1.5). */
    public static float contraste = 1.0f;
}
