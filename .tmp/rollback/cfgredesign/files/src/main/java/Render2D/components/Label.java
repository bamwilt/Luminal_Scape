package Render2D.components;

/**
 * Texto de una linea dentro de una caja.
 *
 * <p>Se centra en su rectangulo y, si el texto no cabe a lo ancho, se recorta
 * con {@code ...} antes de dibujarlo. El recorte se hace contra el ancho que le
 * ha dado su contenedor (por ejemplo el area central de un {@link Button}), de
 * modo que un texto largo nunca se sale de la caja.
 */
public class Label implements UIComponent {

    private float x, y, width, height;
    private String text;
    private float r=1f, g=1f, b=1f;
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
        if (height > 0f && width > 0f) {
            // Centrado en su caja: usa la posicion interna del contenedor.
            textRenderer.drawCentered(pintado, x + width / 2f, y + height / 2f, r, g, b);
        } else {
            // Sin caja asignada: se dibuja en su origen (comportamiento previo).
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
    public void setColor(float r, float g, float b) { this.r=r; this.g=g; this.b=b; }
    public void actualizarPosicion(int ww, int wh) {}
}
