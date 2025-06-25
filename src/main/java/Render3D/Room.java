package Render3D;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import UtilsRender.Shader;

public class Room {

    private Wall floor;
    private Wall ceiling;
    private List<Wall> walls;
    private List<Wall> doorFrames;
    private List<Wall> doorSideWalls;

    private Vector3f centerPosition;
    private float width, height, depth;
    private String name;

    private static final float DOOR_WIDTH = 3.0f;
    private static final float DOOR_HEIGHT = 2.5f;
    private static final float WALL_THICKNESS = 0.4f;

    public Room(float width, float height, float depth, Vector3f centerPosition,
            int wallTexture, int floorTexture, int ceilingTexture) {
        this.centerPosition = new Vector3f(centerPosition);
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.name = "Room";

        createFloor(floorTexture);
        createCeiling(ceilingTexture);
        createWalls(wallTexture);
    }

    private void createFloor(int floorTexture) {
        floor = new Wall(width, 0.1f, depth, floorTexture, true);
        floor.setPosition(centerPosition.x, centerPosition.y - height / 2, centerPosition.z);
    }

    private void createCeiling(int ceilingTexture) {
        ceiling = new Wall(width, 0.1f, depth, ceilingTexture, true);
        ceiling.setPosition(centerPosition.x, centerPosition.y + height / 2, centerPosition.z);
    }

    private void createWalls(int wallTexture) {
        walls = new ArrayList<>();
        doorFrames = new ArrayList<>();
        doorSideWalls = new ArrayList<>();

        createWallNorth(wallTexture);
        createWallSouth(wallTexture);
        createWallEast(wallTexture);
        createWallWest(wallTexture);
    }

    private void createWallNorth(int texture) {
        Wall north = new Wall(width, height, WALL_THICKNESS, texture, true);
        north.setPosition(centerPosition.x, centerPosition.y, centerPosition.z - depth / 2);
        north.setTexture(texture, 4.0f, 4.0f, true);
        walls.add(north);
    }

    private void createWallSouth(int texture) {
        Wall south = new Wall(width, height, WALL_THICKNESS, texture, true);
        south.setPosition(centerPosition.x, centerPosition.y, centerPosition.z + depth / 2);
        south.setTexture(texture, 4.0f, 4.0f, true);
        walls.add(south);
    }

    private void createWallEast(int texture) {
        Wall east = new Wall(WALL_THICKNESS, height, depth, texture, true);
        east.setPosition(centerPosition.x + width / 2, centerPosition.y, centerPosition.z);
        east.setTexture(texture, 4.0f, 4.0f, true);
        walls.add(east);
    }

    private void createWallWest(int texture) {
        Wall west = new Wall(WALL_THICKNESS, height, depth, texture, true);
        west.setPosition(centerPosition.x - width / 2, centerPosition.y, centerPosition.z);
        west.setTexture(texture, 4.0f, 4.0f, true);
        walls.add(west);
    }

    public void setDoor(String side) {
        setDoor(side, 0.5f, 1.5f);
    }

    public void setDoor(String side, float relativePosition) {
        setDoor(side, relativePosition, 1.5f);
    }

    public void setDoor(String side, float relativePosition, float frameBottomOffset) {
        int wallIndex = getWallIndex(side);
        if (wallIndex == -1) {
            return;
        }

        Wall wall = walls.get(wallIndex);
        if (wall == null) {
            return;
        }

        Vector3f pos = wall.getPosition();
        Vector3f size = wall.getSize();
        int textureID = wall.getTextureID();

        float doorX = calculateDoorX(side, pos, size, relativePosition);
        float doorZ = calculateDoorZ(side, pos, size, relativePosition);

        float frameHeight = DOOR_HEIGHT + Math.abs(frameBottomOffset);
        float frameCenterY = pos.y + (frameBottomOffset / 2f) + (frameHeight / 2f);

        addDoorTopFrame(side, doorX, doorZ, frameCenterY, frameHeight, textureID);
        addDoorSideWalls(side, relativePosition, pos, size, textureID);

        walls.set(wallIndex, null);
    }

    private void addDoorTopFrame(String side, float doorX, float doorZ, float frameCenterY, float frameHeight, int textureID) {
        Wall topFrame = new Wall(DOOR_WIDTH, frameHeight, WALL_THICKNESS, textureID, true);

        float rotationY = (side.equalsIgnoreCase("east") || side.equalsIgnoreCase("west"))
                ? (float) Math.toRadians(90) : 0f;

        topFrame.setRotation(0, rotationY, 0);
        topFrame.setPosition(doorX, frameCenterY, doorZ);
        topFrame.setTexture(textureID, 2.0f, 2.0f, true);
        doorFrames.add(topFrame);
    }

    private int getWallIndex(String side) {
        switch (side.toLowerCase()) {
            case "north":
                return 0;
            case "south":
                return 1;
            case "east":
                return 2;
            case "west":
                return 3;
            default:
                return -1;
        }
    }

    private float calculateDoorX(String side, Vector3f pos, Vector3f size, float relPos) {
        if (side.equalsIgnoreCase("north") || side.equalsIgnoreCase("south")) {
            return pos.x - size.x / 2 + size.x * relPos;
        }
        return pos.x;
    }

