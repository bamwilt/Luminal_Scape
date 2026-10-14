package Render2D.ui;

import Render2D.SharedQuadMesh;
import Render2D.layout.Layout;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

/**
 * Recuadro con fondo, borde, margen interior y una {@link Layout} dentro.
 *
 * <p>Es el contenedor de la interfaz: no tiene comportamiento propio, solo
 * pinta su caja y coloca a sus hijos con la maqueta que se le da. Con esto el
 * dashboard del menu principal es un panel gris con borde que reparte sus
 * botones en una columna, y los botones en si son paneles que ademas escuchan
 * clics (ver {@link Button}).
 *
 * <p>El panel mide su contenido: mientras nadie le adjudique un rectangulo, su
 * ancho y alto salen de la maqueta mas el margen. Asi puede ser hijo de otra
 * maqueta que lo mida sin tener que saber su tamano de antemano.
 */
public class Panel implements UIComponent {

    private static SharedQuadMesh sharedMesh;

    private float x;
    private float y;
    private float ancho;
    private float alto;

    private float bgR;
    private float bgG;
    private float bgB;
    private float bgA;

    private float bordeR = 1f;
    private float bordeG = 1f;
    private float bordeB = 1f;
    private float bordeA;
    private float borde;

    private float padIzq;
    private float padDer;
    private float padArriba;
    private float padAbajo;

    private Layout layout;
    private final List<UIComponent> hijos = new ArrayList<>();

    /** Color de fondo, canales 0..1. */
    public Panel setBackground(float r, float g, float b, float a) {
        this.bgR = r;
        this.bgG = g;
        this.bgB = b;
        this.bgA = a;
        return this;
    }

    /** Color y grosor del borde, en pixeles de interfaz; grosor 0 = sin borde. */
    public Panel setBorder(float r, float g, float b, float a, float grosor) {
        this.bordeR = r;
        this.bordeG = g;
        this.bordeB = b;
        this.bordeA = a;
        this.borde = Math.max(0f, grosor);
        return this;
    }

    /** Margen interior igual en los cuatro lados. */
    public Panel setPadding(float todos) {
        return setPadding(todos, todos, todos, todos);
    }

    /** Margen interior por lado, en pixeles de interfaz. */
    public Panel setPadding(float izquierda, float derecha, float arriba, float abajo) {
        this.padIzq = Math.max(0f, izquierda);
        this.padDer = Math.max(0f, derecha);
        this.padArriba = Math.max(0f, arriba);
        this.padAbajo = Math.max(0f, abajo);
        return this;
    }

    /** Maqueta que reparte a los hijos dentro del margen. */
    public Panel setLayout(Layout layout) {
        this.layout = layout;
        return this;
    }

    /** Anade un hijo. Con {@link GridLayout} el orden es el de lectura. */
    public Panel add(UIComponent hijo) {
        if (hijo != null) {
            hijos.add(hijo);
        }
        return this;
    }

    public Layout getLayout() {
        return layout;
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        if (layout != null) {
            layout.arrange(x + padIzq, y + padArriba,
                    Math.max(0f, ancho - padIzq - padDer),
                    Math.max(0f, alto - padArriba - padAbajo));
        }
    }

    @Override
    public float getAncho() {
        if (ancho > 0f) {
            return ancho;
        }
        return layout == null ? 0f : layout.getAnchoTotal() + padIzq + padDer;
    }

    @Override
    public float getAlto() {
        if (alto > 0f) {
            return alto;
        }
        return layout == null ? 0f : layout.getAltoTotal() + padArriba + padAbajo;
    }

    @Override
    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ensureMesh();
        if (bgA > 0f) {
            sharedMesh.draw(x, y, ancho, alto, windowWidth, windowHeight,
                    color(bgR, bgG, bgB, bgA), 0f);
        }
        if (borde > 0f && bordeA > 0f) {
            int[] bc = color(bordeR, bordeG, bordeB, bordeA);
            sharedMesh.draw(x, y, ancho, borde, windowWidth, windowHeight, bc, 0f);
            sharedMesh.draw(x, y + alto - borde, ancho, borde, windowWidth, windowHeight, bc, 0f);
            sharedMesh.draw(x, y, borde, alto, windowWidth, windowHeight, bc, 0f);
            sharedMesh.draw(x + ancho - borde, y, borde, alto, windowWidth, windowHeight, bc, 0f);
        }

        for (UIComponent hijo : hijos) {
            hijo.dibujar(windowWidth, windowHeight, windowHandle);
        }
        // Los hijos de la maqueta son los que se colocan con add() en el
        // GridLayout/BorderLayout; hay que dibujarlos o el panel sale vacio
        // (solo con su caja).
        if (layout != null) {
            for (UIComponent hijo : layout.components()) {
                hijo.dibujar(windowWidth, windowHeight, windowHandle);
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    private static int[] color(float r, float g, float b, float a) {
        return new int[] {
            (int) (r * 255f), (int) (g * 255f), (int) (b * 255f), (int) (a * 255f)
        };
    }

    private static void ensureMesh() {
        if (sharedMesh == null) {
            sharedMesh = new SharedQuadMesh();
        }
    }

    /** Libera la malla compartida de los paneles. */
    public static void cleanupShared() {
        if (sharedMesh != null) {
            sharedMesh.cleanup();
            sharedMesh = null;
        }
    }
}
