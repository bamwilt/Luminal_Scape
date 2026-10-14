package Render2D.layout;

import Render2D.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Coloca componentes en una cuadricula que puede estirar las celdas.
 *
 * <p>Es la version "dashboard" de {@link GridLayoutUI}: se le da un rectangulo
 * (el del contenedor) y reparte ese hueco en columnas y filas. Con
 * {@code fillX} las columnas se reparten el ancho a partes iguales, de modo que
 * un boton largo y uno corto acaban midiendo lo mismo; sin el, cada columna mide
 * lo que pida su componente mas ancho. Lo mismo con {@code fillY} para las
 * alturas.
 *
 * <p>Eje que no se rellena se centra: el bloque de botones queda en mitad del
 * panel aunque no lo ocupe entero.
 */
public final class GridLayout implements Layout {

    private final int columns;
    private final float gapX;
    private final float gapY;
    private final boolean fillX;
    private final boolean fillY;
    private final List<UIComponent> componentes = new ArrayList<>();

    /** Ancho de cada columna, medido con el componente mas ancho. */
    private float[] colWidths = new float[0];
    /** Alto de cada fila, medido con el componente mas alto. */
    private float[] rowHeights = new float[0];

    /**
     * @param columns celdas por fila, al menos 1
     * @param gapX    hueco horizontal entre celdas, en pixeles de interfaz
     * @param gapY    hueco vertical entre celdas, en pixeles de interfaz
     * @param fillX   si las columnas se reparten el ancho entero a partes iguales
     * @param fillY   si las filas se reparten el alto entero a partes iguales
     */
    public GridLayout(int columns, float gapX, float gapY, boolean fillX, boolean fillY) {
        this.columns = Math.max(1, columns);
        this.gapX = Math.max(0f, gapX);
        this.gapY = Math.max(0f, gapY);
        this.fillX = fillX;
        this.fillY = fillY;
    }

    /** Anade un componente a la siguiente celda en orden de lectura. */
    public GridLayout add(UIComponent componente) {
        if (componente != null) {
            componentes.add(componente);
        }
        return this;
    }

    @Override
    public List<UIComponent> components() {
        return componentes;
    }

    /** Filas que ocupa lo anadido, redondeando hacia arriba. */
    public int getFilas() {
        return componentes.isEmpty() ? 0 : (componentes.size() + columns - 1) / columns;
    }

    /**
     * Mide cada columna y cada fila con el componente mas ancho o mas alto que
     * le toca. Se recalcula en cada {@link #arrange}, porque un rotulo que cambia
     * de texto cambia de tamano.
     */
    private void medir() {
        int cols = componentes.isEmpty() ? 0 : columns;
        colWidths = new float[cols];
        rowHeights = new float[getFilas()];
        for (int i = 0; i < componentes.size(); i++) {
            int col = i % columns;
            int fila = i / columns;
            UIComponent c = componentes.get(i);
            colWidths[col] = Math.max(colWidths[col], c.getAncho());
            rowHeights[fila] = Math.max(rowHeights[fila], c.getAlto());
        }
    }

    @Override
    public float getAnchoTotal() {
        medir();
        float total = 0f;
        for (float w : colWidths) {
            total += w;
        }
        return total + gapX * Math.max(0, colWidths.length - 1);
    }

    @Override
    public float getAltoTotal() {
        medir();
        float total = 0f;
        for (float h : rowHeights) {
            total += h;
        }
        return total + gapY * Math.max(0, rowHeights.length - 1);
    }

    @Override
    public void arrange(float x, float y, float ancho, float alto) {
        int n = componentes.size();
        if (n == 0) {
            return;
        }
        medir();
        int filas = rowHeights.length;

        float[] col = colWidths.clone();
        float[] row = rowHeights.clone();
        if (fillX) {
            float cada = Math.max(0f, (ancho - gapX * (columns - 1)) / columns);
            for (int i = 0; i < col.length; i++) {
                col[i] = cada;
            }
        }
        if (fillY) {
            float cada = Math.max(0f, (alto - gapY * (filas - 1)) / filas);
            for (int i = 0; i < row.length; i++) {
                row[i] = cada;
            }
        }

        float bloqueW = gapX * Math.max(0, col.length - 1);
        for (float w : col) {
            bloqueW += w;
        }
        float bloqueH = gapY * Math.max(0, row.length - 1);
        for (float h : row) {
            bloqueH += h;
        }
        // Centrado en el contenedor en el eje que no se rellena.
        float startX = x + (ancho - bloqueW) / 2f;
        float startY = y + (alto - bloqueH) / 2f;

        for (int i = 0; i < componentes.size(); i++) {
            int c = i % columns;
            int f = i / columns;
            float cx = startX;
            for (int k = 0; k < c; k++) {
                cx += col[k] + gapX;
            }
            float cy = startY;
            for (int k = 0; k < f; k++) {
                cy += row[k] + gapY;
            }
            componentes.get(i).setBounds(cx, cy, col[c], row[f]);
        }
    }
}
