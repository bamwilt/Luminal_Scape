package Render2D;

import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;

public class Button {

    private static SharedQuadMesh sharedMesh;

    private float relX, relY;
    private int x, y;
    private int width, height;
    private int windowWidth;
    private int windowHeight;
    /**
     * Píxeles de ventana por unidad de interfaz ({@link UIScale}).
     *
     * <p>El botón se mide y se coloca en unidades virtuales, pero el ratón
     * llega en píxeles reales: sin esta escala el clic se compararía contra
     * un rectángulo virtual y a los botones les faltaría un factor de ancho y
     * alto por el lado que se agranda.
     */
    private float uiScale = 1f;
    private float cornerRadius = 14f;
    private String text;
    private final TextRender textRenderer;

    // Colores RGBA (0-255); el canal A da la transparencia (por defecto algo
    // translúcidos para integrarse mejor con el fondo).
    private int[] normalColor = {26, 38, 56, 200};
    private int[] hoverColor = {60, 90, 130, 215};
    private int[] pressedColor = {90, 130, 180, 235};
    private int[] disabledColor = {80, 80, 80, 150};
    private int[] currentColor = normalColor;
    private int[] textColor = {255, 255, 255, 255};

    private boolean mousePressedInside = false;
    private boolean enabled = true;

    private OnClickListener onClickListener;

    public Button(float relX, float relY, int width, int height, String text, TextRender textRenderer) {
        this.relX = relX;
        this.relY = relY;
        this.width = width;
        this.height = height;
        this.text = text;
        this.textRenderer = textRenderer;
        ensureSharedMesh();
    }

    private static void ensureSharedMesh() {
        if (sharedMesh == null) {
            sharedMesh = new SharedQuadMesh();
        }
    }

    /**
     * Factor de {@link UIScale} de la ventana. Lo llama Main al crear los
     * botones y cada vez que se redimensiona.
     */
    public void setScale(float escala) {
        this.uiScale = escala > 0f ? escala : 1f;
    }

    public void updatePosition(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        x = (int) (relX * windowWidth);
        y = (int) (relY * windowHeight);
    }

    /**
     * Reposiciona el botón en coordenadas relativas (0..1). Si ya se conoce el
     * tamaño de la ventana, actualiza también la posición en píxeles al momento.
     */
    public void setPositionRelative(float newRelX, float newRelY) {
        this.relX = newRelX;
        this.relY = newRelY;
        if (windowWidth > 0 && windowHeight > 0) {
            updatePosition(windowWidth, windowHeight);
        }
    }

    public void draw(int windowWidth, int windowHeight, long windowHandle) {
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

        sharedMesh.draw(x, y, width, height, windowWidth, windowHeight, currentColor, cornerRadius);

        // El texto va centrado en las dos direcciones. Antes se anclaba al
        // borde superior del boton (y + height - alto), asi que el rotulo
        // rozaba el borde de arriba y el hueco de abajo parecia un error de
        // alineacion. Centrado, el margen queda repartido y el boton se lee
        // como un boton.
        float textX = x + (width - textRenderer.getTextWidth(text)) / 2f;
        float textY = y + (height - textRenderer.getTextHeight(text)) / 2f;
        textRenderer.renderer(
                text,
                textX,
                textY,
                textColor[0] / 255f,
                textColor[1] / 255f,
                textColor[2] / 255f
        );

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
        this.text = text;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** X en unidades de interfaz, ya calculada con el ultimo resize. */
    public int getX() {
        return x;
    }

    /** Y en unidades de interfaz, ya calculada con el ultimo resize. */
    public int getY() {
        return y;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** X del ratón en unidades de interfaz (el GLFW da píxeles de ventana). */
    private int mouseVirtualX(long windowHandle) {
        double[] mx = new double[1];
        glfwGetCursorPos(windowHandle, mx, null);
        return (int) (mx[0] / uiScale);
    }

    /** Y del ratón en unidades de interfaz. */
    private int mouseVirtualY(long windowHandle) {
        double[] my = new double[1];
        glfwGetCursorPos(windowHandle, null, my);
        return (int) (my[0] / uiScale);
    }

    public boolean isInside(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @FunctionalInterface
    public interface OnClickListener {

        void onClick();
    }

    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }

    public static void cleanupShared() {
        if (sharedMesh != null) {
            sharedMesh.cleanup();
            sharedMesh = null;
        }
    }
}