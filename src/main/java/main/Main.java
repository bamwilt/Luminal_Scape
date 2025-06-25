package main;

import MediaUtil.SoundLoader;
import MediaUtil.SoundManager;
import UtilsRender.TextureLoader;
import UtilsRender.Window;
import Render2D.Button;
import Player.Player;
import Player.InputPlayer;
import UtilsRender.Shader;
import Render2D.TextRender;
import Render3D.Room;
import Render3D.CollisionManager;
import UtilsRender.TimeUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;

public class Main {

    private static int TOTAL_TIME_SECONDS = 5 * 60;
    private static TimeUtils.Countdown gameTimer;

    private static Player player;
    private static InputPlayer inputPlayer;
    private static Shader wallShader;
    private static Window window;
    private static TextRender textRenderer;
    private static CollisionManager collisionManager;

    private static Vector3f lightPos = new Vector3f(0.0f, 5.0f, 0.0f);
    private static List<Room> rooms = new ArrayList<>();
    private static SoundManager soundManager;

    private static float stepCooldown = 0.4f;
    private static float timeSinceLastStep = 0f;

    private static boolean showMenu = false;
    private static Button buttonMenu;
    private static Button buttonCerrarMenu;
    private static Button buttonRestartTime;
    private static Button buttonMinusFPS;
    private static Button buttonPlusFPS;
    private static Button buttonSalir;

    private static int currentFPS = 0;
    private static int frames = 0;
    private static float fpsTimer = 0f;

    public static void main(String[] args) {
        initApplication();
        initMusic();
        gameTimer = new TimeUtils.Countdown(TOTAL_TIME_SECONDS);
        gameTimer.start();
        mainLoop();
        cleanup();
    }

    private static void initMusic() {
        soundManager = SoundManager.getInstance();
        SoundLoader.loadAllSounds(soundManager);
        soundManager.playMusic("ambient", 0.2f);
    }

    private static void initApplication() {
        window = new Window(800, 600, "Luminal Scape");
        window.init();
        window.toggleFullscreen();
        collisionManager = new CollisionManager();

        player = new Player(collisionManager);
        inputPlayer = new InputPlayer(window.getWindowHandle(), player);

        initTextRenderer();
        initShaders();
        initRooms();
        initButtons();

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        player.getCamera().setPosition(new Vector3f(0.0f, 3.0f, 3.0f));
    }

    private static void initTextRenderer() {
        textRenderer = new TextRender("fonts/Roboto-Bold.ttf", 28);
        textRenderer.setProjection(window.getWidth(), window.getHeight());
    }

    private static void initShaders() {
        wallShader = new Shader("shaders/wall_vertex.glsl", "shaders/wall_fragment.glsl");
    }

    private static void initRooms() {
        int wallTexture = TextureLoader.loadTexture("textures/backWall.jpg");
        int floorTexture = TextureLoader.loadTexture("textures/Floor.jpg");
        int ceilingTexture = TextureLoader.loadTexture("textures/Floor2.jpg");

        float roomWidth = 20.0f;
        float roomHeight = 8.0f;
        float roomDepth = 20.0f;
        float spacing = 20.0f;

        for (int i = 0; i < 5; i++) {
            Vector3f pos = new Vector3f(0.0f, 4.0f, 0.0f);
            Room room;

            switch (i) {
                case 0:
                    room = new Room(roomWidth, roomHeight, roomDepth, pos, wallTexture, floorTexture, ceilingTexture);
                    room.setDoor("north", 0.5f, -3.0f);
                    room.setDoor("south");
                    room.setDoor("east", 0.5f);
                    room.setDoor("west", 0.5f);
                    room.setName("Central");
                    break;
                case 1:
                    pos.x += spacing;
                    room = new Room(roomWidth, roomHeight, roomDepth, pos, wallTexture, floorTexture, ceilingTexture);
                    room.setDoor("west", 0.5f);
                    room.setName("Este");
                    break;
                case 2:
                    pos.x -= spacing;
                    room = new Room(roomWidth, roomHeight, roomDepth, pos, wallTexture, floorTexture, ceilingTexture);
                    room.setDoor("east", 0.5f);
                    room.setName("Oeste");
                    break;
                case 3:
                    pos.z -= spacing;
                    room = new Room(roomWidth, roomHeight, roomDepth, pos, wallTexture, floorTexture, ceilingTexture);
                    room.setDoor("south", 0.5f);
                    room.setName("Sur");
                    break;
                case 4:
                    pos.z += spacing;
                    room = new Room(roomWidth, roomHeight, roomDepth, pos, wallTexture, floorTexture, ceilingTexture);
                    room.setDoor("north", 0.5f);
                    room.setName("Norte");
                    break;
                default:
                    continue;
            }

            rooms.add(room);
            room.registerCollisions(collisionManager);
        }
    }

