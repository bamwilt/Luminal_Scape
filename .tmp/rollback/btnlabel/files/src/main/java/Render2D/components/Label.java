package Render2D.components;

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
        textRenderer.drawBase(text, x, y, r, g, b);
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
