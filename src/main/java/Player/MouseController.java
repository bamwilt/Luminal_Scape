package Player;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

/**
 * Control de cámara por mouse (FPS estándar con cursor oculto y re-centrado):
 * cada frame se mide el desplazamiento del cursor respecto al centro de la
 * ventana y se vuelve a centrar. De este modo el yaw rota en el plano XZ de
 * forma continua en simultáneo con el movimiento por teclado, sin bloquearse
 * nunca al llegar a un borde. El eje Y queda fijo para el jugador (sin pitch).
 * Al liberar el cursor (menú) vuelve a un cursor normal y visible.
 */
public class MouseController {

    private static final float SENSITIVITY = 0.15f;

    private final Player player;
    private final long windowHandle;

    private boolean captured = false;

    public MouseController(Player player, long windowHandle) {
        this.player = player;
        this.windowHandle = windowHandle;
        setCaptured(true);
    }

    public void update() {
        if (!captured) {
            return;
        }

        double[] xPos = new double[1];
        double[] yPos = new double[1];
        glfwGetCursorPos(windowHandle, xPos, yPos);

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetWindowSize(windowHandle, w, h);
        float cx = w[0] / 2f;
        float cy = h[0] / 2f;

        float dx = (float) xPos[0] - cx;
        float yaw = player.getCamera().getYaw() + dx * SENSITIVITY;
        player.getCamera().setYaw(yaw);

        glfwSetCursorPos(windowHandle, cx, cy);
    }

    public void setCaptured(boolean capture) {
        if (this.captured == capture) {
            return;
        }
        this.captured = capture;
        glfwSetInputMode(windowHandle, GLFW_CURSOR,
                capture ? GLFW_CURSOR_HIDDEN : GLFW_CURSOR_NORMAL);

        if (capture) {
            int[] w = new int[1];
            int[] h = new int[1];
            glfwGetWindowSize(windowHandle, w, h);
            glfwSetCursorPos(windowHandle, w[0] / 2.0, h[0] / 2.0);
        }
    }

    public boolean isCaptured() {
        return captured;
    }
}