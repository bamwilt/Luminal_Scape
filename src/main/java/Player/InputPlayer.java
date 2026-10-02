package Player;

import Render3D.map.MapConfig;

import static org.lwjgl.glfw.GLFW.*;

public class InputPlayer {

    private final long windowHandle;
    private final Player player;

    public InputPlayer(long windowHandle, Player player) {
        this.windowHandle = windowHandle;
        this.player = player;
    }

    public void update(float deltaTime) {
        boolean forward = glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS;
        boolean backward = glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS;
        boolean leftMove = glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS;
        boolean rightMove = glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS;

        boolean running = glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
        player.setRunning(running);

        boolean crouching = glfwGetKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        player.setCrouching(crouching);

        // Q y E giran la cámara. Se leen aquí, y no en un controlador aparte,
        // porque el yaw lo comparte el ratón: si cada uno llevara su propio
        // ángulo los dos se pisarían y el giro del ratón saltaría.
        rotateFromKeys(deltaTime);

        player.update(deltaTime, forward, backward, leftMove, rightMove);
    }

    /**
     * Giro de cámara con Q (izquierda) y E (derecha).
     *
     * <p>La velocidad sale de {@link MapConfig#KEY_YAW_SPEED} en grados por
     * segundo y se multiplica por el frame, así que el giro va igual de rápido
     * vaya el juego a 30 o a 60 FPS. Si no se pulsara ninguna de las dos, los
     * grados son cero y no se toca la cámara.
     */
    private void rotateFromKeys(float deltaTime) {
        float direccion = 0f;
        if (glfwGetKey(windowHandle, GLFW_KEY_Q) == GLFW_PRESS) {
            direccion -= 1f;
        }
        if (glfwGetKey(windowHandle, GLFW_KEY_E) == GLFW_PRESS) {
            direccion += 1f;
        }
        if (direccion == 0f) {
            return;
        }
        player.getCamera().rotateYaw(direccion * MapConfig.KEY_YAW_SPEED * deltaTime);
    }
}