    private static void initButtons() {
        buttonMenu = new Button(0.02f, 0.02f, 150, 50, "Menu", textRenderer);
        buttonMenu.setOnClickListener(() -> showMenu = !showMenu);

        float baseX = 0.02f;
        float baseY = 0.1f;
        float spacingY = 0.11f;

        buttonCerrarMenu = new Button(baseX, baseY + 0 * spacingY, 150, 50, "Cerrar Menu", textRenderer);
        buttonCerrarMenu.setOnClickListener(() -> showMenu = false);

        buttonRestartTime = new Button(baseX, baseY + 1 * spacingY, 200, 50, "Reiniciar Tiempo", textRenderer);
        buttonRestartTime.setOnClickListener(() -> {
            gameTimer = new TimeUtils.Countdown(TOTAL_TIME_SECONDS);
            gameTimer.start();
        });

            buttonMinusFPS = new Button(baseX, baseY + 2 * spacingY, 100, 50, "-FPS", textRenderer);
        buttonMinusFPS.setOnClickListener(() -> {
            TimeUtils.setFPS(30);
        });

        buttonPlusFPS = new Button(baseX + 0.18f, baseY + 2 * spacingY, 100, 50, "+FPS", textRenderer);
        buttonPlusFPS.setOnClickListener(() -> {
            TimeUtils.setFPS(60);
        });

        buttonSalir = new Button(baseX, baseY + 3 * spacingY, 150, 50, "Salir", textRenderer);
        buttonSalir.setOnClickListener(() -> System.exit(0));

        int w = window.getWidth();
        int h = window.getHeight();
        buttonMenu.updatePosition(w, h);
        buttonCerrarMenu.updatePosition(w, h);
        buttonRestartTime.updatePosition(w, h);
        buttonMinusFPS.updatePosition(w, h);
        buttonPlusFPS.updatePosition(w, h);
        buttonSalir.updatePosition(w, h);

        window.setOnResizeCallback((w2, h2) -> {
            textRenderer.setProjection(w2, h2);
            buttonMenu.updatePosition(w2, h2);
            buttonCerrarMenu.updatePosition(w2, h2);
            buttonRestartTime.updatePosition(w2, h2);
            buttonMinusFPS.updatePosition(w2, h2);
            buttonPlusFPS.updatePosition(w2, h2);
            buttonSalir.updatePosition(w2, h2);
        });
    }

    private static void handleInput() {
        buttonMenu.handleMouseEvent(window.getWindowHandle());
        if (showMenu) {
            buttonCerrarMenu.handleMouseEvent(window.getWindowHandle());
            buttonRestartTime.handleMouseEvent(window.getWindowHandle());
            buttonMinusFPS.handleMouseEvent(window.getWindowHandle());
            buttonPlusFPS.handleMouseEvent(window.getWindowHandle());
            buttonSalir.handleMouseEvent(window.getWindowHandle());
        }
    }

