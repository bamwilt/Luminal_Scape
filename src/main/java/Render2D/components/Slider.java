package Render2D.components;

import java.util.ArrayList;
import java.util.List;

import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;
import static org.lwjgl.glfw.GLFW.*;
import Render2D.interaction.KeyEvent;

/**
 * Barra deslizante moderna.
 *
 * <p>Dibuja una barra fina y redondeada (tipo pill) y, debajo, el nombre
 * alineado a la izquierda y el porcentaje a la derecha.
 *
 * <p>{@link #value} es el unico estado de la barra: cuanto esta llena, de 0
 * (vacia) a 1 (llena). La barra se pinta con ese mismo numero
 * ({@code trackW * value}) y el porcentaje que se ve es {@code value * 100}.
 * El numero no se calcula aparte: un {@link Label} se enlaza al evento "cambio
 * de valor" ({@link #addListener}) y, cada vez que {@code value} cambia, el
 * label consulta {@link #getValue()} y se reescribe. Asi barra y numero no
 * pueden separarse.
 */
public class Slider implements UIComponent, EventListener {

    /** Escucha los cambios de valor de la barra. */
    public interface SliderListener {
        /** Se llama cada vez que {@code value} cambia. */
        void onValueChanged(Slider slider);
    }

    /** Grosor de la barra. */
    private static final float BAR_H = 8f;
    /** Hueco entre la barra y el nombre. */
    private static final float GAP_NOMBRE = 8f;

    private final TextRenderer tr;
    private final String label;
    /** Nombre del ajuste (debajo de la barra, a la izquierda). */
    private final Label labelComp;
    /** Porcentaje (debajo de la barra, a la derecha); se refresca por listener. */
    private final Label valueComp;
    private final Panel track, fill;
    private final List<SliderListener> listeners = new ArrayList<>();

    private float x, y, w, h;
    private float trackX, trackW;
    private float barY, textY, textH;

    /** Cuanto esta llena la barra: 0 vacia, 1 llena. Unico estado. */
    private float value = 0f;
    private float paso = 0.05f;
    private boolean arrastrando = false;
    private int[] fillColor = {0, 255, 0, 255};
    private int[] trackColor = {46, 54, 72, 255};
    private java.util.function.Consumer<Float> onChange;
    private float uiScale = 1f;

    public Slider(String label, TextRenderer tr) {
        this.tr = tr;
        this.label = label;
        labelComp = new Label(label, tr).setAlign(Label.Align.LEFT);
        labelComp.setColor(0.92f, 0.94f, 1f);
        valueComp = new Label("", tr).setAlign(Label.Align.RIGHT);
        valueComp.setColor(0.85f, 0.88f, 0.96f);
        // El numero se enlaza al evento de la barra: al cambiar value, el label
        // consulta el slider y se reescribe. addListener lo refresca ya una vez.
        addListener(this::refreshValueLabel);

        track = new Panel()
                .setBackground(trackColor[0] / 255f, trackColor[1] / 255f, trackColor[2] / 255f, trackColor[3] / 255f)
                .setCornerRadius(BAR_H / 2f);
        fill = new Panel()
                .setBackground(fillColor[0] / 255f, fillColor[1] / 255f, fillColor[2] / 255f, fillColor[3] / 255f)
                .setCornerRadius(BAR_H / 2f);
    }

    public Slider setFillColor(float r, float g, float b, float a) {
        fillColor = new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255)};
        if (fill != null) fill.setBackground(r, g, b, a);
        return this;
    }

    public Slider setPaso(float p) { paso = p; return this; }

    /** Engancha la accion del juego; recibe el valor 0..1. */
    public Slider setOnChange(java.util.function.Consumer<Float> c) { onChange = c; return this; }

    /**
     * Enlaza un listener al evento "cambio de valor". Al registrarlo se llama
     * una vez, para que quien escuche arranque sincronizado con el valor actual.
     */
    public Slider addListener(SliderListener l) {
        if (l != null) {
            listeners.add(l);
            l.onValueChanged(this);
        }
        return this;
    }

    /** Cuanto esta llena la barra, de 0 (vacia) a 1 (llena). */
    public float getValue() { return value; }

    /** Fija el valor avisando al juego (como si el jugador moviera la barra). */
    public Slider setValor(float v) { setValue(v, true); return this; }

    /** Fija el valor sin disparar la accion del juego (p. ej. al restablecer). */
    public void setValorSilencioso(float v) { setValue(v, false); }

    private void setValue(float v, boolean avisarJuego) {
        value = Math.max(0f, Math.min(1f, v));
        fill.setBounds(trackX, barY, trackW * value, BAR_H);
        for (SliderListener l : listeners) {
            l.onValueChanged(this);
        }
        if (avisarJuego && onChange != null) {
            onChange.accept(value);
        }
    }

    /** El label del numero consulta el slider y se reescribe. */
    private void refreshValueLabel(Slider s) {
        valueComp.setText(Math.round(s.getValue() * 100f) + "%");
    }

    @Override public void setBounds(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        textH = tr.measureText(label).alto();
        float contenido = BAR_H + GAP_NOMBRE + textH;
        float top = y + Math.max(0f, (h - contenido) / 2f);
        barY = top;
        trackX = x;
        trackW = w;
        track.setBounds(trackX, barY, trackW, BAR_H);
        fill.setBounds(trackX, barY, trackW * value, BAR_H);
        textY = barY + BAR_H + GAP_NOMBRE;
        // El nombre reserva el hueco del numero para no escribirse encima.
        float numW = tr.getTextWidth("100%") + 12f;
        labelComp.setBounds(x, textY, Math.max(0f, w - numW), textH);
        valueComp.setBounds(x, textY, w, textH);
    }

    @Override public void dibujar(int ww, int wh, long hh) {
        track.dibujar(ww, wh, hh);
        fill.dibujar(ww, wh, hh);
        labelComp.dibujar(ww, wh, hh);
        valueComp.dibujar(ww, wh, hh);
    }

    @Override public float getAncho() { return w; }
    @Override public float getAlto() { return h; }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    public void actualizarPosicion(int ww, int wh) {}

    public void handleMouseEvent(long handle) {
        int mx = mouseX(handle), my = mouseY(handle);
        int state = glfwGetMouseButton(handle, GLFW_MOUSE_BUTTON_LEFT);
        boolean over = mx >= x && mx <= x + w && my >= y && my <= y + h;
        if (state == GLFW_PRESS && over) { arrastrando = true; }
        else if (state == GLFW_RELEASE) { arrastrando = false; }
        if (arrastrando) {
            float rel = Math.max(0f, Math.min(1f, (mx - trackX) / trackW));
            if (rel != value) {
                setValue(rel, true);
            }
        }
    }

    public void setScale(float s) { this.uiScale = s; }

    private int mouseX(long h) { double[] a = new double[1]; glfwGetCursorPos(h, a, null); return (int) (a[0] / uiScale); }
    private int mouseY(long h) { double[] a = new double[1]; glfwGetCursorPos(h, null, a); return (int) (a[0] / uiScale); }

    @Override public boolean onMouseEvent(MouseEvent e) { return false; }
    @Override public boolean onKeyEvent(KeyEvent e) { return false; }
}
