package Render2D.components;

/**
 * Texto de una linea dentro de una caja.
 *
 * <p>Se centra verticalmente en su rectangulo y se alinea a la izquierda, al
 * centro o a la derecha. Si el texto no cabe a lo ancho, se recorta con
 * {@code ...} contra el ancho que le ha dado su contenedor (por ejemplo el area
 * central de un {@link Button}), de modo que nunca se sale de la caja.
 */
public class Label implements UIComponent {

    /** Alineacion horizontal del texto dentro de su caja. */
    public enum Align { LEFT, CENTER, RIGHT }

    private float x, y, width, height;
    private String text;
    private float r=1f, g=1f, b=1f;
    private Align align = Align.CENTER;
    private final TextRenderer textRenderer;

    public Label(String text, TextRenderer tr) {
        this.text = text;
        this.textRenderer = tr;
    }

    @Override public void setBounds(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    @Override public void dibujar(int ww, int wh, long handle) {
        String pintado = textoPintado();
        if (width > 0f && height > 0f) {
            float ancho = textRenderer.getTextWidth(pintado);
            float alto = textRenderer.measureText(pintado).alto();
            float tx;
            switch (align) {
                case LEFT:  tx = x; break;
                case RIGHT: tx = x + width - ancho; break;
                default:    tx = x + (width - ancho) / 2f;
            }
            float ty = y + (height - alto) / 2f;
            textRenderer.drawTope(pintado, tx, ty, r, g, b);
        } else {
            textRenderer.drawBase(pintado, x, y, r, g, b);
        }
    }

    /** Texto tal cual se dibujaria, ya recortado con {@code ...} si no cabe. */
    public String textoPintado() {
        if (width > 0f) {
            return textRenderer.elide(text, width);
        }
        return text;
    }

    @Override public float getAncho() { return textRenderer.getTextWidth(text); }
    @Override public float getAlto() { return textRenderer.measureText(text).alto(); }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    public void setText(String t) { this.text = t; }
    public String getText() { return text; }
    public Label setAlign(Align a) { this.align = (a == null) ? Align.CENTER : a; return this; }
    public Align getAlign() { return align; }
    public void setColor(float r, float g, float b) { this.r=r; this.g=g; this.b=b; }
    public void actualizarPosicion(int ww, int wh) {}
}
