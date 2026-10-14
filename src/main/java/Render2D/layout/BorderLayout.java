package Render2D.layout;

import Render2D.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Parte un rectangulo en regiones: lados y centro.
 *
 * <p>Es el reparto clasico de cinco zonas. Lo que pide la pantalla de titulo es
 * sencillo: el dashboard ocupa la <b>zona oeste</b> una fraccion del ancho (un
 * 34%) y el logo vive en el <b>centro</b>, que se queda con el resto. Las zonas
 * que no se usan no reservan hueco.
 *
 * <p>Las fracciones son del ancho (oeste/este) o del alto (norte/sur), no
 * pixeles: asi el reparto se mantiene al redimensionar la window.
 */
public final class BorderLayout implements Layout {

    /** Zonas del contenedor. */
    public enum Region {
        /** Franja superior. */
        NORTH,
        /** Franja inferior. */
        SOUTH,
        /** Franja izquierda (el dashboard). */
        WEST,
        /** Franja derecha. */
        EAST,
        /** El resto, en medio. */
        CENTER
    }

    private UIComponent north;
    private UIComponent south;
    private UIComponent west;
    private UIComponent east;
    private UIComponent center;

    private float westFraction;
    private float eastFraction;
    private float northFraction;
    private float southFraction;

    private float centerX;
    private float centerY;
    private float centerAncho;
    private float centerAlto;

    /** Coloca un componente en una region. */
    public BorderLayout set(Region region, UIComponent componente) {
        switch (region) {
            case NORTH:
                north = componente;
                break;
            case SOUTH:
                south = componente;
                break;
            case WEST:
                west = componente;
                break;
            case EAST:
                east = componente;
                break;
            default:
                center = componente;
        }
        return this;
    }

    /** Coloca la franja izquierda y fija que fraccion del ancho ocupa. */
    public BorderLayout setWest(UIComponent componente, float fraccion) {
        west = componente;
        westFraction = Math.max(0f, Math.min(1f, fraccion));
        return this;
    }

    /** Coloca la franja derecha y fija que fraccion del ancho ocupa. */
    public BorderLayout setEast(UIComponent componente, float fraccion) {
        east = componente;
        eastFraction = Math.max(0f, Math.min(1f, fraccion));
        return this;
    }

    /** Coloca la franja superior y fija que fraccion del alto ocupa. */
    public BorderLayout setNorth(UIComponent componente, float fraccion) {
        north = componente;
        northFraction = Math.max(0f, Math.min(1f, fraccion));
        return this;
    }

    /** Coloca la franja inferior y fija que fraccion del alto ocupa. */
    public BorderLayout setSouth(UIComponent componente, float fraccion) {
        south = componente;
        southFraction = Math.max(0f, Math.min(1f, fraccion));
        return this;
    }

    /** Coloca el centro (lo que sobra tras descontar las franjas). */
    public BorderLayout setCenter(UIComponent componente) {
        center = componente;
        return this;
    }

    @Override
    public void arrange(float x, float y, float ancho, float alto) {
        float top = y;
        float bottom = y + alto;
        float left = x;
        float right = x + ancho;

        if (north != null) {
            float h = alto * northFraction;
            north.setBounds(left, top, right - left, h);
            top += h;
        }
        if (south != null) {
            float h = alto * southFraction;
            south.setBounds(left, bottom - h, right - left, h);
            bottom -= h;
        }
        if (west != null) {
            float w = (right - left) * westFraction;
            west.setBounds(left, top, w, bottom - top);
            left += w;
        }
        if (east != null) {
            float w = (right - left) * eastFraction;
            east.setBounds(right - w, top, w, bottom - top);
            right -= w;
        }
        centerX = left;
        centerY = top;
        centerAncho = right - left;
        centerAlto = bottom - top;
        if (center != null) {
            center.setBounds(centerX, centerY, centerAncho, centerAlto);
        }
    }

    @Override
    public float getAnchoTotal() {
        return 0f;
    }

    @Override
    public float getAltoTotal() {
        return 0f;
    }

    /**
     * Componentes colocados en alguna region, en orden norte-sur-oeste-este-centro.
     *
     * <p>No se guardan en una lista propia: son los mismos objetos que reciben
     * {@link #set} o {@link #setWest} y demas. Aqui se recogen solo para que un
     * {@link Panel} que use este reparto sepa a quien dibujar.
     */
    @Override
    public List<UIComponent> components() {
        List<UIComponent> todos = new ArrayList<>(5);
        if (north != null) {
            todos.add(north);
        }
        if (south != null) {
            todos.add(south);
        }
        if (west != null) {
            todos.add(west);
        }
        if (east != null) {
            todos.add(east);
        }
        if (center != null) {
            todos.add(center);
        }
        return todos;
    }

    /** X del borde izquierdo de la zona central, tras el ultimo {@link #arrange}. */
    public float getCenterX() {
        return centerX;
    }

    /** Y del borde superior de la zona central, tras el ultimo {@link #arrange}. */
    public float getCenterY() {
        return centerY;
    }

    /** Ancho de la zona central, tras el ultimo {@link #arrange}. */
    public float getCenterAncho() {
        return centerAncho;
    }

    /** Alto de la zona central, tras el ultimo {@link #arrange}. */
    public float getCenterAlto() {
        return centerAlto;
    }
}