    private static void mainLoop() {
        TimeUtils.setFPS(60);
        window.loop(() -> {
            TimeUtils.update();
            soundManager.update();
            clearScreen();
            handleInput();

            if (!gameTimer.isFinished()) {
                inputPlayer.update(TimeUtils.getDeltaTime());
                handleFootstepSound(TimeUtils.getDeltaTime());
            }

            frames++;
            fpsTimer += TimeUtils.getDeltaTime();
            if (fpsTimer >= 1.0f) {
                currentFPS = frames;
                frames = 0;
                fpsTimer = 0f;
            }

            render3DScene();
            renderUI();
        });
    }

    private static void handleFootstepSound(float deltaTime) {
        timeSinceLastStep += deltaTime;
        Vector3f velocity = player.getVelocity();
        float speed = velocity.length();
        if (speed > 0.1f && timeSinceLastStep >= stepCooldown) {
            float pitch = 0.9f + (float) Math.random() * 0.2f;
            soundManager.playSound("step", 1.0f, pitch, false);

            int error = alGetError();
            if (error != AL_NO_ERROR) {
                System.err.println("OpenAL error al reproducir paso: " + error);
            }

            timeSinceLastStep = 0f;
        }
    }

    private static void clearScreen() {
        GL11.glClearColor(0.08f, 0.08f, 0.06f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static void render3DScene() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(45.0f),
                    (float) window.getWidth() / window.getHeight(),
                    0.1f,
                    100.0f);

            Matrix4f view = player.getCamera().getViewMatrix();

            wallShader.use();
            wallShader.setMat4("projection", projection);
            wallShader.setMat4("view", view);

            wallShader.setVec3("lightPos", lightPos);
            wallShader.setVec3("lightColor", 0.3f, 0.3f, 0.1f);
            wallShader.setVec3("viewPos", player.getCamera().getPosition());
            wallShader.setFloat("emissionStrength", 0.2f);
            wallShader.setFloat("glowIntensity", 0.2f);

            for (Room room : rooms) {
                room.render(wallShader);
            }
        }
    }

    private static Room getCurrentRoom(Vector3f position) {
        for (Room room : rooms) {
            if (room.contains(position)) {
                return room;
            }
        }
        return null;
    }

    private static void renderUI() {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        buttonMenu.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());

        if (showMenu) {
            buttonCerrarMenu.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());
            buttonRestartTime.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());
            buttonMinusFPS.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());
            buttonPlusFPS.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());
            buttonSalir.draw(window.getWidth(), window.getHeight(), window.getWindowHandle());

            Room currentRoom = getCurrentRoom(player.getPosition());
            String roomName = "Habitacion: " + (currentRoom != null ? currentRoom.getName() : "Ninguna");
            textRenderer.rendererRelativo(roomName, 0.01f, 0.85f, 1.0f, 1.0f, 1.0f);
            textRenderer.rendererRelativo("FPS: " + currentFPS, 0.88f, 0.02f, 0.3f, 1.0f, 0.3f);

            float x = 0.75f;
            float y = 0.85f;
            float dy = 0.035f;
            textRenderer.rendererRelativo("Controles: \nWASD - Moverse\nQ / E - Rotar cam\nCtrl Izq - agacharse\n Shif - Correr", x, y, 1f, 1f, 1f);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

        if (!gameTimer.isFinished()) {
            float remainingSeconds = gameTimer.getRemainingSeconds();
            String timeDisplay = formatTime((int) remainingSeconds);
            textRenderer.rendererRelativo("TIME: " + timeDisplay, 0.02f, 0.98f, 1.0f, 0.3f, 0.3f);
        } else {
            textRenderer.rendererRelativo("TIME: END", 0.02f, 0.98f, 1.0f, 0.1f, 0.1f);
        }
    }

    private static void cleanup() {
        for (Room room : rooms) {
            room.cleanup();
        }
        wallShader.cleanup();
        window.cleanup();
        soundManager.cleanup();
    }

    private static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
