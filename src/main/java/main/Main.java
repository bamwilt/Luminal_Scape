package main;

import MediaUtil.AudioSystem;
import UtilsRender.TextureLoader;
import UtilsRender.Window;
import Render2D.Button;
import Player.Player;
import Player.InputPlayer;
import UtilsRender.Shader;
import Render2D.TextRender;
import Render3D.Background;
import Render3D.CollisionManager;
import Render3D.DungeonManager;
import UtilsRender.Countdown;
import UtilsRender.FpsCounter;
import UtilsRender.TimeUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class Main {

    private static Countdown gameTimer;

    private static Player player;
    private static InputPlayer inputPlayer;
    private static Shader wallShader;
    private static Shader itemShader;
    private static float elapsedTime = 0f;
    private static Window window;
    private static TextRender textRenderer;
    private static CollisionManager collisionManager;
    private static AudioSystem audioSystem;
    private static DungeonManager dungeonManager;
    private static Background background;

    private static Vector3f lightPos = new Vector3f(GameConfig.LIGHT_X, GameConfig.LIGHT_Y, GameConfig.LIGHT_Z);

    private static boolean showMenu = false;
    private static Button buttonMenu;
    private static Button buttonCerrarMenu;
    private static Button buttonRestartTime;
    private static Button buttonMinusFPS;
    private static Button buttonPlusFPS;
    private static Button buttonSalir;

    private static int currentFPS = 0;
    private static final FpsCounter fpsCounter = new FpsCounter();

    public static void main(String[] args) {
        initApplication();
        audioSystem = new AudioSystem();

        gameTimer = new Countdown(GameConfig.TOTAL_TIME_SECONDS);
        gameTimer.start();

        mainLoop();
        cleanup();
    }

    private static void initApplication() {
        window = new Window(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, GameConfig.WINDOW_TITLE);
        window.init();
        window.toggleFullscreen();
        collisionManager = new CollisionManager();

        player = new Player(collisionManager);
        inputPlayer = new InputPlayer(window.getWindowHandle(), player);

        initTextRenderer();
        initShaders();
        background = new Background();
        initDungeon();
        initButtons();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void initTextRenderer() {
        textRenderer = new TextRender(GameConfig.FONT_PATH, GameConfig.FONT_SIZE);
        textRenderer.setProjection(window.getWidth(), window.getHeight());
    }

    private static void initShaders() {
        wallShader = new Shader("shaders/wall_vertex.glsl", "shaders/wall_fragment.glsl");
        itemShader = new Shader("shaders/wall_vertex.glsl", "shaders/item_fragment.glsl");
    }

    private static void initDungeon() {
        int wallTexture = TextureLoader.loadTexture(GameConfig.WALL_TEXTURE);
        int floorTexture = TextureLoader.loadTexture(GameConfig.FLOOR_TEXTURE);
        int ceilingTexture = TextureLoader.loadTexture(GameConfig.CEILING_TEXTURE);

        dungeonManager = new DungeonManager(DungeonManager.sampleLayout(), wallTexture, floorTexture, ceilingTexture);
        dungeonManager.registerCollisions(collisionManager);

        Vector3f spawn = dungeonManager.getSpawnPosition();
        if (spawn != null) {
            player.setPosition(spawn);
        } else {
            player.getCamera().setPosition(new Vector3f(
                    GameConfig.CAMERA_START_X,
                    GameConfig.CAMERA_START_Y,
                    GameConfig.CAMERA_START_Z));
        }
        player.getCamera().setYaw(dungeonManager.getSpawnYaw());
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
            gameTimer = new Countdown(GameConfig.TOTAL_TIME_SECONDS);
            gameTimer.start();
        });

        buttonMinusFPS = new Button(baseX, baseY + 2 * spacingY, 100, 50, "-FPS", textRenderer);
        buttonMinusFPS.setOnClickListener(() -> TimeUtils.setFPS(30));

        buttonPlusFPS = new Button(baseX + 0.18f, baseY + 2 * spacingY, 100, 50, "+FPS", textRenderer);
        buttonPlusFPS.setOnClickListener(() -> TimeUtils.setFPS(60));

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
        TimeUtils.setFPS(GameConfig.TARGET_FPS);
        window.loop(() -> {
            TimeUtils.update();
            fpsCounter.update(TimeUtils.getDeltaTime());
            currentFPS = fpsCounter.getCurrentFPS();

            float deltaTime = TimeUtils.getDeltaTime();
            elapsedTime += deltaTime;
            // Los pasos solo suenan mientras la partida está activa (paso velocidad 0 si terminó)
            float playerSpeed = gameTimer.isFinished() ? 0f : player.getVelocity().length();
            audioSystem.update(deltaTime, playerSpeed);

            clearScreen();
            handleInput();

            if (!gameTimer.isFinished()) {
                inputPlayer.update(deltaTime);
                if (dungeonManager.update(player.getPosition(), deltaTime)) {
                    audioSystem.playPicked();
                }
            }

            render3DScene();
            renderUI();
        });
    }

    private static void clearScreen() {
        GL11.glClearColor(GameConfig.CLEAR_R, GameConfig.CLEAR_G, GameConfig.CLEAR_B, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static void render3DScene() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(GameConfig.FOV_DEGREES),
                    (float) window.getWidth() / window.getHeight(),
                    GameConfig.NEAR_PLANE,
                    GameConfig.FAR_PLANE);

            Matrix4f view = player.getCamera().getViewMatrix();

            background.render(elapsedTime, window.getWidth(), window.getHeight());

            wallShader.use();
            wallShader.setMat4("projection", projection);
            wallShader.setMat4("view", view);

            wallShader.setVec3("lightPos", lightPos);
            wallShader.setVec3("lightColor", GameConfig.LIGHT_R, GameConfig.LIGHT_G, GameConfig.LIGHT_B);
            wallShader.setVec3("viewPos", player.getCamera().getPosition());
            wallShader.setFloat("emissionStrength", GameConfig.EMISSION_STRENGTH);
            wallShader.setFloat("glowIntensity", GameConfig.GLOW_INTENSITY);

            dungeonManager.render(wallShader);

            itemShader.use();
            itemShader.setMat4("projection", projection);
            itemShader.setMat4("view", view);
            itemShader.setVec3("lightPos", lightPos);
            itemShader.setVec3("lightColor", GameConfig.LIGHT_R, GameConfig.LIGHT_G, GameConfig.LIGHT_B);
            itemShader.setVec3("viewPos", player.getCamera().getPosition());
            dungeonManager.renderItems(itemShader, elapsedTime);
        }
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

            textRenderer.rendererRelativo("Habitacion: Dungeon", 0.01f, 0.85f, 1.0f, 1.0f, 1.0f);
            textRenderer.rendererRelativo("FPS: " + currentFPS, 0.88f, 0.02f, 0.3f, 1.0f, 0.3f);

            float x = 0.75f;
            float y = 0.85f;
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
        dungeonManager.cleanup();
        background.cleanup();
        wallShader.cleanup();
        itemShader.cleanup();
        window.cleanup();
        audioSystem.cleanup();
    }

    private static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}