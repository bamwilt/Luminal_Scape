package Render2D.layout;

import Render2D.components.UIComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Coloca componentes de interfaz en una cuadricula de celdas.
 *
 * <p>Existe porque los menus se maquetaban a mano: cada boton se situaba con
 * una cuenta distinta respecto al centro de la pantalla y cada texto con su
 * propio desplazamiento. Con muchos elementos eso no cuadra: los textos se
 * montan sobre los botones, las flechas {@code < >} se meten dentro de la
 * etiqueta del nivel y cada linea necesita su propio hueco.
 *
 * <p>Aqui no hay posiciones escritas a mano. Se declara cuantas columnas tiene
 * la cuadrica, cuanto separacion hay entre ellas, y se anaden los componentes
 * en orden de lectura. El elemento {@code i} cae en la celda
 * {@code col = i % columnas}, {@code fila = i / columnas}.
 *
 * <p>Las celdas NO tienen una medida impuesta: cada columna es tan ancha como el
 * componente mas ancho que le toca, y cada fila tan alta como el mas alto. Es la
 * diferencia entre "centrar cada elemento por su cuenta" y "darle a cada
 * elemento un hueco" — un texto largo recibe su hueco y empuja al de al lado en
 * vez de montarse encima.
 *
 * <pre>{@code
 * // Flecha, rotulo, flecha: la etiqueta manda en la anchura del centro.
 * GridLayoutUI selector = new GridLayoutUI(3,
 *         new UIComponent[]{botonIzq, etiqueta, botonDer}, 18f, 0f);
 *
 * // Tres botones en columna.
 * GridLayoutUI menu = new GridLayoutUI(1,
 *         new UIComponent[]{continuar, reiniciarTiempo, salir}, 0f, 22f);
 * }</pre>
 *
 * <p>El grid no dibuja nada: solo reparte rectangulos y quien dibuja es cada
 * componente, a partir del rectangulo que le ha tocado.
 */
public final class GridLayoutUI {

    private final int columnas;
    private final float separacionX;
    private final float separacionY;
    private final List<UIComponent> componentes = new ArrayList<>();

    /**
     * Ancho de cada columna y alto de cada fila, calculados a partir de los
     * componentes. Se recalculan en cada {@link #apply()}, porque un rotulo que
     * cambia de texto ("Nivel 1" -> "Nivel 10") cambia de ancho.
     */
    private float[] anchosColumna;
    private float[] altosFila;

    /** Origen en pixeles de interfaz donde se dibuja la celda (0,0). */
    private float origenX;
    private float origenY;

    /**
     * @param columnas    celdas por fila, al menos 1
     * @param separacionX hueco horizontal entre celdas, en pixeles
     * @param separacionY hueco vertical entre celdas, en pixeles
     * @param componentes los componentes en orden de lectura
     */
    public GridLayoutUI(int columnas, UIComponent[] componentes,
            float separacionX, float separacionY) {
        this.columnas = Math.max(1, columnas);
        this.separacionX = Math.max(0f, separacionX);
        this.separacionY = Math.max(0f, separacionY);
        if (componentes != null) {
            for (UIComponent c : componentes) {
                add(c);
            }
        }
        medirCeldas();
    }

    /**
     * Anade un componente a la siguiente celda en orden de lectura.
     *
     * @return esta misma cuadrica, para encadenar
     */
    public GridLayoutUI add(UIComponent componente) {
        if (componente != null) {
            componentes.add(componente);
            medirCeldas();
        }
        return this;
    }

    /**
     * Anade varios componentes seguidos.
     *
     * @return esta misma cuadrica, para encadenar
     */
    public GridLayoutUI addAll(UIComponent... nuevos) {
        for (UIComponent c : Arrays.asList(nuevos)) {
            add(c);
        }
        return this;
    }

    public int getColumnas() {
        return columnas;
    }

    public int getComponentes() {
        return componentes.size();
    }

    /** Numero de filas que ocupa lo anadido, redondeando hacia arriba. */
    public int getFilas() {
        return componentes.isEmpty() ? 0 : (componentes.size() + columnas - 1) / columnas;
    }

