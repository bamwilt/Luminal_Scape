package Player;

import Core.AjustesJuego;

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
 * window y se vuelve a centrar. De este modo el yaw rota en el plano XZ de
 * forma continua en simultáneo con el movimiento por teclado, sin bloquearse
 * nunca al llegar a un borde. El eje Y queda fijo para el player (sin pitch).
 * Al liberar el cursor (menú) vuelve a un cursor normal y visible.
 */
public class MouseController {

    /**
     * Sensibilidad actual: grados de yaw por pixel de desplazamiento leido.
     *
     * <p>Es mutable porque la pantalla de Configuracion la cambia en caliente
     * con {@link #setSensitivity}. El valor de partida es el mismo que el del
     * slider ({@link AjustesJuego#SENSIBILIDAD_DEFECTO}), para que la pantalla
     * enseñe al abrirse lo que el juego esta usando.
     */
    private float sensitivity = AjustesJuego.SENSIBILIDAD_DEFECTO;

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
        float yaw = player.getCamara().getYaw() + dx * sensitivity;
        player.getCamara().setYaw(yaw);

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

    /**
     * Fija la sensibilidad del raton (grados de yaw por pixel).
     *
     * <p>Se acota al rango de {@link AjustesJuego} para que ni el slider ni
     * nadie pueda dejar la camara quieta o inmanejable.
     */
    public void setSensitivity(float valor) {
        this.sensitivity = Math.max(AjustesJuego.SENSIBILIDAD_MIN,
                Math.min(AjustesJuego.SENSIBILIDAD_MAX, valor));
    }

    /** Sensibilidad actual, en grados de yaw por pixel. */
    public float getSensitivity() {
        return sensitivity;
    }
}