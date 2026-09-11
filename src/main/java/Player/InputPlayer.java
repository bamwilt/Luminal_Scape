package Player;

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

        player.update(deltaTime, forward, backward, leftMove, rightMove);
    }
}
