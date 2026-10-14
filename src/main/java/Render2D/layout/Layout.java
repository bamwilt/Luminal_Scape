package Render2D.layout;

import Render2D.ui.UIComponent;

/**
 * Reparte el rectangulo de un contenedor entre sus componentes.
 *
 * <p>Es lo que permite que un boton no sepa donde va: el contenedor le da un
 * rectangulo y la maqueta decide como repartirlo. Hay dos implementaciones:
 *
 * <ul>
 *   <li>{@link GridLayout}: cuadricula de columnas y filas, que puede rellenar
 *       el hueco entero (todos los botones del mismo tamano) o empaquetarse al
 *       tamano natural de sus piezas y centrarse.</li>
 *   <li>{@link BorderLayout}: regiones laterales y centrales, para partir una
 *       pantalla en un panel al lado (por ejemplo el dashboard) y el resto.</li>
 * </ul>
 *
 * <p>La maqueta no conoce OpenGL ni el raton: solo coloca rectangulos.
 */
public interface Layout {

    /**
     * Coloca los componentes dentro del rectangulo {@code (x, y, ancho, alto)}.
     *
     * <p>Cada hijo recibe su rectangulo con {@link UIComponent#setBounds}; el
     * contenedor no dibuja nada.
     */
    void arrange(float x, float y, float ancho, float alto);

    /**
     * Componentes que reparte la maqueta, en orden de dibujo.
     *
     * <p>Hace falta porque el contenedor ({@link Panel}) pinta a sus hijos: si la
     * maqueta se los guardara solo para medirlos, el panel no tendria forma de
     * saber que hay dentro y dibujaria su caja vacia.
     */
    java.util.List<UIComponent> components();

    /** Ancho natural que pide la maqueta, sin contar el contenedor. */
    float getAnchoTotal();

    /** Alto natural que pide la maqueta, sin contar el contenedor. */
    float getAltoTotal();
}
