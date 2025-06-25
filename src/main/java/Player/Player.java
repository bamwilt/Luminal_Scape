package Player;

import org.joml.Vector3f;
import Render3D.CollisionManager;

public class Player {

    private final Camera camera;
    private final CollisionManager collisionManager;

    private boolean isRunning = false;
    private boolean isCrouching = false;

    private final float walkSpeed = 20.0f;
    private final float runSpeed = 40.0f;

    private final float crouchOffset = 1.0f;
    private final float playerWidth = 1.0f;
    private final float playerDepth = 1.0f;
    private final float playerStandingHeight = 1.8f;
    private final float crouchSpeed = 5.0f; 

    private float baseHeight;
    private Vector3f lastPosition;

    public Player(CollisionManager collisionManager) {
        this.camera = new Camera();
        this.collisionManager = collisionManager;
        this.baseHeight = camera.getPosition().y;
        this.lastPosition = new Vector3f(camera.getPosition());
    }

    public void update(float deltaTime, boolean forward, boolean backward, boolean left, boolean right) {
        Vector3f previousPos = new Vector3f(camera.getPosition());

        float currentSpeed = isRunning ? runSpeed : walkSpeed;
        float movementAmount = currentSpeed * deltaTime;

        if (forward) {
            camera.moveForward(movementAmount);
        }
        if (backward) {
            camera.moveBackward(movementAmount);
        }
        if (left) {
            camera.moveLeft(movementAmount);
        }
        if (right) {
            camera.moveRight(movementAmount);
        }

        float targetHeight = isCrouching ? (playerStandingHeight - crouchOffset) : playerStandingHeight;
        Vector3f pos = camera.getPosition();

        if (isCrouching) {
            pos.y = lerp(pos.y, baseHeight - crouchOffset, crouchSpeed * deltaTime);
        } else {
            pos.y = baseHeight;
        }

        camera.setPosition(pos);

        Vector3f size = new Vector3f(playerWidth, targetHeight, playerDepth);
        collisionManager.setPlayerBounds(camera.getPosition(), size);

        if (collisionManager.checkCollisions()) {
            camera.setPosition(previousPos);
        }

        lastPosition.set(previousPos);
    }

    private float lerp(float start, float end, float t) {
        if (t > 1.0f) {
            t = 1.0f;
        }
        return start + t * (end - start);
    }

    public void rotateLeft() {
        camera.turnLeft();
    }

    public void rotateRight() {
        camera.turnRight();
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public void setCrouching(boolean crouching) {
        this.isCrouching = crouching;
    }

    public void setPosition(Vector3f position) {
        camera.setPosition(position);
        this.baseHeight = position.y;
        this.lastPosition.set(position);
    }

    public void setBaseHeight(float height) {
        this.baseHeight = height;
        if (!isCrouching) {
            Vector3f pos = camera.getPosition();
            pos.y = baseHeight;
            camera.setPosition(pos);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public Vector3f getPosition() {
        return camera.getPosition();
    }

    public Vector3f getVelocity() {
        Vector3f currentPosition = getPosition();
        Vector3f velocity = new Vector3f(currentPosition).sub(lastPosition);
        return velocity;
    }
} 
