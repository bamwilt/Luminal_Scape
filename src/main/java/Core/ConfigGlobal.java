package Core;

/**
 * Configuración global del juego: window, mundo, interfaz, recursos, tiempo,
 * iluminación, cámara, proyección y cielo.
 *
 * <p>Todo lo que se pueda ajustar sin tocar la lógica vive aquí. El resto de
 * clases leen de esta clase y nunca escriben en ella: es la única fuente de
 * verdad, así que el comportamiento del juego se cambia tocando solo este
 * fichero.
 *
 * <p>No contiene comportamiento, solo constantes. Si alguna vez necesita
 * lógica (leer un fichero, validar rangos), eso pertenece a otra clase: esta
 * debe seguir siendo un simple punto de configuración.
 */
public final class ConfigGlobal {

    // ---------------------------------------------------------------------
    // GameWindow y pantalla
    // ---------------------------------------------------------------------

    public static final int ANCHO_VENTANA = 800;
    public static final int ALTO_VENTANA = 600;
    public static final String TITULO_VENTANA = "Luminal Scape";

    // ---------------------------------------------------------------------
    // Mundo / muros (3D)
    // ---------------------------------------------------------------------

    /** Velocidad andando, en unidades de mundo por segundo. */
    public static final float VELOCIDAD_MOVIMIENTO = 20.0f;
    /** Velocidad con Shift, en unidades de mundo por segundo. */
    public static final float VELOCIDAD_CARRERA = 40.0f;
    /** Sensibilidad del ratón, en grados de yaw por pixel. */
    public static final float SENSIBILIDAD_MOUSE = 0.1f;

    // ---------------------------------------------------------------------
    // Interfaz de usuario (2D)
    // ---------------------------------------------------------------------

    public static final float UI_ESCALA_TEXTO_TITULO = 1.0f;
    public static final float UI_ESCALA_TEXTO_NORMAL = 1.0f;

    /** Cian brillante: selección y elementos activos. */
    public static final float[] COLOR_UI_PRIMARIO = { 0.0f, 0.8f, 0.8f, 1.0f };
    /** Blanco suave: texto normal. */
    public static final float[] COLOR_UI_SECUNDARIO = { 0.9f, 0.9f, 0.9f, 1.0f };
    /** Fondo oscuro translúcido: botones. */
    public static final float[] COLOR_UI_FONDO_BOTON = { 0.15f, 0.18f, 0.22f, 0.8f };

    // ---------------------------------------------------------------------
    // Tiempo de partida
    // ---------------------------------------------------------------------

    public static final int TIEMPO_TOTAL_SEGUNDOS = 60;
    public static final int SEGUNDOS_POR_ITEM = 10;
    public static final int FPS_OBJETIVO = 60;

    /**
     * Aviso de tiempo scarce en el HUD. Se marca en dos escalones: primero
     * parpadeo lento en ámbar al entrar en el primer umbral, y rojo rápido en
     * el último. Los umbrales se toman como fracción del tiempo del nivel (para
     * que un nivel corto no avise demasiado tarde) con un tope en segundos.
     */
    public static final float AVISO_TIEMPO_FRACCION = 0.35f;
    public static final float CRITICO_TIEMPO_FRACCION = 0.15f;
    public static final int AVISO_TIEMPO_MAX_SEGUNDOS = 30;
    public static final int CRITICO_TIEMPO_MAX_SEGUNDOS = 10;
    /** Parpadeos por segundo en cada escalón del aviso. */
    public static final float AVISO_TIEMPO_PARPADEO_HZ = 2.5f;
    public static final float CRITICO_TIEMPO_PARPADEO_HZ = 6f;

    // ---------------------------------------------------------------------
    // Decoración (reparto del catálogo)
    // ---------------------------------------------------------------------

