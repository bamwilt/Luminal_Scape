package Render2D.components;

/**
 * Barra de progreso formada por dos {@link Panel}: el fondo y el relleno.
 *
 * <p>El progreso lo fija el programa con una fraccion ({@code 0..1}) o con un
 * par de cuentas ({@code getProgress()}). No guarda tiempo ni conoce de que es
 * el progreso: solo pinta el fondo entero y encima la parte proporcional del
 * relleno, de modo que con 2 de 10 la barra queda al 20%.
 *
 * <p>Va en {@code Render2D.ui} porque es un elemento de interfaz mas, como
 * {@link Button} o {@link Label}: quien lo coloca le da su rectangulo con
 * {@link #setBounds} y el solo reparte ese hueco entre las dos cajas.
 */
public class ProgressBar implements UIComponent {

    /** Margen interior entre el borde del fondo y el relleno, en pixeles. */
    private static final float MARGEN = 2f;

    private final Panel fondo;
    private final Panel relleno;

    private float x;
    private float y;
    private float ancho;
    private float alto;

    /** Fraccion ya pintada del fondo, en {@code 0..1}. */
    private float fraccion;

    public ProgressBar() {
        fondo = new Panel()
                .setBackground(0.05f, 0.06f, 0.09f, 0.92f)
                .setBorder(0.35f, 0.45f, 0.60f, 0.85f, 1f);
        relleno = new Panel()
                .setBackground(0.25f, 0.85f, 0.40f, 0.96f);
    }

    /** Color del relleno (canales 0..1). */
    public ProgressBar setFillColor(float r, float g, float b, float a) {
        relleno.setBackground(r, g, b, a);
        return this;
    }

    /** Color del fondo (canales 0..1). */
    public ProgressBar setTrackColor(float r, float g, float b, float a) {
        fondo.setBackground(r, g, b, a);
        return this;
    }

    /** Fija el progreso como fraccion saturada a {@code 0..1}. */
    public ProgressBar setProgress(float nuevaFraccion) {
        this.fraccion = Math.max(0f, Math.min(1f, nuevaFraccion));
        repartir();
        return this;
    }

    /**
     * Fija el progreso con dos cuentas: {@code actual} sobre {@code total}.
     *
     * <p>Es el uso que pide el programa: con 10 elementos y 2 hechos, la barra
     * queda al 20%. Un {@code total} no positivo se trata como barra vacia.
     */
    public ProgressBar setValue(int actual, int total) {
        return setProgress(total <= 0 ? 0f : Math.max(0f, (float) actual / (float) total));
    }

    /** Fraccion de relleno actual, {@code 0..1}. */
    public float getProgress() {
        return fraccion;
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.ancho = Math.max(0f, ancho);
        this.alto = Math.max(0f, alto);
        repartir();
    }

    /**
     * Reparte el hueco entre fondo y relleno con el progreso de ahora.
     *
     * <p>Se recalcula al colocar y al cambiar el progreso, para no depender del
     * orden en que se llamen: da igual primero {@code setBounds} y luego
     * {@code setValue} que al reves.
     */
    private void repartir() {
        fondo.setBounds(x, y, ancho, alto);
        float util = Math.max(0f, ancho - MARGEN * 2f);
        float rellenoAncho = util * fraccion;
        relleno.setBounds(x + MARGEN, y + MARGEN, rellenoAncho,
                Math.max(0f, alto - MARGEN * 2f));
    }

    @Override
    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        fondo.dibujar(windowWidth, windowHeight, windowHandle);
        relleno.dibujar(windowWidth, windowHeight, windowHandle);
    }

    @Override
    public float getAncho() {
        return ancho;
    }

    @Override
    public float getAlto() {
        return alto;
    }

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
}
