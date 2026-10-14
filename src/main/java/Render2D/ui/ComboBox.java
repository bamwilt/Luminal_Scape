package Render2D.ui;

import Render2D.TextRenderer;
import java.util.ArrayList;
import java.util.List;

/**
 * Boton que despliega una lista de opciones, como un combo.
 *
 * <p>Arriba vive el boton principal ({@link Button}). Al soltar el clic dentro
 * se abre o se cierra. Abierto, sus opciones se dibujan como una grilla de una
 * sola columna pegada al borde inferior del principal: son botones cuadrados,
 * muy juntos, y tienen que quedar SIEMPRE por encima de lo demas.
 *
 * <p>Por eso el dibujo va en dos llamadas y las dos son cosa de quien coloca el
 * combo: {@link #dibujar} pinta solo el principal, alla donde toque en su
 * interface, y {@link #dibujarOpciones} pinta el desplegable y hay que
 * llamarla al FINAL del frame, despues de todo lo demas. Asi el orden de
 * dibujo del dueño ya no puede tapar las opciones: quien decide que va encima
 * es el frame, no el sitio donde este colocado el combo.
 *
 * <p>Es reutilizable: quien lo coloca le da el rectangulo principal con
 * {@link #setBounds} y las opciones se reparten debajo (mismo ancho, alto de
 * opcion). El texto de arriba muestra la opcion elegida para no tener que
 * abrirlo.
 */
public class ComboBox implements UIComponent {

    /** Colores de las cajas desplegadas, para que cada una tenga el suyo. */
    private static final int[][] PALETA = {
        {26, 34, 52, 245},
        {34, 44, 68, 247},
        {42, 54, 84, 248},
        {50, 66, 100, 248},
        {58, 78, 122, 250},
    };

    private final TextRenderer textRenderer;
    private final String titulo;
    private final Button principal;
    private final List<Button> opciones = new ArrayList<>();
    private final List<Runnable> acciones = new ArrayList<>();

    private boolean abierto;

    private float x;
    private float y;
    private float ancho;
    private float alto;

    /** Alto de cada opcion desplegada, en pixeles de interfaz. */
    private float altoOpcion = 34f;
    /** Hueco entre opciones: pequeno, para que se vean pegadas. */
    private float hueco = 2f;

    public ComboBox(String titulo, TextRenderer textRenderer) {
        this.titulo = titulo;
        this.textRenderer = textRenderer;
        this.principal = new Button(titulo, textRenderer, 16f, 14f, 180f);
        this.principal.setOnClickListener(() -> abierto = !abierto);
    }

    /** Alto de las opciones desplegadas (por defecto 34). */
    public ComboBox setAltoOpcion(float altoOpcion) {
        this.altoOpcion = Math.max(1f, altoOpcion);
        return this;
    }

    /**
     * Anade una opcion al final.
     *
     * @param etiqueta texto del boton desplegado
     * @param accion   lo que se dispara al elegirla (puede ser {@code null})
     */
    public ComboBox addOption(String etiqueta, Runnable accion) {
        Button opcion = new Button(etiqueta, textRenderer, 14f, 8f, 120f);
        int[] color = PALETA[opciones.size() % PALETA.length];
        opcion.setColorInt(color[0], color[1], color[2], color[3]);
        opcion.setOnClickListener(() -> {
            abierto = false;
            principal.setText(titulo + ": " + etiqueta);
            if (accion != null) {
                accion.run();
            }
        });
        opciones.add(opcion);
        acciones.add(accion);
        return this;
    }

    /** Cierra el desplegable si estaba abierto. */
    public void cerrar() {
        abierto = false;
    }

    /**
     * Si esta desplegado.
     *
     * <p>Es el nombre viejo de {@link #estaAbierto()}; se mantiene porque el
     * codigo y los tests la usaban, pero el nombre en español es el bueno.
     */
    public boolean isAbierto() {
        return abierto;
    }

    /** Pasa la escala de la ventana al boton principal y a sus opciones. */
    public void setScale(float uiScale) {
        principal.setScale(uiScale);
        for (Button opcion : opciones) {
            opcion.setScale(uiScale);
        }
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.ancho = Math.max(0f, ancho);
        this.alto = Math.max(0f, alto);
        principal.setBounds(x, y, this.ancho, this.alto);

        // Las opciones van justo debajo y con la misma anchura, en una sola
        // columna. Se colocan ya para que el clic y el dibujo solo tengan que
        // activarlas cuando el combo este abierto.
        float fila = y + this.alto + hueco;
        for (Button opcion : opciones) {
            opcion.setBounds(x, fila, this.ancho, altoOpcion);
            fila += altoOpcion + hueco;
        }
    }

    @Override
    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        // Solo el principal. Las opciones las pinta quien coloca el combo con
        // dibujarOpciones, al final del frame, para que nada las tape.
        principal.dibujar(windowWidth, windowHeight, windowHandle);
    }

    /**
     * Dibuja solo las opciones desplegadas.
     *
     * <p>Hay que llamarla al final del frame, despues de todos los demas
     * elementos de la interface, y solo si {@link #estaAbierto()}. Si nadie la
     * llama, el desplegable se abre (el clic funciona y las opciones reciben
     * raton) pero no se ve.
     *
     * @param windowWidth  ancho de la ventana, en pixeles de interface
     * @param windowHeight alto de la ventana, en pixeles de interface
     * @param windowHandle handle nativo de la ventana
     */
    public void dibujarOpciones(int windowWidth, int windowHeight, long windowHandle) {
        if (!abierto) {
            return;
        }
        for (Button opcion : opciones) {
            opcion.dibujar(windowWidth, windowHeight, windowHandle);
        }
    }

    /** true si el desplegable esta abierto (para saber si hay que pintarlo). */
    public boolean estaAbierto() {
        return abierto;
    }

    /** Reparte el raton entre el boton principal y, si esta abierto, sus opciones. */
    public void handleMouseEvent(long windowHandle) {
        principal.handleMouseEvent(windowHandle);
        if (abierto) {
            for (Button opcion : opciones) {
                opcion.handleMouseEvent(windowHandle);
            }
        }
    }

    @Override
    public float getAncho() {
        return ancho;
    }

    @Override
    public float getAlto() {
        return alto;
    }
}