    private float calculateDoorZ(String side, Vector3f pos, Vector3f size, float relPos) {
        if (side.equalsIgnoreCase("east") || side.equalsIgnoreCase("west")) {
            return pos.z - size.z / 2 + size.z * relPos;
        }
        return pos.z;
    }

    private void addDoorTopFrame(String side, float doorX, float doorZ, float topY, int textureID) {
        Wall topFrame = new Wall(DOOR_WIDTH, DOOR_HEIGHT, WALL_THICKNESS, textureID, true);

        float rotationY = (side.equalsIgnoreCase("east") || side.equalsIgnoreCase("west"))
                ? (float) Math.toRadians(90) : 0f;

        topFrame.setRotation(0, rotationY, 0);
        topFrame.setPosition(doorX, topY, doorZ);
        topFrame.setTexture(textureID, 2.0f, 2.0f, true);
        doorFrames.add(topFrame);
    }

    private void addDoorSideWalls(String side, float relativePosition, Vector3f pos, Vector3f size, int textureID) {
        if (side.equalsIgnoreCase("north") || side.equalsIgnoreCase("south")) {
            addSideWallsHorizontal(pos, size, relativePosition, textureID);
        } else {
            addSideWallsVertical(pos, size, relativePosition, textureID);
        }
    }

    private void addSideWallsHorizontal(Vector3f pos, Vector3f size, float relativePosition, int textureID) {
        float leftWidth = size.x * relativePosition - DOOR_WIDTH / 2;
        float rightWidth = size.x - leftWidth - DOOR_WIDTH;

        if (leftWidth > 0) {
            Wall leftSegment = new Wall(leftWidth, height, WALL_THICKNESS, textureID, true);
            leftSegment.setPosition(pos.x - size.x / 2 + leftWidth / 2, pos.y, pos.z);
            leftSegment.setTexture(textureID, 4.0f, 4.0f, true);
            doorSideWalls.add(leftSegment);
        }

        if (rightWidth > 0) {
            Wall rightSegment = new Wall(rightWidth, height, WALL_THICKNESS, textureID, true);
            rightSegment.setPosition(pos.x + size.x / 2 - rightWidth / 2, pos.y, pos.z);
            rightSegment.setTexture(textureID, 4.0f, 4.0f, true);
            doorSideWalls.add(rightSegment);
        }
    }

    private void addSideWallsVertical(Vector3f pos, Vector3f size, float relativePosition, int textureID) {
        float leftDepth = size.z * relativePosition - DOOR_WIDTH / 2;
        float rightDepth = size.z - leftDepth - DOOR_WIDTH;

        if (leftDepth > 0) {
            Wall leftSegment = new Wall(WALL_THICKNESS, height, leftDepth, textureID, true);
            leftSegment.setPosition(pos.x, pos.y, pos.z - size.z / 2 + leftDepth / 2);
            leftSegment.setTexture(textureID, 4.0f, 4.0f, true);
            doorSideWalls.add(leftSegment);
        }

        if (rightDepth > 0) {
            Wall rightSegment = new Wall(WALL_THICKNESS, height, rightDepth, textureID, true);
            rightSegment.setPosition(pos.x, pos.y, pos.z + size.z / 2 - rightDepth / 2);
            rightSegment.setTexture(textureID, 4.0f, 4.0f, true);
            doorSideWalls.add(rightSegment);
        }
    }

    // Getters
    public Wall getFloor() {
        return floor;
    }

    public Wall getCeiling() {
        return ceiling;
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public List<Wall> getDoorFrames() {
        return doorFrames;
    }

    public List<Wall> getDoorSideWalls() {
        return doorSideWalls;
    }

    public List<Wall> getAllCollidableComponents() {
        List<Wall> collidables = new ArrayList<>();
        for (Wall wall : walls) {
            if (wall != null) {
                collidables.add(wall);
            }
        }
        collidables.addAll(doorFrames);
        collidables.addAll(doorSideWalls);
        return collidables;
    }

    public void registerCollisions(CollisionManager collisionManager) {
        for (Wall wall : getAllCollidableComponents()) {
            collisionManager.addCollision(wall);
        }
    }

    public void render(Shader shader) {
        floor.render(shader);
        ceiling.render(shader);
        for (Wall wall : walls) {
            if (wall != null) {
                wall.render(shader);
            }
        }
        for (Wall frame : doorFrames) {
            frame.render(shader);
        }
        for (Wall sideWall : doorSideWalls) {
            sideWall.render(shader);
        }
    }

    public void cleanup() {
        floor.cleanup();
        ceiling.cleanup();
        for (Wall wall : walls) {
            if (wall != null) {
                wall.cleanup();
            }
        }
        for (Wall frame : doorFrames) {
            frame.cleanup();
        }
        for (Wall sideWall : doorSideWalls) {
            sideWall.cleanup();
        }
    }

    public boolean contains(Vector3f point) {
        float minX = centerPosition.x - width / 2;
        float maxX = centerPosition.x + width / 2;
        float minY = centerPosition.y - height / 2;
        float maxY = centerPosition.y + height / 2;
        float minZ = centerPosition.z - depth / 2;
        float maxZ = centerPosition.z + depth / 2;

        return point.x >= minX && point.x <= maxX
                && point.y >= minY && point.y <= maxY
                && point.z >= minZ && point.z <= maxZ;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
