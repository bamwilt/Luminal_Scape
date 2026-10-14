package Render2D.ui;

import Render2D.TextRenderer;

/**
 * Una linea de texto que ocupa hueco en la cuadricula.
 *
 * <p>Existe para que los rótulos entren en la misma maqueta que los botones. Si
 * el texto se dibuja suelto con coordenadas propias, cada linea necesita su
 * cuenta y basta con que una se salga un poco para montarse sobre el boton de
 * debajo. Como componente de pleno derecho, la cuadricula le reserva su celda y
 * el texto se dibuja centrado dentro de ella.
 *
 * <p>Solo guarda el texto y el color: el alto sale de la fuente, y el ancho
 * tambien, de modo que la celda se dimensiona al texto real y no a un valor
 * supuesto.
 */
public class Label implements UIComponent {

    private final TextRenderer textRenderer;

    private String texto;
    private float r = 1f;
    private float g = 1f;
    private float b = 1f;

    private float x;
    private float y;
    private float ancho;
    private float alto;

    public Label(String texto, TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
        this.texto = texto;
        medir();
    }

    public Label setText(String texto) {
        this.texto = texto;
        medir();
        return this;
    }

    public Label setColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    public String getText() {
        return texto;
    }

    /**
     * Dimensiona el componente al texto.
     *
     * <p>El ancho sale de {@link TextRenderer#getTextWidth} y el alto de la caja
     * real de la tinta, para que la celda que le toque en la cuadricula sea
     * justo la que ocupa el texto y no un cuadrado con la tinta descentrada.
     */
    private void medir() {
        TextRenderer.TextMetrics m = textRenderer.measureText(texto);
        this.ancho = textRenderer.getTextWidth(texto);
        this.alto = Math.max(1f, m.alto());
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
    }

    @Override
    public float getAncho() {
        return ancho;
    }

    @Override
    public float getAlto() {
        return alto;
    }

    @Override
    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        // La caja es invisible; si el texto no entra en el ancho que le ha dado
        // la maqueta, se recorta con "..." en vez de salirse o tapar al vecino.
        String visible = textRenderer.elide(texto, ancho);
        textRenderer.drawCentered(visible, x + ancho / 2f, y + alto / 2f, r, g, b);
    }
}