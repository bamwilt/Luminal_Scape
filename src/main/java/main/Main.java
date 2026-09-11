package main;

import MediaUtil.AudioSystem;
import UtilsRender.TextureLoader;
import UtilsRender.Window;
import Render2D.Button;
import Render2D.Minimap;
import Render2D.QuadBatch;
import Render2D.TextRender;
import Render2D.VisionManager;
import Player.Player;
import Player.InputPlayer;
import Player.MouseController;
import UtilsRender.Shader;
import Render3D.collision.CollisionManager;
import Render3D.graphics.Background;
import Render3D.map.DungeonManager;
import Render3D.map.LevelLoader;
import UtilsRender.Countdown;
import UtilsRender.FpsCounter;
import UtilsRender.TimeUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class Main {

    private enum GameState { TITLE, PLAYING, PAUSED, END }

    private enum Difficulty { FACIL, NORMAL, DIFICIL }

    private static Countdown gameTimer;

    private static Player player;
    private static InputPlayer inputPlayer;
    private static MouseController mouseController;
    private static Shader wallShader;
    private static Shader itemShader;
    private static float elapsedTime = 0f;
    private static Window window;
    private static TextRender textRenderer;
    private static TextRender bigTextRenderer;
    private static CollisionManager collisionManager;
    private static AudioSystem audioSystem;
    private static DungeonManager dungeonManager;
    private static String[] levelLayout;
    private static LevelLoader.LevelInfo[] levelFiles;
    private static int levelIndex = 0;
    private static Background background;
    private static VisionManager visionManager;
    private static QuadBatch uiBatch;
    private static Minimap minimap;

    private static GameState state = GameState.TITLE;

    // Dificultad seleccionada en el menú del título. Fácil muestra el minimapa
    // y Normal lo oculta; en Difícil los items dan 3s menos (ver itemBonus).
    private static Difficulty difficulty = Difficulty.NORMAL;

    /** Segundos que da un item según la dificultad (Difícil: -3s). */
    private static int itemBonusSeconds() {
        return difficulty == Difficulty.DIFICIL
                ? GameConfig.ITEM_BONUS_SECONDS - 3
                : GameConfig.ITEM_BONUS_SECONDS;
    }

    // Fin de la lista de niveles: muestra "END" y vuelve al título.
    private static float endStartTime = 0f;
    private static final float END_TO_TITLE_DELAY = 3.5f;

    private static int wallTexture;
    private static int floorTexture;
    private static int ceilingTexture;

    private static Vector3f lightPos = new Vector3f(GameConfig.LIGHT_X, GameConfig.LIGHT_Y, GameConfig.LIGHT_Z);

    // Estado de entrada
    private static boolean menuKeyPressed = false;
    private static boolean escKeyPressed = false;
    private static boolean enterKeyPressed = false;
    private static boolean leftKeyPressed = false;
    private static boolean rightKeyPressed = false;
    private static boolean key8Prev = false;
    private static boolean key9Prev = false;
    private static boolean key0Prev = false;

    // Botones del menú de pausa (centrados).
    private static Button buttonCerrarMenu;
    private static Button buttonRestartTime;
    private static Button buttonMinusFPS;
    private static Button buttonPlusFPS;
    private static Button buttonSalir;
    private static final Button[] menuButtons = new Button[5];

    // Config del minimapa y reinicio de partida (menú de pausa).
    private static Button buttonMinimapMinus;
    private static Button buttonMinimapPlus;
    private static Button buttonMinimapToggle;
    private static Button buttonReiniciar;

    // Botones de las pantallas de título.
    private static Button buttonJugar;

    // Selector de nivel en el título (◀ / ▶).
    private static Button buttonLevelPrev;
    private static Button buttonLevelNext;

    // Selector de dificultad en el título (◀ / ▶).
    private static Button buttonDiffPrev;
    private static Button buttonDiffNext;

    private static int currentFPS = 0;
    private static final FpsCounter fpsCounter = new FpsCounter();

    // Toast al recoger items.
    private static String toastText = null;
    private static float toastStartTime = 0f;

    public static void main(String[] args) {
        initApplication();
        audioSystem = new AudioSystem();
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
        mouseController = new MouseController(player, window.getWindowHandle());
        mouseController.setCaptured(false);

        initTextRenderer();
        initShaders();
        background = new Background();
        initDungeon();
        initVision();
        initUiLayer();
        initButtons();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void initTextRenderer() {
        textRenderer = new TextRender(GameConfig.FONT_PATH, GameConfig.FONT_SIZE);
        textRenderer.setProjection(window.getWidth(), window.getHeight());
        bigTextRenderer = new TextRender(GameConfig.FONT_PATH, GameConfig.VISION_FONT_SIZE);
        bigTextRenderer.setProjection(window.getWidth(), window.getHeight());
    }

    private static void initShaders() {
        wallShader = new Shader("shaders/wall_vertex.glsl", "shaders/wall_fragment.glsl");
        itemShader = new Shader("shaders/wall_vertex.glsl", "shaders/item_fragment.glsl");
    }

    private static void initDungeon() {
        wallTexture = TextureLoader.loadTexture(GameConfig.WALL_TEXTURE);
        floorTexture = TextureLoader.loadTexture(GameConfig.FLOOR_TEXTURE);
        ceilingTexture = TextureLoader.loadTexture(GameConfig.CEILING_TEXTURE);

        levelFiles = LevelLoader.listLevels();
        levelLayout = LevelLoader.load(currentLevel());
        dungeonManager = new DungeonManager(levelLayout, wallTexture, floorTexture, ceilingTexture);
        dungeonManager.registerCollisions(collisionManager);
    }

    /** Ruta del nivel seleccionado (índice cíclico sobre {@link #levelFiles}). */
    private static String currentLevel() {
        return levelFiles[levelIndex % levelFiles.length].path;
    }

    /** Título (directiva name:) del nivel seleccionado. */
    private static String currentLevelName() {
        return levelFiles[levelIndex % levelFiles.length].name;
    }

    /** Título del siguiente nivel (índice +1, cíclico). */
    private static String nextLevelName() {
        return levelFiles[(levelIndex + 1) % levelFiles.length].name;
    }

    /** Nombre mostrado de la dificultad seleccionada. */
    private static String difficultyLabel() {
        switch (difficulty) {
            case FACIL:
                return "Facil";
            case DIFICIL:
                return "Dificil";
            default:
                return "Normal";
        }
    }

    /** Tiempo (directiva time: mm:ss) del nivel seleccionado, en segundos. */
    private static int currentLevelTime() {
        return levelFiles[levelIndex % levelFiles.length].timeSeconds;
    }

    /** Cambia el nivel seleccionado (adelante/atrás) y recarga su layout. */
    private static void stepLevel(int delta) {
        levelIndex = (levelIndex + delta % levelFiles.length + levelFiles.length) % levelFiles.length;
        levelLayout = LevelLoader.load(currentLevel());
    }

    /** Cambia la dificultad seleccionada (circular). */
    private static void stepDifficulty(int delta) {
        Difficulty[] values = Difficulty.values();
        difficulty = values[(difficulty.ordinal() + delta % values.length + values.length) % values.length];
    }

    private static void initVision() {
        visionManager = new VisionManager(GameConfig.FONT_PATH, GameConfig.VISION_FONT_SIZE);
        visionManager.setProjection(window.getWidth(), window.getHeight());
    }

    private static void initUiLayer() {
        uiBatch = new QuadBatch();
        uiBatch.setProjection(window.getWidth(), window.getHeight());
        minimap = new Minimap(dungeonManager);
    }

    /** (Re)inicia la partida: reconstruye el nivel, resetea items y tiempo. */
    private static void startGame() {
        collisionManager.clear();
        // Facil muestra el minimapa; Normal y Dificil lo ocultan al arrancar.
        minimap.setVisible(difficulty == Difficulty.FACIL);
        updateMinimapToggleLabel();
        levelLayout = LevelLoader.load(currentLevel());
        dungeonManager.applyLayout(levelLayout, wallTexture, floorTexture, ceilingTexture);
        dungeonManager.registerCollisions(collisionManager);

        gameTimer = new Countdown(currentLevelTime());
        gameTimer.start();

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

        visionManager.showMessage(currentLevelName(), 1f, 0.86f, 0.35f, 0.75f, 2.5f);

        state = GameState.PLAYING;
        mouseController.setCaptured(true);
    }

    private static void pauseGame() {
        if (gameTimer != null) {
            gameTimer.pause();
        }
        state = GameState.PAUSED;
        mouseController.setCaptured(false);
    }

    private static void resumeGame() {
        // Si el tiempo se acabó (se perdió), "Continuar" carga ya el siguiente
        // nivel en lugar de reanudar una partida terminada.
        if (gameTimer != null && gameTimer.isFinished()) {
            if (levelIndex >= levelFiles.length - 1) {
                endGame();
            } else {
                stepLevel(1);
                startGame();
            }
            return;
        }
        if (gameTimer != null) {
            gameTimer.resume();
        }
        state = GameState.PLAYING;
        mouseController.setCaptured(true);
    }

    private static void winGame() {
        // Sin pantalla de victoria: se carga directamente el siguiente nivel.
        if (levelIndex >= levelFiles.length - 1) {
            endGame();
        } else {
            stepLevel(1);
            startGame();
        }
    }

    private static void loseGame() {
        // En lugar de una pantalla aparte, se abre el menú completo de pausa;
        // ahí se ve el nombre del siguiente nivel y se puede reanudar/continuar.
        if (gameTimer != null) {
            gameTimer.pause();
        }
        state = GameState.PAUSED;
        mouseController.setCaptured(false);
        visionManager.showMessage("SE ACABO EL TIEMPO", 1f, 0.05f, 0.05f, 0.8f, 2.5f);
    }

    /** Fin de todos los niveles: "END" y, tras un momento, al título. */
    private static void endGame() {
        state = GameState.END;
        mouseController.setCaptured(false);
        endStartTime = elapsedTime;
        visionManager.showMessage("END", 1f, 0.86f, 0.35f, 0.85f, END_TO_TITLE_DELAY);
    }

    private static void goToTitle() {
        state = GameState.TITLE;
        levelIndex = 0;
        mouseController.setCaptured(false);
    }

    private static void initButtons() {
        buttonCerrarMenu = new Button(0.5f, 0.2f, 150, 50, "Continuar", textRenderer);
        buttonCerrarMenu.setOnClickListener(() -> resumeGame());

        buttonRestartTime = new Button(0.5f, 0.2f, 200, 50, "Reiniciar Tiempo", textRenderer);
        buttonRestartTime.setOnClickListener(() -> {
            gameTimer = new Countdown(currentLevelTime());
            gameTimer.start();
        });

        buttonMinusFPS = new Button(0.5f, 0.2f, 100, 50, "-FPS", textRenderer);
        buttonMinusFPS.setOnClickListener(() -> TimeUtils.setFPS(30));

        buttonPlusFPS = new Button(0.5f, 0.2f, 100, 50, "+FPS", textRenderer);
        buttonPlusFPS.setOnClickListener(() -> TimeUtils.setFPS(60));

        buttonSalir = new Button(0.5f, 0.2f, 150, 50, "Salir", textRenderer);
        buttonSalir.setOnClickListener(() -> System.exit(0));

        menuButtons[0] = buttonCerrarMenu;
        menuButtons[1] = buttonRestartTime;
        menuButtons[2] = buttonMinusFPS;
        menuButtons[3] = buttonPlusFPS;
        menuButtons[4] = buttonSalir;

        buttonJugar = new Button(0.5f, 0.5f, 200, 56, "Jugar", textRenderer);
        buttonJugar.setOnClickListener(() -> startGame());

        buttonLevelPrev = new Button(0.5f, 0.5f, 42, 42, "<", textRenderer);
        buttonLevelPrev.setOnClickListener(() -> stepLevel(-1));

        buttonLevelNext = new Button(0.5f, 0.5f, 42, 42, ">", textRenderer);
        buttonLevelNext.setOnClickListener(() -> stepLevel(1));

        buttonDiffPrev = new Button(0.5f, 0.5f, 42, 42, "<", textRenderer);
        buttonDiffPrev.setOnClickListener(() -> stepDifficulty(-1));

        buttonDiffNext = new Button(0.5f, 0.5f, 42, 42, ">", textRenderer);
        buttonDiffNext.setOnClickListener(() -> stepDifficulty(1));

        buttonMinimapMinus = new Button(0.5f, 0.5f, 42, 42, "-", textRenderer);
        buttonMinimapMinus.setOnClickListener(() -> minimap.shrink());

        buttonMinimapPlus = new Button(0.5f, 0.5f, 42, 42, "+", textRenderer);
        buttonMinimapPlus.setOnClickListener(() -> minimap.grow());

        buttonMinimapToggle = new Button(0.5f, 0.5f, 200, 44, "Minimapa: OFF", textRenderer);
        buttonMinimapToggle.setOnClickListener(() -> {
            minimap.toggle();
            updateMinimapToggleLabel();
        });

        buttonReiniciar = new Button(0.5f, 0.5f, 200, 50, "Reiniciar", textRenderer);
        buttonReiniciar.setOnClickListener(() -> startGame());

        centerMenuButtons();
        layoutFlowButtons();

        window.setOnResizeCallback((w2, h2) -> {
            textRenderer.setProjection(w2, h2);
            bigTextRenderer.setProjection(w2, h2);
            visionManager.setProjection(w2, h2);
            uiBatch.setProjection(w2, h2);
            centerMenuButtons();
            layoutFlowButtons();
        });
    }

    /** Centro los botones del menú de pausa: columna centrada en X, bloque en Y. */
    private static void centerMenuButtons() {
        int w = window.getWidth();
        int h = window.getHeight();
        float gap = 22f;
        float totalH = 0f;
        for (Button b : menuButtons) {
            totalH += b.getHeight();
        }
        totalH += gap * (menuButtons.length - 1);
        float y = (h - totalH) / 2f;
        float cx = w / 2f;
        for (Button b : menuButtons) {
            b.setPositionRelative((cx - b.getWidth() / 2f) / w, y / h);
            b.updatePosition(w, h);
            y += b.getHeight() + gap;
        }

        // Fila [- n px +] del tamaño del minimapa, botón de visibilidad y
        // botón de reinicio.
        float mapRowY = y + 40f;
        buttonMinimapMinus.setPositionRelative((cx - 90f) / w, mapRowY / h);
        buttonMinimapMinus.updatePosition(w, h);
        buttonMinimapPlus.setPositionRelative((cx + 48f) / w, mapRowY / h);
        buttonMinimapPlus.updatePosition(w, h);
        positionCentered(buttonMinimapToggle, mapRowY + 36f);
        positionCentered(buttonReiniciar, mapRowY + 108f);
    }

    /** Y (píxeles) de la fila de tamaño del minimapa en el menú de pausa. */
    private static float minimapRowY(int h) {
        float y = (h - menuTotalHeight()) / 2f;
        y += menuTotalHeight() + 40f;
        return y;
    }

    /** Texto del botón que muestra/oculta el minimapa según su estado. */
    private static void updateMinimapToggleLabel() {
        buttonMinimapToggle.setText(minimap.isVisible() ? "Minimapa: ON" : "Minimapa: OFF");
    }

    /** Botones del título centrados en pantalla. */
    private static void layoutFlowButtons() {
        int w = window.getWidth();
        int h = window.getHeight();
        positionCentered(buttonJugar, h * 0.42f);

        float selectorY = h * 0.42f + 90f;
        buttonLevelPrev.setPositionRelative((w - 180f) / 2f / w, selectorY / h);
        buttonLevelPrev.updatePosition(w, h);
        buttonLevelNext.setPositionRelative((w + 96f) / 2f / w, selectorY / h);
        buttonLevelNext.updatePosition(w, h);

        float diffY = selectorY + 64f;
        buttonDiffPrev.setPositionRelative((w - 180f) / 2f / w, diffY / h);
        buttonDiffPrev.updatePosition(w, h);
        buttonDiffNext.setPositionRelative((w + 96f) / 2f / w, diffY / h);
        buttonDiffNext.updatePosition(w, h);
    }

    private static void positionCentered(Button b, float yPixels) {
        int w = window.getWidth();
        int h = window.getHeight();
        b.setPositionRelative((w - b.getWidth()) / 2f / w, yPixels / h);
        b.updatePosition(w, h);
    }

    private static float menuTotalHeight() {
        float gap = 22f;
        float total = 0f;
        for (Button b : menuButtons) {
            total += b.getHeight();
        }
        total += gap * (menuButtons.length - 1);
        return total;
    }

    private static void handleInput() {
        long handle = window.getWindowHandle();

        boolean mPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (mPressed && !menuKeyPressed) {
            if (state == GameState.PLAYING) {
                pauseGame();
            } else if (state == GameState.PAUSED) {
                resumeGame();
            }
        }
        menuKeyPressed = mPressed;

        // P también pausa/libera el mouse (y reanuda). ESC está reservado.
        boolean pPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_P) == GLFW.GLFW_PRESS;
        if (pPressed && !escKeyPressed) {
            if (state == GameState.PLAYING) {
                pauseGame();
            } else if (state == GameState.PAUSED) {
                resumeGame();
            }
        }
        escKeyPressed = pPressed;

        switch (state) {
            case PLAYING:
                handleNumKeys(handle);
                break;
            case PAUSED:
                for (Button b : menuButtons) {
                    b.handleMouseEvent(handle);
                }
                buttonMinimapMinus.handleMouseEvent(handle);
                buttonMinimapPlus.handleMouseEvent(handle);
                buttonMinimapToggle.handleMouseEvent(handle);
                buttonReiniciar.handleMouseEvent(handle);
                break;
            case TITLE:
                boolean enterPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
                if (enterPressed && !enterKeyPressed) {
                    startGame();
                }
                enterKeyPressed = enterPressed;

                buttonJugar.handleMouseEvent(handle);
                buttonLevelPrev.handleMouseEvent(handle);
                buttonLevelNext.handleMouseEvent(handle);
                buttonDiffPrev.handleMouseEvent(handle);
                buttonDiffNext.handleMouseEvent(handle);

                boolean leftPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
                if (leftPressed && !leftKeyPressed) {
                    stepLevel(-1);
                }
                leftKeyPressed = leftPressed;

                boolean rightPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;
                if (rightPressed && !rightKeyPressed) {
                    stepLevel(1);
                }
                rightKeyPressed = rightPressed;
                break;
            default:
                break;
        }
    }

    /** Atajos 8/9/0 para probar los efectos de visión (solo en partida). */
    private static void handleNumKeys(long handle) {
        boolean k8 = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_8) == GLFW.GLFW_PRESS;
        if (k8 && !key8Prev) {
            visionManager.showMessage("PELIGRO", 1f, 0.05f, 0.05f, 0.75f, 1.8f);
        }
        key8Prev = k8;

        boolean k9 = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_9) == GLFW.GLFW_PRESS;
        if (k9 && !key9Prev) {
            visionManager.addOverlay(0.1f, 1f, 0.35f, 0.55f, 1.6f);
        }
        key9Prev = k9;

        boolean k0 = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_0) == GLFW.GLFW_PRESS;
        if (k0 && !key0Prev) {
            visionManager.darken(0.9f, 1.3f);
        }
        key0Prev = k0;
    }

    private static void showToast(String text) {
        toastText = text;
        toastStartTime = elapsedTime;
    }

    private static void update() {
        float deltaTime = TimeUtils.getDeltaTime();
        elapsedTime += deltaTime;
        // Los pasos solo suenan mientras la partida está activa.
        float playerSpeed = (state == GameState.PLAYING && gameTimer != null && !gameTimer.isFinished())
                ? player.getVelocity().length() : 0f;
        audioSystem.update(deltaTime, playerSpeed);

        if (state == GameState.PLAYING) {
            mouseController.update();
            inputPlayer.update(deltaTime);
            boolean picked = dungeonManager.update(player.getPosition(), deltaTime);
            if (picked) {
                audioSystem.playPicked();
                if (gameTimer != null) {
                    gameTimer.addSeconds(itemBonusSeconds());
                }
                int collected = dungeonManager.getCollectedItems();
                showToast("Artefacto " + collected + "/" + dungeonManager.getTotalItems()
                        + " (+" + itemBonusSeconds() + "s)");
                if (dungeonManager.getTotalItems() > 0 && collected >= dungeonManager.getTotalItems()) {
                    winGame();
                }
            }
            if (state == GameState.PLAYING && gameTimer.isFinished()) {
                loseGame();
            }
        }

        if (state == GameState.END && elapsedTime - endStartTime >= END_TO_TITLE_DELAY) {
            goToTitle();
        }
    }

    private static void mainLoop() {
        TimeUtils.setFPS(GameConfig.TARGET_FPS);
        window.loop(() -> {
            TimeUtils.update();
            fpsCounter.update(TimeUtils.getDeltaTime());
            currentFPS = fpsCounter.getCurrentFPS();

            clearScreen();
            visionManager.update(TimeUtils.getDeltaTime());
            handleInput();
            update();

            render3DScene();
            renderUI();
            visionManager.render();
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

            background.render(elapsedTime, window.getWidth(), window.getHeight(), levelIndex);

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
        int w = window.getWidth();
        int h = window.getHeight();

        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Capas de fondo y minimapa (batch único).
        switch (state) {
            case TITLE:
                uiBatch.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.75f);
                break;
            case PAUSED:
                uiBatch.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.5f);
                minimap.draw(uiBatch, w, h, player.getPosition(), player.getCamera().getYaw());
                break;
            case END:
                uiBatch.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.88f);
                break;
            case PLAYING:
                minimap.draw(uiBatch, w, h, player.getPosition(), player.getCamera().getYaw());
                break;
            default:
                break;
        }
        uiBatch.render();

        switch (state) {
            case PLAYING:
                renderPlayingHud(w, h);
                break;
            case PAUSED:
                renderPauseMenu(w, h);
                break;
            case TITLE:
                renderTitleScreen(w, h);
                break;
            default:
                break;
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    private static void renderPlayingHud(int w, int h) {
        String hints = "Presiona M para abrir el menu";
        textRenderer.renderer(hints, w - textRenderer.getTextWidth(hints) - 20f, 20f, 1f, 1f, 1f);

        textRenderer.rendererRelativo(
                "items:(" + dungeonManager.getCollectedItems() + "/" + dungeonManager.getTotalItems() + ")",
                0.02f, 0.94f, 0.3f, 1f, 0.3f);

        if (!gameTimer.isFinished()) {
            textRenderer.rendererRelativo("TIME: " + formatTime((int) gameTimer.getRemainingSeconds()),
                    0.02f, 0.98f, 1f, 0.3f, 0.3f);
        } else {
            textRenderer.rendererRelativo("TIME: END", 0.02f, 0.98f, 1f, 0.1f, 0.1f);
        }

        drawToast(w, h);
    }

    private static void renderPauseMenu(int w, int h) {
        buttonCerrarMenu.draw(w, h, window.getWindowHandle());
        buttonRestartTime.draw(w, h, window.getWindowHandle());
        buttonMinusFPS.draw(w, h, window.getWindowHandle());
        buttonPlusFPS.draw(w, h, window.getWindowHandle());
        buttonSalir.draw(w, h, window.getWindowHandle());

        float buttonsTop = (h - menuTotalHeight()) / 2f;
        centeredText(w, "Menu", buttonsTop - 70f, 1f, 1f, 1f);
        // En el menú (pausa o derrota) se indica el siguiente nivel y se puede
        // continuar cargaándolo rápido.
        centeredText(w, "Siguiente nivel: " + nextLevelName(), buttonsTop - 40f, 0.86f, 0.35f, 0.1f);

        // FPS en la esquina superior derecha.
        String fpsText = "FPS: " + currentFPS;
        textRenderer.renderer(fpsText, w - textRenderer.getTextWidth(fpsText) - 20f, 20f, 0.3f, 1f, 0.3f);

        // Fila de tamaño del minimapa: [-] n px [+], botón de visibilidad.
        float mapRowY = minimapRowY(h);
        updateMinimapToggleLabel();
        buttonMinimapMinus.draw(w, h, window.getWindowHandle());
        buttonMinimapPlus.draw(w, h, window.getWindowHandle());
        buttonMinimapToggle.draw(w, h, window.getWindowHandle());
        String sizeLabel = minimap.getSizePx() + " px";
        float labelW = textRenderer.getTextWidth(sizeLabel);
        textRenderer.renderer(sizeLabel, (w - labelW) / 2f, mapRowY + ((42 - textRenderer.getTextHeight(sizeLabel)) / 2f), 0f, 1f, 1f);
        centeredText(w, "Minimapa", mapRowY - 36f, 1f, 1f, 1f);

        buttonReiniciar.draw(w, h, window.getWindowHandle());

        centeredText(w,
                "Controles: WASD - Moverse | Mouse - Rotar cam | M/P - Menu | Ctrl - Agacharse | Shift - Correr | 8/9/0 - FX",
                mapRowY + 152f, 1f, 1f, 1f);
    }

    private static void renderTitleScreen(int w, int h) {
        centeredBigText(w, "LUMINAL SCAPE", h * 0.30f, 1f, 0.86f, 0.35f);
        centeredText(w, "Encuentra los artefactos antes de que se acabe el tiempo",
                h * 0.30f + 68f, 1f, 1f, 1f);

        buttonJugar.draw(w, h, window.getWindowHandle());

        float selectorY = h * 0.42f + 90f;
        buttonLevelPrev.draw(w, h, window.getWindowHandle());
        buttonLevelNext.draw(w, h, window.getWindowHandle());
        centeredText(w, "Nivel: " + currentLevelName(), selectorY + 9f, 0f, 1f, 1f);

        float diffY = selectorY + 64f;
        buttonDiffPrev.draw(w, h, window.getWindowHandle());
        buttonDiffNext.draw(w, h, window.getWindowHandle());
        centeredText(w, "Dificultad: " + difficultyLabel(), diffY + 9f, 1f, 0.86f, 0.35f);

        centeredText(w, "Presiona ENTER para jugar | < > para elegir nivel",
                diffY + 70f, 1f, 1f, 1f);
        centeredText(w, "WASD - Moverse | Mouse - Rotar cam | M/P - Menu | Ctrl - Agacharse | Shift - Correr",
                h * 0.90f, 1f, 1f, 1f);
    }

    private static void drawToast(int w, int h) {
        if (toastText == null) {
            return;
        }
        float t = elapsedTime - toastStartTime;
        float fadeOutStart = TOAST_DURATION - 0.7f;
        if (t > TOAST_DURATION) {
            toastText = null;
            return;
        }
        float alpha = 1f;
        if (t < 0.3f) {
            alpha = t / 0.3f;
        } else if (t > fadeOutStart) {
            alpha = 1f - (t - fadeOutStart) / 0.7f;
        }
        float textW = textRenderer.getTextWidth(toastText);
        textRenderer.renderer(toastText, (w - textW) / 2f, h - 90f, 1f, 0.95f, 0.6f, alpha);
    }

    private static void centeredText(float windowWidth, String text, float y, float r, float g, float b) {
        float www = textRenderer.getTextWidth(text);
        textRenderer.renderer(text, (windowWidth - www) / 2f, y, r, g, b);
    }

    private static void centeredBigText(float windowWidth, String text, float y, float r, float g, float b) {
        float www = bigTextRenderer.getTextWidth(text);
        bigTextRenderer.renderer(text, (windowWidth - www) / 2f, y, r, g, b);
    }

    private static void cleanup() {
        dungeonManager.cleanup();
        background.cleanup();
        wallShader.cleanup();
        itemShader.cleanup();
        visionManager.cleanup();
        uiBatch.cleanup();
        window.cleanup();
        audioSystem.cleanup();
    }

    private static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static final float TOAST_DURATION = 2.2f;
}