package Render2D.ui;

import Render2D.SharedQuadMesh;
import Render2D.TextRenderer;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Boton de interfaz: un recuadro con eventos y un {@link Label} dentro.
 *
 * <p>El boton no lleva su propia posicion: la recibe de una maqueta
 * ({@link GridLayoutUI} o {@link GridLayout}) en {@link #setBounds}. Tampoco
 * dibuja el texto por su cuenta: el rotulo es un {@link Label} interno, con su
 * caja invisible, que se recorta con {@code "..."} si no entra en el ancho del
 * boton. El recuadro tiene las esquinas rectas ({@code cornerRadius = 0}).
 */
public class Button implements UIComponent {

    private static SharedQuadMesh sharedMesh;

    private float x;
    private float y;
    private float width;
    private float height;
    private int windowWidth;
    private int windowHeight;
    /**
     * Píxeles de window por unidad de interfaz ({@link UIScale}).
     *
     * <p>El botón se mide y se coloca en unidades virtuales, pero el ratón
     * llega en píxeles reales: sin esta scale el clic se compararía contra
     * un rectángulo virtual y a los botones les faltaría un factor de ancho y
     * alto por el lado que se agranda.
     */
    private float uiScale = 1f;
    /** Esquinas rectas: el menu quiere botones cuadrados, no redondeados. */
    private float cornerRadius = 0f;
    /** Margen horizontal que se deja alrededor del rotulo. */
    private final float margenH;
    private final Label label;
    private final TextRenderer textRenderer;

    // Colores RGBA (0-255); el canal A da la transparencia (por defecto algo
    // translúcidos para integrarse mejor con el fondo).
    private int[] normalColor = {26, 38, 56, 200};
    private int[] hoverColor = {60, 90, 130, 215};
    private int[] pressedColor = {90, 130, 180, 235};
    private int[] disabledColor = {80, 80, 80, 150};
    private int[] currentColor = normalColor;
    private int[] textColor = {255, 255, 255};

    private boolean mousePressedInside = false;
    private boolean enabled = true;

    private OnClickListener onClickListener;

    public Button(String text, TextRenderer textRenderer) {
        this(text, textRenderer, 34f, 16f, CELDA_BOTON_MINIMA_H);
    }

    public Button(String text, TextRenderer textRenderer, float margenH, float margenV) {
        this(text, textRenderer, margenH, margenV, CELDA_BOTON_MINIMA_H);
    }

    /**
     * @param margenH      hueco horizontal a cada lado del rotulo
     * @param margenV      hueco vertical arriba y abajo del rotulo
     * @param anchoMinimo  ancho minimo, para que un boton corto como
     *                     {@code <} no quede en una astilla
     */
    public Button(String text, TextRenderer textRenderer, float margenH, float margenV,
            float anchoMinimo) {
        this.textRenderer = textRenderer;
        this.margenH = margenH;
        this.label = new Label(text, textRenderer);

        TextRenderer.TextMetrics m = textRenderer.measureText(text);
        this.width = Math.max(anchoMinimo, textRenderer.getTextWidth(text) + margenH * 2f);
        this.height = Math.max(CELDA_BOTON_MINIMA_H, m.alto() + margenV * 2f);
    }

    /** Alto minimo de un boton, para que el rotulo no quede apretado. */
    public static final float CELDA_BOTON_MINIMA_H = 52f;

    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int mouseX = mouseVirtualX(windowHandle);
        int mouseY = mouseVirtualY(windowHandle);
        boolean hovered = isInside(mouseX, mouseY);

        if (!enabled) {
            currentColor = disabledColor;
        } else if (mousePressedInside) {
            currentColor = pressedColor;
        } else if (hovered) {
            currentColor = hoverColor;
        } else {
            currentColor = normalColor;
        }

        ensureMesh();
        sharedMesh.draw(x, y, width, height, windowWidth, windowHeight, currentColor, cornerRadius);

        // El rotulo es un Label interno con caja invisible. Se le da el ancho
        // util del boton para que recorte con "..." si no entra, y toda la altura
        // para que quede centrado verticalmente.
        label.setColor(textColor[0] / 255f, textColor[1] / 255f, textColor[2] / 255f);
        label.setBounds(x + margenH, y, Math.max(0f, width - margenH * 2f), height);
        label.dibujar(windowWidth, windowHeight, windowHandle);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    public void handleMouseEvent(long windowHandle) {
        int mouseX = mouseVirtualX(windowHandle);
        int mouseY = mouseVirtualY(windowHandle);

        int leftButtonState = glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_LEFT);
        if (leftButtonState == GLFW_PRESS && isInside(mouseX, mouseY)) {
            mousePressedInside = true;
        } else if (leftButtonState == GLFW_RELEASE) {
            if (mousePressedInside && isInside(mouseX, mouseY)) {
                if (onClickListener != null && enabled) {
                    onClickListener.onClick();
                }
            }
            mousePressedInside = false;
        }
    }

    // Set color con floats (0..1)
    public void setColor(float r, float g, float b, float a) {
        normalColor = rgbaToColor(r, g, b, a);
    }

    public void setHoverColor(float r, float g, float b, float a) {
        hoverColor = rgbaToColor(r, g, b, a);
    }

    public void setPressedColor(float r, float g, float b, float a) {
        pressedColor = rgbaToColor(r, g, b, a);
    }

    public void setDisabledColor(float r, float g, float b, float a) {
        disabledColor = rgbaToColor(r, g, b, a);
    }

    public void setTextColor(float r, float g, float b) {
        textColor = new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            255
        };
    }

    // Set color con int (0..255)
    public void setColorInt(int r, int g, int b, int a) {
        normalColor = new int[]{r, g, b, a};
    }

    public void setHoverColorInt(int r, int g, int b, int a) {
        hoverColor = new int[]{r, g, b, a};
    }

    public void setPressedColorInt(int r, int g, int b, int a) {
        pressedColor = new int[]{r, g, b, a};
    }

    public void setDisabledColorInt(int r, int g, int b, int a) {
        disabledColor = new int[]{r, g, b, a};
    }

    public void setTextColorInt(int r, int g, int b) {
        textColor = new int[]{r, g, b, 255};
    }

    private int[] rgbaToColor(float r, float g, float b, float a) {
        return new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (a * 255)
        };
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.width = ancho;
        this.height = alto;
    }

    @Override
    public float getAncho() {
        return width;
    }

    @Override
    public float getAlto() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /** X en unidades de interfaz, ya calculada con el ultimo resize. */
    public float getX() {
        return x;
    }

    /** Y en unidades de interfaz, ya calculada con el ultimo resize. */
    public float getY() {
        return y;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Píxeles de window por unidad de interfaz: lo usa el clic del ratón. */
    public void setScale(float uiScale) {
        this.uiScale = uiScale <= 0f ? 1f : uiScale;
    }

    public void actualizarPosicion(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    /** X del ratón en unidades de interfaz (el GLFW da píxeles de window). */
    private int mouseVirtualX(long windowHandle) {
        double[] mx = new double[1];
        glfwGetCursorPos(windowHandle, mx, null);
        return (int) (mx[0] / uiScale);
    }

    /** Y del ratón en unidades de interfaz (el GLFW da píxeles de window). */
    private int mouseVirtualY(long windowHandle) {
        double[] my = new double[1];
        glfwGetCursorPos(windowHandle, null, my);
        return (int) (my[0] / uiScale);
    }

    public boolean isInside(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /** Accion que se dispara al soltar el clic dentro del boton. */
    @FunctionalInterface
    public interface OnClickListener {
        void onClick();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private static void ensureMesh() {
        if (sharedMesh == null) {
            sharedMesh = new SharedQuadMesh();
        }
    }

    /** Libera la malla compartida de los botones. */
    public static void cleanupShared() {
        if (sharedMesh != null) {
            sharedMesh.cleanup();
            sharedMesh = null;
        }
    }
}