    /** Ancho total de la cuadrica, separaciones incluidas. */
    public float getAnchoTotal() {
        if (componentes.isEmpty()) {
            return 0f;
        }
        float total = 0f;
        for (float ancho : anchosColumna) {
            total += ancho;
        }
        return total + separacionX * (anchosColumna.length - 1);
    }

    /** Alto total de la cuadrica, separaciones incluidas. */
    public float getAltoTotal() {
        if (altosFila.length == 0) {
            return 0f;
        }
        float total = 0f;
        for (float alto : altosFila) {
            total += alto;
        }
        return total + separacionY * (altosFila.length - 1);
    }

    /**
     * Fija la esquina superior izquierda de la cuadrica, en pixeles de
     * interfaz, sin colocar todavia los componentes.
     */
    public GridLayoutUI setOrigen(float x, float y) {
        this.origenX = x;
        this.origenY = y;
        return this;
    }

    /**
     * Situa la cuadrica centrada en la ventana virtual y coloca cada componente
     * en su celda.
     *
     * @param windowWidth  ancho virtual de la ventana
     * @param windowHeight alto virtual de la ventana
     */
    public GridLayoutUI center(int windowWidth, int windowHeight) {
        return setOrigen((windowWidth - getAnchoTotal()) / 2f,
                (windowHeight - getAltoTotal()) / 2f)
                .apply();
    }

/**
     * Coloca cada componente en su celda sin tocar el origen.
     *
     * <p>Un componente mas pequeno que su celda queda CENTRADO dentro de ella, y
     * uno mas ancho la ensancha. Como la celda se mide con el componente mas
     * grande de su columna, no hay caso en el que un elemento se salga de su
     * hueco: el texto largo estira la celda y empuja al vecino de al lado en vez
     * de montarse encima, que era exactamente lo que pasaba en los menus.
     *
     * @return esta misma cuadrica
     */
    public GridLayoutUI apply() {
        medirCeldas();

        float[] inicioColumna = new float[anchosColumna.length];
        float x = origenX;
        for (int c = 0; c < anchosColumna.length; c++) {
            inicioColumna[c] = x;
            x += anchosColumna[c] + separacionX;
        }

        float[] inicioFila = new float[altosFila.length];
        float y = origenY;
        for (int f = 0; f < altosFila.length; f++) {
            inicioFila[f] = y;
            y += altosFila[f] + separacionY;
        }

        for (int i = 0; i < componentes.size(); i++) {
            UIComponent c = componentes.get(i);
            int col = i % columnas;
            int fila = i / columnas;

            // La celda se ha medido con ESTE MISMO componente hace un momento, asi que
            // nunca se pasa de su celda y no hay que recortarlo. Recortar aqui
            // taparia un error en vez de dejarlo ver.
            float w = c.getAncho();
            float h = c.getAlto();
            c.setBounds(inicioColumna[col] + (anchosColumna[col] - w) / 2f,
                    inicioFila[fila] + (altosFila[fila] - h) / 2f,
                    w,
                    h);
        }
        return this;
    }

    /** Quita todos los componentes, dejando la cuadrica como estaba. */
    public void clear() {
        componentes.clear();
        medirCeldas();
    }

    /**
     * Reparte lo que mide cada columna y cada fila.
     *
     * <p>La columna toma el ancho del componente mas ancho que le toca y la fila
     * el alto del mas alto. Es lo que hace que un rotulo largo estire su celda en
     * lugar de salirse de ella.
     */
    private void medirCeldas() {
        int filas = getFilas();
        int colsUsadas = componentes.isEmpty() ? 0 : Math.min(componentes.size(), columnas);

        anchosColumna = new float[colsUsadas];
        altosFila = new float[filas];

        for (int i = 0; i < componentes.size(); i++) {
            UIComponent c = componentes.get(i);
            int col = i % columnas;
            int fila = i / columnas;

            anchosColumna[col] = Math.max(anchosColumna[col], c.getAncho());
            altosFila[fila] = Math.max(altosFila[fila], c.getAlto());
        }
    }
}