    /**
     * Reparto del catálogo de decoración: probabilidad acumulada de que una celda
     * '#' saque una pieza de cada grupo, en el orden plantas / mesas / alfombra.
     *
     * <p>Antes la decoración se repartía sola por la forma del mapa, con un
     * porcentaje por condición (esquinas, callejones, puertas). Ahora la decide
     * quien escribe el nivel: se pone un '#' donde se quiere una pieza y lo único
     * que queda que ajustar es qué proporción saca cada grupo. Los números son
     * probabilidad acumulada, y el último es mayor que 1 para que el último grupo
     * cubra siempre el resto del rango.
     */
    public static final float[] PROP_DECOR_GROUP_ENDS = { 0.55f, 0.85f, 1.01f };

    /**
     * Densidad del generador automático de decoración: probabilidad de que una
     * celda de suelo transitable reciba una pieza, además de las que pide el
     * nivel a mano con '#'.
     *
     * <p>Vuelve a ser un ajuste y no un número dentro de {@code PropLayout}
     * porque es lo primero que se quiere tocar cuando un mapa se ve pelado o
     * abarrotado: con cambiarlo aquí basta, sin recompilar la lógica. El valor
     * es el que tenía el reparto automático antiguo, medido como el punto en el
     * que la decoración puntúa el recorrido en vez de llenarlo.
     */

    // ---------------------------------------------------------------------
    // Recursos
    // ---------------------------------------------------------------------

    public static final String TEXTURA_PARED = "textures/backWall.jpg";
    public static final String TEXTURA_SUELO = "textures/Floor.jpg";
    public static final String TEXTURA_TECHO = "textures/br_ceiling_tiles.png";
    /** Logo de la pantalla de título, sustituye al texto grande. */
    public static final String TEXTURA_LOGO = "textures/title_liminal.png";
    /** Anchura del logo como fracción de la window (la altura sale de la imagen). */
    public static final float LOGO_ANCHURA_FRACCION = 0.58f;
    public static final float LOGO_TOP_FRACCION = 0.06f;

    public static final String RUTA_FUENTE = "fonts/Roboto-Bold.ttf";
    public static final int TAMANO_FUENTE = 28;
    /** Mensajes grandes de visión. */
    public static final int TAMANO_FUENTE_VISION = 64;

    // ---------------------------------------------------------------------
    // Iluminación
    // ---------------------------------------------------------------------

    public static final float LUZ_X = 0.0f;
    public static final float LUZ_Y = 5.0f;
    public static final float LUZ_Z = 0.0f;
    public static final float LUZ_R = 0.3f;
    public static final float LUZ_G = 0.3f;
    public static final float LUZ_B = 0.1f;
    public static final float EMISION = 0.2f;
    public static final float BRILLO = 0.2f;

    // ---------------------------------------------------------------------
    // Cámara inicial y proyección
    // ---------------------------------------------------------------------

    public static final float CAMARA_X = 0.0f;
    public static final float CAMARA_Y = 3.0f;
    public static final float CAMARA_Z = 3.0f;

    public static final float FOV_GRADOS = 45.0f;
    public static final float PLANO_CERCANO = 0.1f;
    public static final float PLANO_LEJANO = 100.0f;

    // ---------------------------------------------------------------------
    // Cielo
    // ---------------------------------------------------------------------

    /**
     * Color del horizonte (también el destino de la niebla y el color de
     * limpieza del frame) y del cenit. El shader mezcla de uno a otro según la
     * altura de la dirección de vista, así que el cenit debe ser más oscuro
     * para que el degradado lea como profundidad y no como luz plana.
     * Cada nivel puede sobreescribirlos desde su level_type.
     */
    public static final float CIELO_HORIZONTE_R = 0.53f;
    public static final float CIELO_HORIZONTE_G = 0.48f;
    public static final float CIELO_HORIZONTE_B = 0.13f;
    public static final float CIELO_CENIT_R = 0.18f;
    public static final float CIELO_CENIT_G = 0.16f;
    public static final float CIELO_CENIT_B = 0.06f;

    private ConfigGlobal() {
        // Constructor privado: esta clase no se instancia, solo se leen sus
        // constantes desde ConfigGlobal.ANCHO_VENTANA, etc.
    }
}