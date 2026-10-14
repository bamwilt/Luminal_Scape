package Render2D.ui;

/**
 * Contrato minimo de un elemento de interfaz.
 *
 * <p>Un componente se coloca en un rectangulo de unidades de interfaz (pixels
 * virtuales), se dibuja y, si tiene comportamiento, attendera los eventos del
 * raton. Todas las pantallas se componen de estos elementos, asi que mientras no
 * sepamos que pantalla esta activa solo necesitamos saber "que hay que dibujar",
 * "donde va cada elemento" y "como se recolocan al cambiar el tamaño".
 *
 * <p>La posicion se expresa en pixeles virtuales, no en proporciones: la
 * interfaz se dibuja siempre en una resolucion fija ({@link UIScale}) y luego se
 * estira. Por eso el ancho y el alto son medidas de interfaz y no medidas reales
 * de pantalla, y por eso quien coloca elementos es
 * {@link GridLayoutUI}, no cada pantalla por su cuenta.
 *
 * <p>La interfaz esta separada del mundo: aqui no se toca OpenGL en 3D, ni
 * logica de juego, ni tiempo. Solo cuadrilateros, texto y coordenadas.
 */
public interface UIComponent {

    /**
     * Fija el rectangulo del componente en pixeles de interfaz.
     *
     * <p>La esquina superior izquierda es (x, y). Es la operacion que
     * {@link GridLayoutUI} usa para colocar el componente en su celda, asi que
     * un componente no debe decidir su propia posicion.
     *
     * @param x      borde izquierdo
     * @param y      borde superior
     * @param ancho  ancho del componente
     * @param alto   alto del componente
     */
    void setBounds(float x, float y, float ancho, float alto);

    /**
     * Dibuja el componente.
     *
     * @param windowWidth  ancho virtual de la ventana
     * @param windowHeight alto virtual de la ventana
     * @param windowHandle handle de GLFW, para leer el raton
     */
    void dibujar(int windowWidth, int windowHeight, long windowHandle);

    /** Ancho actual del componente en pixeles de interfaz. */
    float getAncho();

    /** Alto actual del componente en pixeles de interfaz. */
    float getAlto();
}