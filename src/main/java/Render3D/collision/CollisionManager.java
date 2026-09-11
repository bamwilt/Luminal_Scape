package Render3D.collision;

import Render3D.graphics.Wall;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class CollisionManager {

    private Vector3f playerPosition;
    private Vector3f playerSize;

    private final List<Wall> walls = new ArrayList<>();
    private final List<Wall> railingWalls = new ArrayList<>();

    public void setPlayerBounds(Vector3f position, Vector3f size) {
        this.playerPosition = new Vector3f(position);
        this.playerSize = new Vector3f(size);
    }

    public void addCollision(Wall wall) {
        walls.add(wall);
    }

    /** Colisiones de raillings, en una lista aparte de los muros. */
    public void addRailingCollision(Wall wall) {
        railingWalls.add(wall);
    }

    /** Libera todas las colisiones registradas (desecha muros ya borrados en GL). */
    public void clear() {
        walls.clear();
        railingWalls.clear();
    }

    public boolean checkCollisions() {
        if (playerPosition == null || playerSize == null) return false;

        for (Wall wall : walls) {
            if (isColliding(wall)) {
                return true;
            }
        }
        for (Wall wall : railingWalls) {
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
                playerPosition.x - playerSize.x / 2,
                playerPosition.y,
                playerPosition.z - playerSize.z / 2
        );
        Vector3f playerMax = new Vector3f(
                playerPosition.x + playerSize.x / 2,
                playerPosition.y + playerSize.y,
                playerPosition.z + playerSize.z / 2
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
