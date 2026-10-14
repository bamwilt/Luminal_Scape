package Core;

/**
 * Ajustes de control que se cambian en la pantalla de Configuracion.
 *
 * <p>Los sliders de {@link MenuScreens} mueven un valor 0..1: es el recorrido
 * que sabe pintar {@link Render2D.ui.Slider}. Lo que ese recorrido significa en
 * el juego vive aqui, para que el rango de cada ajuste este escrito una sola
 * vez: el valor por defecto que se le pone al slider y el valor que acaba
 * aplicando {@link GameLoop} leen los mismos numeros, y no pueden separarse.
 *
 * <p>Solo tiene constantes y la traduccion entre los dos recorridos: no guarda
 * estado, no habla con nadie y no sabe quien la llama. Los valores que el
 * jugador confirma con "Guardar configuracion" se quedan aparte, en
 * {@link #guardar}, y "Restablecer por defecto" los vuelve a los de fabrica.
 */
public final class AjustesJuego {

    /**
     * Sensibilidad del raton, en grados de yaw por pixel de desplazamiento.
     *
     * <p>El raton se relee desde el centro de la ventana en cada frame (ver
     * {@link Player.MouseController}), asi que el numero es grados de giro por
     * pixel leido: con el maximo, un barrido de 200 pixeles gira la camara 80
     * grados.
     */
    public static final float SENSIBILIDAD_MIN = 0.02f;
    public static final float SENSIBILIDAD_MAX = 0.40f;
    public static final float SENSIBILIDAD_DEFECTO = 0.15f;

    /**
     * Multiplicador sobre la velocidad de andar y de correr del player.
     *
     * <p>Es un factor, no una velocidad: 1 deja las velocidades base de
     * {@link Player.Player} como estan y el ajuste solo las escala.
     */
    public static final float VELOCIDAD_MIN = 0.5f;
    public static final float VELOCIDAD_MAX = 2f;
    public static final float VELOCIDAD_DEFECTO = 1f;

    /** Volumen de partida de los seis ajustes, con el que arranca el juego. */
    private static final float MUSICA_INICIAL = 1f;
    private static final float EFECTOS_INICIAL = 1f;
    private static final float BRILLO_INICIAL = 0.5f;
    private static final float CONTRASTE_INICIAL = 0.5f;
    private static final float SENSIBILIDAD_INICIAL =
            recorridoSensibilidad(SENSIBILIDAD_DEFECTO);
    private static final float VELOCIDAD_INICIAL =
            recorridoVelocidad(VELOCIDAD_DEFECTO);

    /**
     * Los ultimos valores confirmados, como recorridos 0..1 del slider.
     *
     * <p>Arrancan en los de fabrica y solo los cambian
     * {@link #guardar(float...)} (lo que escribe "Guardar configuracion") y
     * {@link #cargar()} (lo que hace "Restablecer por defecto"). Se guardan
     * aqui y no en {@code GameLoop} porque son el valor que tiene que recordar
     * la pantalla de Configuracion: si se quedaran en los setters del juego,
     * reponer la pantalla obligaria a llevarlos de un lado a otro.
     */
    private static float musica = MUSICA_INICIAL;
    private static float efectos = EFECTOS_INICIAL;
    private static float brillo = BRILLO_INICIAL;
    private static float contraste = CONTRASTE_INICIAL;
    private static float sensibilidad = SENSIBILIDAD_INICIAL;
    private static float velocidad = VELOCIDAD_INICIAL;

    private AjustesJuego() {
        // Solo se leen sus constantes y se llaman sus traducciones.
    }

    /** Recorrido 0..1 del slider -> sensibilidad real del raton. */
    public static float sensibilidad(float recorrido) {
        return SENSIBILIDAD_MIN + recorrido * (SENSIBILIDAD_MAX - SENSIBILIDAD_MIN);
    }

    /** Sensibilidad real -> recorrido 0..1 que la deja en su sitio. */
    public static float recorridoSensibilidad(float sensibilidad) {
        return (sensibilidad - SENSIBILIDAD_MIN) / (SENSIBILIDAD_MAX - SENSIBILIDAD_MIN);
    }

    /** Recorrido 0..1 del slider -> multiplicador de velocidad. */
    public static float velocidad(float recorrido) {
        return VELOCIDAD_MIN + recorrido * (VELOCIDAD_MAX - VELOCIDAD_MIN);
    }

    /** Multiplicador de velocidad -> recorrido 0..1 que lo deja en su sitio. */
    public static float recorridoVelocidad(float escala) {
        return (escala - VELOCIDAD_MIN) / (VELOCIDAD_MAX - VELOCIDAD_MIN);
    }

    /**
     * Deja fijos los seis valores actuales como los confirmados.
     *
     * <p>Lo llama "Guardar configuracion", con lo que hay ahora mismo en los
     * sliders. No toca nada: guardar es recordar, no aplicar.
     */
    public static void guardar(float musica, float efectos, float brillo, float contraste,
                               float sensibilidadRecorrido, float velocidadRecorrido) {
        AjustesJuego.musica = acotar(musica);
        AjustesJuego.efectos = acotar(efectos);
        AjustesJuego.brillo = acotar(brillo);
        AjustesJuego.contraste = acotar(contraste);
        AjustesJuego.sensibilidad = acotar(sensibilidadRecorrido);
        AjustesJuego.velocidad = acotar(velocidadRecorrido);
    }

    /**
     * Restablece los seis valores a los de fabrica y devuelve lo que hay que
     * aplicar.
     *
     * <p>Devuelve el array en el mismo orden que {@link #guardar}: quien lo
     * llama recorre los valores y se los pasa a sus propios setters y a la
     * pantalla, que es quien sabe como se aplica cada uno.
     */
    public static float[] cargar() {
        musica = MUSICA_INICIAL;
        efectos = EFECTOS_INICIAL;
        brillo = BRILLO_INICIAL;
        contraste = CONTRASTE_INICIAL;
        sensibilidad = SENSIBILIDAD_INICIAL;
        velocidad = VELOCIDAD_INICIAL;
        return new float[] { musica, efectos, brillo, contraste, sensibilidad, velocidad };
    }

    private static float acotar(float valor) {
        return Math.max(0f, Math.min(1f, valor));
    }
}
