package Player;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private final Vector3f position;
    private final Vector3f front;
    private final Vector3f up;
    private float yaw;
    private float pitch; 
    private float speed;
    private Matrix4f viewMatrix = new Matrix4f();

    public Camera() {
        position = new Vector3f(0.0f, 2.5f, 0.0f);
        front = new Vector3f(0.0f, 0.0f, -1.0f);
        up = new Vector3f(0.0f, 1.0f, 0.0f);
        yaw = -90.0f;
        pitch = 0.0f; 
        speed = 0.5f;
        updateViewMatrix();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public void setSpeed(float speed_) {
        speed = 0.5f + speed_;
    }

    public void moveForward(float amount) {
        position.add(new Vector3f(front).mul(amount));
        updateViewMatrix();
    }

    public void moveBackward(float amount) {
        position.sub(new Vector3f(front).mul(amount));
        updateViewMatrix();
    }

    public void moveLeft(float amount) {
        Vector3f left = new Vector3f(front).cross(up).normalize();
        position.add(left.mul(-amount));
        updateViewMatrix();
    }

    public void moveRight(float amount) {
        Vector3f right = new Vector3f(front).cross(up).normalize();
        position.add(right.mul(amount));
        updateViewMatrix();
    }

    public void moveUp(float y) {
        position.y += y;
    }

    public void moveDown(float y) {
        position.y -= y;
    }

    private void updateDirection() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);

        front.x = (float) (Math.cos(radYaw) * Math.cos(radPitch));
        front.y = (float) Math.sin(radPitch);
        front.z = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        front.normalize();
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public Vector3f getFront() {
        return front;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
        updateViewMatrix();
    }

    /** Orienta la cámara hacia un yaw en grados (0 = dirección +X, 90 = +Z). */
    public void setYaw(float yaw) {
        this.yaw = yaw;
        updateDirection();
        updateViewMatrix();
    }

    private void updateViewMatrix() {
        viewMatrix.identity()
                .lookAt(position, new Vector3f(position).add(front), up);
    }
}
