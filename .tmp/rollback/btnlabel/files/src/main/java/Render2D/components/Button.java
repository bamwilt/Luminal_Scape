package Render2D.components;

import Render2D.interaction.KeyEvent;
import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;



import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL11;

public class Button implements UIComponent, EventListener {

    public static final float CELDA_BOTON_MINIMA_H = 52f;

    private float x, y, width, height;
    private int windowWidth, windowHeight;
    private float uiScale = 1f;
    private float cornerRadius = 0f;
    private final float margenH;
    private final Label label;
    private final TextRenderer textRenderer;

    private int[] normalColor = {26,38,56,255};
    private int[] hoverColor = {60,90,130,255};
    private int[] pressedColor = {90,130,180,255};
    private int[] disabledColor = {80,80,80,255};
    private int[] currentColor = normalColor;
    private int[] textColor = {255,255,255};

    private boolean mousePressedInside = false;
    private boolean enabled = true;
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
        this.label = new Label(text, tr);
        TextRenderer.TextMetrics m = tr.measureText(text);
        this.width = Math.max(anchoMinimo, tr.getTextWidth(text) + margenH * 2f);
        this.height = Math.max(CELDA_BOTON_MINIMA_H, m.alto() + margenV * 2f);
    }

    @Override public void dibujar(int ww, int wh, long handle) {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int mx = mouseVirtualX(handle);
        int my = mouseVirtualY(handle);
        boolean hover = isInside(mx, my);

        if (!enabled) currentColor = disabledColor;
        else if (mousePressedInside) currentColor = pressedColor;
        else if (hover) currentColor = hoverColor;
        else currentColor = normalColor;

        SharedQuadMesh mesh = SharedQuadMesh.getInstance();
        if (mesh != null) mesh.draw(x, y, width, height, ww, wh, currentColor, cornerRadius);

        label.setColor(textColor[0]/255f, textColor[1]/255f, textColor[2]/255f);
        label.setBounds(x + margenH, y, Math.max(0f, width - margenH*2f), height);
        label.dibujar(ww, wh, handle);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    public void handleMouseEvent(long handle) {
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
    public void setCornerRadius(float r) { cornerRadius = r; }

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
