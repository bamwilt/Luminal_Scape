package Render3D;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class CollisionManager {

    private Vector3f testPosition;
    private Vector3f testSize;

    private final List<Wall> walls = new ArrayList<>();

    public void setPlayerBounds(Vector3f position, Vector3f size) {
        this.testPosition = new Vector3f(position);
        this.testSize = new Vector3f(size);
    }

    public void addCollision(Wall wall) {
        walls.add(wall);
    }

    public boolean checkCollisions() {
        if (testPosition == null || testSize == null) return false;

        for (Wall wall : walls) {
            if (isColliding(wall)) {
                return true;
            }
        }
        return false;
    }

    private boolean isColliding(Wall wall) {
        Vector3f wallPos = wall.getPosition();
        Vector3f wallSize = wall.getSize();

        Vector3f playerMin = new Vector3f(
                testPosition.x - testSize.x / 2,
                testPosition.y,
                testPosition.z - testSize.z / 2
        );
        Vector3f playerMax = new Vector3f(
                testPosition.x + testSize.x / 2,
                testPosition.y + testSize.y,
                testPosition.z + testSize.z / 2
        );

        Vector3f wallMin = new Vector3f(
                wallPos.x - wallSize.x / 2,
                wallPos.y - wallSize.y / 2,
                wallPos.z - wallSize.z / 2
        );
        Vector3f wallMax = new Vector3f(
                wallPos.x + wallSize.x / 2,
                wallPos.y + wallSize.y / 2,
                wallPos.z + wallSize.z / 2
        );

        return playerMax.x > wallMin.x &&
               playerMin.x < wallMax.x &&
               playerMax.y > wallMin.y &&
               playerMin.y < wallMax.y &&
               playerMax.z > wallMin.z &&
               playerMin.z < wallMax.z;
    }
}
