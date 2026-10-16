package Render2D.components;

import Render2D.interaction.KeyEvent;
import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;
import Render2D.layout.BorderLayout;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Boton = un {@link Panel} (fondo y borde) con un {@link Label} en el centro.
 *
 * <p>El {@code Panel} usa un {@link BorderLayout} con el label en la region
 * CENTER y un padding igual a los margenes ({@code margenH}/{@code margenV}).
 * Asi el label recibe siempre el area interior del boton: se centra en ella y,
 * si el texto no cabe, se recorta con {@code ...} sin salirse nunca de la caja.
 */
public class Button implements UIComponent, EventListener {

    public static final float CELDA_BOTON_MINIMA_H = 52f;

    private float x, y, width, height;
    private int windowWidth, windowHeight;
    private float uiScale = 1f;
    private float cornerRadius = 0f;
    private final float margenH;
    private final float margenV;
    private final Label label;
    private final Panel panel;
    private final TextRenderer textRenderer;

    private int[] normalColor = {26,38,56,255};
    private int[] hoverColor = {60,90,130,255};
    private int[] pressedColor = {90,130,180,255};
    private int[] disabledColor = {80,80,80,255};
    private int[] currentColor = normalColor;
    private int[] textColor = {255,255,255};

    private boolean mousePressedInside = false;
    private boolean enabled = true;
    private boolean visible = true;
    private OnClickListener onClickListener;

    public interface OnClickListener { void onClick(); }

    public Button(String text, TextRenderer tr) {
        this(text, tr, 34f, 16f, CELDA_BOTON_MINIMA_H);
    }

    public Button(String text, TextRenderer tr, float margenH, float margenV) {
        this(text, tr, margenH, margenV, CELDA_BOTON_MINIMA_H);
    }

    public Button(String text, TextRenderer tr, float margenH, float margenV, float anchoMinimo) {
        this.textRenderer = tr;
        this.margenH = margenH;
        this.margenV = margenV;
        this.label = new Label(text, tr);
        TextRenderer.TextMetrics m = tr.measureText(text);
        this.width = Math.max(anchoMinimo, tr.getTextWidth(text) + margenH * 2f);
        this.height = Math.max(CELDA_BOTON_MINIMA_H, m.alto() + margenV * 2f);

        // Panel = fondo + borde; su layout coloca el label en el CENTER y el
        // padding reserva los margenes, de modo que el label trabaja dentro del
        // area interior del boton.
        this.panel = new Panel()
                .setLayout(new BorderLayout().setCenter(this.label))
                .setPadding(margenH, margenH, margenV, margenV);
    }

    @Override public void dibujar(int ww, int wh, long handle) {
        if (!visible) {
            return;
        }
        int mx = mouseVirtualX(handle);
        int my = mouseVirtualY(handle);
        boolean hover = isInside(mx, my);

        if (!enabled) currentColor = disabledColor;
        else if (mousePressedInside) currentColor = pressedColor;
        else if (hover) currentColor = hoverColor;
        else currentColor = normalColor;

        // El color de estado se aplica al panel; el panel pinta fondo, borde y
        // despues el label (que va en su layout).
        panel.setBackground(currentColor[0]/255f, currentColor[1]/255f,
                currentColor[2]/255f, currentColor[3]/255f);
        panel.setCornerRadius(cornerRadius);
        label.setColor(textColor[0]/255f, textColor[1]/255f, textColor[2]/255f);
        panel.dibujar(ww, wh, handle);
    }

    public void handleMouseEvent(long handle) {
        if (!visible) {
            return;
        }
        int mx = mouseVirtualX(handle);
        int my = mouseVirtualY(handle);
        int state = glfwGetMouseButton(handle, GLFW_MOUSE_BUTTON_LEFT);
        if (state == GLFW_PRESS && isInside(mx, my)) {
            mousePressedInside = true;
        } else if (state == GLFW_RELEASE) {
            if (mousePressedInside && isInside(mx, my) && onClickListener != null && enabled) {
                onClickListener.onClick();
            }
            mousePressedInside = false;
        }
    }

    @Override public void setBounds(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        // El panel (y con el su BorderLayout) coloca el label dentro del area
        // interior. Sin esto el label se quedaria sin caja.
        panel.setBounds(x, y, w, h);
    }

    @Override public float getAncho() { return width; }
    @Override public float getAlto() { return height; }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    public void actualizarPosicion(int ww, int wh) {
        this.windowWidth = ww; this.windowHeight = wh;
    }

    public void setScale(float s) { this.uiScale = s <= 0 ? 1f : s; }

    public void setOnClickListener(OnClickListener l) { this.onClickListener = l; }

    public void setColorInt(int r,int g,int b,int a) { normalColor = new int[]{r,g,b,a}; }
    public void setColor(float r,float g,float b,float a) { normalColor = rgba(r,g,b,a); }
    public void setText(String t) { label.setText(t); }
    public String getText() { return label.getText(); }
    public void setEnabled(boolean e) { enabled = e; }
    public boolean isVisible() { return visible; }
    public Button setVisible(boolean v) { visible = v; return this; }
    public void setCornerRadius(float r) { cornerRadius = Math.max(0f, r); }

    private int mouseVirtualX(long h) {
        double[] mx = new double[1]; glfwGetCursorPos(h, mx, null);
        return (int)(mx[0] / uiScale);
    }
    private int mouseVirtualY(long h) {
        double[] my = new double[1]; glfwGetCursorPos(h, null, my);
        return (int)(my[0] / uiScale);
    }
    private boolean isInside(int mx, int my) {
        return mx >= x && mx <= x+width && my >= y && my <= y+height;
    }
    private static int[] rgba(float r,float g,float b,float a) {
        return new int[]{(int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255)};
    }

    // EventListener - para usar con EventBus
    @Override public boolean onMouseEvent(MouseEvent e) {
        if (e.type == MouseEvent.Type.PRESS && e.button == MouseEvent.Button.LEFT) {
            if (isInside((int)e.x, (int)e.y)) { mousePressedInside = true; return true; }
        } else if (e.type == MouseEvent.Type.RELEASE && e.button == MouseEvent.Button.LEFT) {
            if (mousePressedInside && isInside((int)e.x, (int)e.y)) {
                if (onClickListener != null && enabled) onClickListener.onClick();
                mousePressedInside = false;
                return true;
            }
            mousePressedInside = false;
        }
        return false;
    }
    @Override public boolean onKeyEvent(KeyEvent e) { return false; }
}
