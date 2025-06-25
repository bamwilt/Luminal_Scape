package Render2D;

import UtilsRender.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class Button {

    private static int sharedVAO = -1;
    private static int sharedVBO = -1;
    private static Shader sharedShader;

    private float relX, relY;
    private int x, y;
    private int width, height;
    private String text;
    private final TextRender textRenderer;

    // Colores RGBA (0-255)
    private int[] normalColor = {26, 38, 56, 255};
    private int[] hoverColor = {60, 90, 130, 255};
    private int[] pressedColor = {90, 130, 180, 255};
    private int[] disabledColor = {80, 80, 80, 255};
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
        initSharedResources();
    }

    private void initSharedResources() {
        if (sharedVAO != -1) {
            return;
        }

        sharedVAO = GL30.glGenVertexArrays();
        sharedVBO = glGenBuffers();

        GL30.glBindVertexArray(sharedVAO);
        glBindBuffer(GL_ARRAY_BUFFER, sharedVBO);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL30.glBindVertexArray(0);

        sharedShader = new Shader("shaders/button_vertex.glsl", "shaders/button_frag.glsl");
    }

    public void updatePosition(int windowWidth, int windowHeight) {
        x = (int) (relX * windowWidth);
        y = (int) (relY * windowHeight);
    }

    public void draw(int windowWidth, int windowHeight, long windowHandle) {
        // Configurar para renderizado transparente
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(windowHandle, mx, my);
        int mouseX = (int) mx[0];
        int mouseY = (int) my[0];
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

        float[] vertices = new float[]{
            x, y,
            x + width, y,
            x + width, y + height,
            x, y,
            x + width, y + height,
            x, y + height
        };

        GL30.glBindVertexArray(sharedVAO);
        glBindBuffer(GL_ARRAY_BUFFER, sharedVBO);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);

        sharedShader.use();
        sharedShader.setFloat("screenWidth", windowWidth);
        sharedShader.setFloat("screenHeight", windowHeight);
        sharedShader.setVec4("buttonColor",
                currentColor[0] / 255f,
                currentColor[1] / 255f,
                currentColor[2] / 255f,
                currentColor[3] / 255f
        );

        glDrawArrays(GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        // Dibuja texto centrado con RGB
        textRenderer.renderer(
                text,
                x + (width * 0.5f) - (textRenderer.getTextWidth(text) / 2f),
                y + height - (textRenderer.getTextHeight(text)),
                textColor[0] / 255f,
                textColor[1] / 255f,
                textColor[2] / 255f
        );

        // Restaurar configuraciones
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    public void handleMouseEvent(long windowHandle) {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(windowHandle, mx, my);
        int mouseX = (int) mx[0];
        int mouseY = (int) my[0];

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
        normalColor = new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (a * 255)
        };
    }

    public void setHoverColor(float r, float g, float b, float a) {
        hoverColor = new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (a * 255)
        };
    }

    public void setPressedColor(float r, float g, float b, float a) {
        pressedColor = new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (a * 255)
        };
    }

    public void setDisabledColor(float r, float g, float b, float a) {
        disabledColor = new int[]{
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (a * 255)
        };
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

    public void setText(String text) {
        this.text = text;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        GL30.glDeleteVertexArrays(sharedVAO);
        glDeleteBuffers(sharedVBO);
        sharedShader.cleanup();
    }
}
