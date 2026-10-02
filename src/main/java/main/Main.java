package main;

import MediaUtil.AudioSystem;
import UtilsRender.TextureLoader;
import UtilsRender.Window;
import Render2D.Button;
import Render2D.Minimap;
import Render2D.UIScale;
import Render2D.QuadBatch;
import Render2D.ImageQuad;
import Render2D.TextRender;
import Render2D.VisionManager;
import Player.Player;
import Player.InputPlayer;
import Player.MouseController;
import UtilsRender.Shader;
import Render3D.collision.CollisionManager;
import Render3D.graphics.SkyRenderer;
import Render3D.map.Atmosphere;
import Render3D.map.DungeonManager;
import Render3D.map.LevelData;
import Render3D.map.LevelLoader;
import Render3D.map.LevelTypePreset;
import Render3D.item.Item;
import Render3D.item.ItemMap;
import Render3D.item.ItemTime;
import Render3D.map.MapConfig;
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
    private static Shader propShader;
    private static float elapsedTime = 0f;
    private static Window window;
    private static ImageQuad titleLogo;
    private static TextRender textRenderer;
    private static CollisionManager collisionManager;
    private static AudioSystem audioSystem;
    private static DungeonManager dungeonManager;
    private static LevelData levelData;
    private static LevelLoader.LevelInfo[] levelFiles;
    private static int levelIndex = 0;
    private static SkyRenderer skyRenderer;
    private static VisionManager visionManager;
    private static QuadBatch uiBatch;
    private static Minimap minimap;

    private static GameState state = GameState.TITLE;

    // Dificultad seleccionada en el menú del título:
    //   Facil  - el minimapa viene encendido y revelado de salida, asi que el
    //            item de mapa se cambia por uno de tiempo.
    //   Normal - el minimapa arranca apagado; el item de mapa lo enciende.
    //   Dificil - como Normal, pero la niebla se cierra a la vista mas corta
    //            (VIEW_DISTANCE_LOW) y los items dan 3s menos (ver itemBonus).
    private static Difficulty difficulty = Difficulty.NORMAL;

    /** Segundos que da un item según la dificultad (Difícil: -3s). */
    private static int itemBonusSeconds() {
        return difficulty == Difficulty.DIFICIL
                ? GameConfig.ITEM_BONUS_SECONDS - 3
                : GameConfig.ITEM_BONUS_SECONDS;
    }

    /**
     * Distancia de vista del nivel, con la dificultad por encima.
     *
     * <p>En Dificil manda {@link MapConfig#VIEW_DISTANCE_LOW} aunque el nivel
     * pida mas: la niebla es el handicap, y si el nivel pudiera abrirla el modo
     * no seria distinto de Normal. Solo afecta a lo que ve el jugador; ni el
     * mapa ni la colision cambian.
     */
    private static float viewDistance() {
        return difficulty == Difficulty.DIFICIL
                ? MapConfig.VIEW_DISTANCE_LOW
                : levelData.getViewDistance();
    }

    // Fin de la lista de niveles: muestra "END" y vuelve al título.
    private static float endStartTime = 0f;
    private static final float END_TO_TITLE_DELAY = 3.5f;

    /**
     * Separacion vertical entre los botones del menu de pausa, en unidades de
     * interfaz.
     *
     * <p>La comparten {@link #centerMenuButtons()} y {@link #menuTotalHeight()}
     * a proposito: las dos calculan el alto del bloque de botones, y con dos
     * numeros distintos el bloque se descuadraria del titulo de arriba.
     */
    private static final float MENU_GAP = 34f;

    private static int wallTexture;
    private static int floorTexture;
    private static int ceilingTexture;
    private static int lightCeilingTexture;
    private static String loadedFloorTexture;
    private static String loadedCeilingTexture;
    private static String loadedWallTexture;
    private static String loadedLightCeilingTexture;

    /**
     * Buffers donde caen las luces puntuales de cada frame, reutilizados para
     * no crear un array por luz y frame. Los rellena
     * {@link DungeonManager#collectLights} y los vuelca
     * {@link Shader#setVec3Array} en los dos shaders del escenario.
     */
    private static final float[] lightPosBuffer = new float[MapConfig.MAX_LIGHT_COUNT * 3];
    private static final float[] lightColorBuffer = new float[MapConfig.MAX_LIGHT_COUNT * 3];

    private static Vector3f lightPos = new Vector3f(GameConfig.LIGHT_X, GameConfig.LIGHT_Y, GameConfig.LIGHT_Z);

    /**
     * Partida CONGELADA con M: el juego se para y suelta el raton, pero no sale
     * el menu.
     *
     * <p>Es distinto de la pausa a proposito. La pausa (P) es un estado del
     * juego con su propia pantalla y sus botones; aqui el estado sigue siendo
     * PLAYING y lo unico que se apaga es la simulacion, asi que el jugador ve
     * el nivel entero tal cual estaba, sin el fondo oscurecido ni el menu
     * encima. Es lo que hace falta cuando hay que salir a otra cosa con el
     * raton (copiar un texto, mirar una ventana) sin perder de vista la partida
     * ni acordarse de como iba.
     *
     * <p>No es un estado de {@link GameState} porque no cambia nada de como se
     * dibuja la pantalla: se dibuja exactamente igual que en partida.
     */
    private static boolean frozen = false;

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
    private static Button buttonSalir;
    private static final Button[] menuButtons = new Button[3];

    // Reinicio de partida (menú de pausa). El minimapa ya NO se controla desde
    // aqui: se decide con la dificultad y se revela con el item de mapa.
    private static Button buttonReiniciar;

    // Botones de las pantallas de título.
    private static Button buttonJugar;

    // Selector de nivel en el título (◀ / ▶).
    private static Button buttonLevelPrev;
    private static Button buttonLevelNext;

    // Selector de dificultad en el título (◀ / ▶).
    private static Button buttonDiffPrev;
    private static Button buttonDiffNext;

    /**
     * Píxeles de ventana por unidad de interfaz.
     *
     * <p>Toda la IU se dibuja en unidades virtuales ({@link UIScale}) y este
     * factor es el que las estira a la ventana real. Se recalcula en cada
     * resize y también en cada frame, porque el tamaño solo se puede leer de la
     * ventana ya construida.
     */
    private static float uiScale = 1f;

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
        skyRenderer = new SkyRenderer();
        initDungeon();
        initVision();
        initUiLayer();
        initButtons();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void initTextRenderer() {
        textRenderer = new TextRender(GameConfig.FONT_PATH, GameConfig.FONT_SIZE);
        // Solo la escala aqui: los botones todavia no existen (se crean en
        // initButtons), y es initButtons quien se los reparte.
        uiScale = UIScale.of(window.getWidth(), window.getHeight());
        textRenderer.setProjection(virtualW(), virtualH());
    }

    private static void initShaders() {
        wallShader = new Shader("shaders/wall_vertex.glsl", "shaders/wall_fragment.glsl");
        // La decoracion tiene su propio vertex porque lleva matriz por pieza,
        // a diferencia del escenario que va ya en coordenadas de mundo.
        propShader = new Shader("shaders/prop_vertex.glsl", "shaders/prop_fragment.glsl");
    }

    private static void initDungeon() {
        levelFiles = LevelLoader.listLevels();
        loadCurrentLevel();
        dungeonManager = new DungeonManager(levelData.toRows(), wallTexture, floorTexture, ceilingTexture,
                lightCeilingTexture);
        dungeonManager.registerCollisions(collisionManager);
    }

    /**
     * Lee el nivel seleccionado y carga las texturas de ambiente que dicta su
     * preset. Las texturas se cachean por ruta porque {@link TextureLoader}
     * crea un id de OpenGL por llamada: recargarlas en cada cambio de nivel
     * fugaria memoria de GPU.
     */
    private static void loadCurrentLevel() {
        levelData = LevelLoader.loadLevel(currentLevel());
        LevelTypePreset preset = levelData.getPreset();

        String floorPath = preset.getFloorTexture() != null
                ? preset.getFloorTexture() : GameConfig.FLOOR_TEXTURE;
        if (!floorPath.equals(loadedFloorTexture)) {
            floorTexture = TextureLoader.loadTexture(floorPath);
            loadedFloorTexture = floorPath;
        }

        String ceilingPath = preset.getCeilingTexture() != null
                ? preset.getCeilingTexture() : GameConfig.CEILING_TEXTURE;
        if (!ceilingPath.equals(loadedCeilingTexture)) {
            ceilingTexture = TextureLoader.loadTexture(ceilingPath);
            loadedCeilingTexture = ceilingPath;
        }

        // El muro tambien es del preset, con el mismo cacheo: sin esto, pasar
        // de los backrooms al pasillo dejaba el muro amarillo del nivel
        // anterior pegado al hormigon nuevo.
        String wallPath = preset.getWallTexture() != null
                ? preset.getWallTexture() : GameConfig.WALL_TEXTURE;
        if (!wallPath.equals(loadedWallTexture)) {
            wallTexture = TextureLoader.loadTexture(wallPath);
            loadedWallTexture = wallPath;
        }

        // La placa del techo con luz '◉' es global, no del preset: es una baldosa
        // mas del pack y va en su propia malla, asi que no depende de donde este
        // el nivel. Se cachea igual por no recrear el id en cada cambio.
        if (!MapConfig.LIGHT_CEILING_TEXTURE.equals(loadedLightCeilingTexture)) {
            lightCeilingTexture = TextureLoader.loadTexture(MapConfig.LIGHT_CEILING_TEXTURE);
            loadedLightCeilingTexture = MapConfig.LIGHT_CEILING_TEXTURE;
        }
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
        return levelData.getTimeSeconds();
    }

    /** Cambia el nivel seleccionado (adelante/atrás) y recarga su layout. */
    private static void stepLevel(int delta) {
        levelIndex = (levelIndex + delta % levelFiles.length + levelFiles.length) % levelFiles.length;
        loadCurrentLevel();
    }

    /** Cambia la dificultad seleccionada (circular). */
    private static void stepDifficulty(int delta) {
        Difficulty[] values = Difficulty.values();
        difficulty = values[(difficulty.ordinal() + delta % values.length + values.length) % values.length];
    }

    private static void initVision() {
        visionManager = new VisionManager(GameConfig.FONT_PATH, GameConfig.VISION_FONT_SIZE);
        visionManager.setProjection(virtualW(), virtualH());
    }

    private static void initUiLayer() {
        uiBatch = new QuadBatch();
        uiBatch.setProjection(virtualW(), virtualH());
        minimap = new Minimap(dungeonManager);
        // Logo del titulo: imagen en vez del texto grande.
        titleLogo = new ImageQuad(GameConfig.TITLE_LOGO_TEXTURE);
    }

    /** (Re)inicia la partida: reconstruye el nivel, resetea items y tiempo. */
    private static void startGame() {
        collisionManager.clear();
        // El minimapa arranca de cero en CADA nivel: el plano que se revelaba
        // en el anterior no dice nada del nuevo, y dejarlo puesto hacia que
        // comentar el nivel 2 bastara para tener el 3 resuelto. Por eso el item
        // de mapa va tambien en el resto de niveles, y por eso el reset va
        // aqui y no solo al empezar la partida.
        minimap.reset();
        if (difficulty == Difficulty.FACIL) {
            // En Facil se empieza con el plano en la mano, asi que el '☑' deja
            // tiempo en vez de mapa: un mapa que no revela nada no vale nada.
            minimap.revealAll();
        }
        dungeonManager.setMapItemGivesTime(difficulty == Difficulty.FACIL);
        loadCurrentLevel();
        dungeonManager.applyLayout(levelData.toRows(), wallTexture, floorTexture, ceilingTexture,
                lightCeilingTexture);
        dungeonManager.registerCollisions(collisionManager);

        gameTimer = new Countdown(currentLevelTime());
        gameTimer.start();
        // Un nivel nuevo nunca arranca congelado: si se entra aqui desde la
        // pausa rapida, el raton tiene que volver a la partida.
        frozen = false;
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

    /**
     * Congela la partida y suelta el raton, sin abrir el menu.
     *
     * <p>Se para el tiempo y se deja de mover la camara y el jugador, asi que al
     * volver la partida sigue exactamente donde estaba. El raton se suelta para
     * que el jugador pueda salir a otra ventana; la pantalla no cambia, porque el
     * estado sigue siendo PLAYING (ver {@link #frozen}).
     */
    private static void freezeGame() {
        if (gameTimer != null) {
            gameTimer.pause();
        }
        frozen = true;
        mouseController.setCaptured(false);
    }

    /** Quita el congelado: el tiempo y el raton vuelven como estaban. */
    private static void unfreezeGame() {
        // Sin este caso, si el jugador congela justo cuando el tiempo se
        // acababa, al volver seguiria con la partida perdida congelada y el
        // "SE ACABO EL TIEMPO" saltaria tarde. Con el, reanudar lleva al
        // siguiente nivel igual que hace "Continuar" tras la derrota.
        if (gameTimer != null && gameTimer.isFinished()) {
            frozen = false;
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
        frozen = false;
        mouseController.setCaptured(true);
    }

    private static void pauseGame() {
        // Si se abre el menu desde una partida congelada, el menu manda: al
        // reanudar se vuelve a partida normal, no a la congelada.
        frozen = false;
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
        frozen = false;
        state = GameState.PLAYING;
        mouseController.setCaptured(true);
    }

    private static void winGame() {
        // El plano es de ESTE nivel: al terminarlo se retira, igual que al
        // perder. Si se dejara puesto, el siguiente nivel empezaria con el
        // mapa del anterior revelado y el jugador tendria delante el plano del
        // nivel que acaba de superar.
        minimap.reset();
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
        // El minimapa no se dibuja ya en pausa (ver renderUI), pero se apaga
        // igual: si el jugador sigue con el tiempo agotado, el plano revelado
        // de un nivel ya terminado no debe quedar encendido.
        minimap.reset();
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
        // Fin de la partida: el plano del ultimo nivel tampoco se queda en
        // pantalla, por el mismo motivo que en winGame/loseGame.
        minimap.reset();
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

        buttonSalir = new Button(0.5f, 0.2f, 150, 50, "Salir", textRenderer);
        buttonSalir.setOnClickListener(() -> System.exit(0));

        menuButtons[0] = buttonCerrarMenu;
        menuButtons[1] = buttonRestartTime;
        menuButtons[2] = buttonSalir;

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

        buttonReiniciar = new Button(0.5f, 0.5f, 200, 50, "Reiniciar", textRenderer);
        buttonReiniciar.setOnClickListener(() -> startGame());

        // Los botones ya existen: se les pasa la escala y se colocan con las
        // medidas virtuales de la ventana actual.
        updateUiScale(window.getWidth(), window.getHeight());
        centerMenuButtons();
        layoutFlowButtons();

        window.setOnResizeCallback((w2, h2) -> {
            updateUiScale(w2, h2);
            textRenderer.setProjection(virtualW(), virtualH());
            visionManager.setProjection(virtualW(), virtualH());
            uiBatch.setProjection(virtualW(), virtualH());
            centerMenuButtons();
            layoutFlowButtons();
        });
    }

    /**
     * Recalcula la escala de la interfaz y la reparte a los botones.
     *
     * <p>Se llama al crear la ventana y en cada resize. Los botones la necesitan
     * porque el ratón llega en píxeles de ventana y ellos se miden en unidades
     * de interfaz: sin esto, en una ventana que no sea la de referencia el clic
     * caería fuera del botón.
     */
    private static void updateUiScale(int w, int h) {
        uiScale = UIScale.of(w, h);
        for (Button b : menuButtons) {
            b.setScale(uiScale);
        }
        buttonReiniciar.setScale(uiScale);
        buttonJugar.setScale(uiScale);
        buttonLevelPrev.setScale(uiScale);
        buttonLevelNext.setScale(uiScale);
        buttonDiffPrev.setScale(uiScale);
        buttonDiffNext.setScale(uiScale);
    }

    /** Ancho de la ventana en unidades de interfaz. */
    private static int virtualW() {
        return UIScale.virtualW(window.getWidth(), uiScale);
    }

    /** Alto de la ventana en unidades de interfaz. */
    private static int virtualH() {
        return UIScale.virtualH(window.getHeight(), uiScale);
    }

    /** Centro los botones del menú de pausa: columna centrada en X, bloque en Y. */
    private static void centerMenuButtons() {
        int w = virtualW();
        int h = virtualH();
        float gap = MENU_GAP;
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

        // Botón de reinicio, con un hueco de separación debajo del bloque.
        positionCentered(buttonReiniciar, y + 40f);
    }

    /** Botones del título centrados en pantalla. */
    private static void layoutFlowButtons() {
        int w = virtualW();
        int h = virtualH();
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
        int w = virtualW();
        int h = virtualH();
        b.setPositionRelative((w - b.getWidth()) / 2f / w, yPixels / h);
        b.updatePosition(w, h);
    }

    private static float menuTotalHeight() {
        // Alto del bloque de botones del menú de pausa. Tiene que salir del
        // MISMO bucle que usa centerMenuButtons() (mismos botones, mismo
        // hueco), porque los dos situan el bloque en pantalla: si se calcularan
        // por separado, el título de arriba y el bloque no coincidirian al
        // cambiar el numero de botones.
        float gap = MENU_GAP;
        float total = 0f;
        for (Button b : menuButtons) {
            total += b.getHeight();
        }
        total += gap * (menuButtons.length - 1);
        return total;
    }

    private static void handleInput() {
        long handle = window.getWindowHandle();

        // M congela la partida y suelta el raton, sin abrir el menu: el nivel se ve
        // entero y al volver a pulsarlo sigue donde estaba. La pausa con menu es
        // la P de abajo.
        boolean mPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (mPressed && !menuKeyPressed) {
            if (state == GameState.PLAYING) {
                if (frozen) {
                    unfreezeGame();
                } else {
                    freezeGame();
                }
            }
        }
        menuKeyPressed = mPressed;

        // P pausa/libera el mouse (y reanuda). ESC está reservado.
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
        // Los pasos solo suenan mientras la partida está activa (y no congelada: si el
        // juego esta parado, el jugador tampoco esta caminando).
        float playerSpeed = (state == GameState.PLAYING && !frozen
                && gameTimer != null && !gameTimer.isFinished())
                ? player.getVelocity().length() : 0f;
        audioSystem.update(deltaTime, playerSpeed);

        // Con la partida congelada la pantalla se sigue dibujando, pero aqui no se
        // simula nada: ni camara, ni jugador, ni recogida de items. El tiempo ya
        // esta parado desde freezeGame.
        if (state == GameState.PLAYING && !frozen) {
            mouseController.update();
            inputPlayer.update(deltaTime);
            Item picked = dungeonManager.update(player.getPosition(), deltaTime);
            if (picked != null) {
                audioSystem.playPicked();
                if (picked instanceof ItemMap) {
                    // El mapa no da tiempo: enciende y revela el minimapa.
                    minimap.revealAll();
                    showToast("Mapa recogido: ahora ves todo el nivel");
                } else {
                    if (gameTimer != null) {
                        gameTimer.addSeconds(itemBonusSeconds());
                    }
                    // El item de tiempo (el '☑' en facil) suma lo mismo que
                    // una llave, asi que el aviso lo nombra aparte: no es una
                    // llave y no debe parecer que falta una en el total.
                    String nombre = picked instanceof ItemTime
                            ? "Item de tiempo"
                            : "Llave " + dungeonManager.getCollectedKeys() + "/"
                                    + dungeonManager.getTotalKeys();
                    showToast(nombre + " (+" + itemBonusSeconds() + "s)");
                    // Para pasar de nivel solo hacen falta las llaves. El mapa y
                    // el tiempo son ayudas: cogelos o no, el nivel se pasa igual.
                    if (dungeonManager.getTotalKeys() > 0
                            && dungeonManager.getCollectedKeys() >= dungeonManager.getTotalKeys()) {
                        winGame();
                    }
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
        // Se limpia con el color del horizonte del nivel, que es el mismo que
        // usa la niebla: cualquier pixel que no cubra ni el cielo ni la
        // geometria queda del color del fondo y no se ve el corte.
        Vector3f horizon = levelData.getPreset().getSkyHorizonColor();
        GL11.glClearColor(horizon.x, horizon.y, horizon.z, 1.0f);
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
            LevelTypePreset preset = levelData.getPreset();
            Vector3f cameraPos = player.getCamera().getPosition();

            // 1. Cielo: se dibuja primero, sin escribir profundidad, y deja el
            //    estado de profundidad listo para la geometria.
            skyRenderer.render(projection, view, cameraPos, elapsedTime, GameConfig.FAR_PLANE, preset);

            // 2. Escenario: muros, suelos y techos con luz ambiente y niebla.
            wallShader.use();
            wallShader.setMat4("projection", projection);
            wallShader.setMat4("view", view);

            wallShader.setVec3("lightPos", lightPos);
            wallShader.setVec3("lightColor", GameConfig.LIGHT_R, GameConfig.LIGHT_G, GameConfig.LIGHT_B);
            wallShader.setVec3("viewPos", cameraPos);
            wallShader.setFloat("emissionStrength", GameConfig.EMISSION_STRENGTH);
            wallShader.setFloat("glowIntensity", GameConfig.GLOW_INTENSITY);
            setAtmosphere(wallShader, preset);
            setLights(wallShader, cameraPos);

            dungeonManager.render(wallShader);

            // 3. Decoracion e items: los dos son modelos GLB, asi que
            // comparten shader. item_fragment.glsl sigue en el repo para
            // cuando vuelva a haber items con textura.
            if (dungeonManager.getTotalProps() > 0 || dungeonManager.hasAnyItem()) {
                propShader.use();
                propShader.setMat4("projection", projection);
                propShader.setMat4("view", view);
                propShader.setVec3("lightPos", lightPos);
                propShader.setVec3("lightColor", GameConfig.LIGHT_R, GameConfig.LIGHT_G, GameConfig.LIGHT_B);
                propShader.setVec3("viewPos", cameraPos);
                setAtmosphere(propShader, preset);
                setLights(propShader, cameraPos);
                dungeonManager.renderProps(propShader);
                dungeonManager.renderItems(propShader, elapsedTime);
            }
        }
    }

    /**
     * Vuelca al shader la luz ambiente y la niebla del nivel. El color de
     * niebla es exactamente el del horizonte del cielo, para que el fundido de
     * los muros termine en el mismo color que el fondo.
     */
    private static void setAtmosphere(Shader shader, LevelTypePreset preset) {
        shader.setFloat("u_ambientLight", levelData.getAmbientLight());
        float viewDistance = viewDistance();
        shader.setFloat("u_viewDistance", viewDistance);
        // El margen cercano se deriva de la distancia de vista, asi que el
        // campo limpio se mantiene proporcional en los tres modos.
        shader.setFloat("u_fogNear", Atmosphere.fogNear(viewDistance));
        shader.setVec3("u_fogColor", preset.getSkyHorizonColor());
    }

    /**
     * Manda al shader las luces puntuales que ve el jugador: las placas '◉' del
     * techo y las llaves, de las que solo llegan las mas cercanas.
     *
     * <p>Se calculan una vez por frame y se vuelcan en los dos shaders del
     * escenario, para que un mueble al pie de una placa reciba la misma luz que
     * el suelo que lo rodea. La caida por distancia la aplica el shader con los
     * dos coeficientes de {@link MapConfig}, que se mandan aqui porque forman
     * parte de la misma ecuacion.
     */
    private static void setLights(Shader shader, Vector3f cameraPos) {
        int count = dungeonManager.collectLights(cameraPos, lightPosBuffer, lightColorBuffer,
                MapConfig.MAX_LIGHT_COUNT);
        shader.setInt("u_lightCount", count);
        shader.setFloat("u_attenLinear", MapConfig.LIGHT_ATTEN_LINEAR);
        shader.setFloat("u_attenQuadratic", MapConfig.LIGHT_ATTEN_QUADRATIC);
        shader.setVec3Array("u_lightPos", lightPosBuffer, count);
        shader.setVec3Array("u_lightColor", lightColorBuffer, count);
    }

    private static void renderUI() {
        // La UI se dibuja entera en unidades de interfaz: el factor de escala
        // se recalcula aqui porque la ventana puede haber cambiado de tamaño
        // sin que haya pasado por el callback de resize.
        updateUiScale(window.getWidth(), window.getHeight());
        int w = virtualW();
        int h = virtualH();

        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Capas de fondo y minimapa (batch único).
        switch (state) {
            case TITLE:
                uiBatch.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.75f);
                break;
            case PAUSED:
                // El minimapa NO se dibuja en pausa. El menú va sobre un fondo
                // plano, y el plano del nivel encima solo estorba (se solapa con
                // los botones y con la pista de controles) y confunde: en pausa
                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se
                // apaga al salir de uno y de otro (ver winGame/loseGame).
                uiBatch.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.5f);
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

    /**
     * Contador de tiempo del HUD. Con tiempo de sobra se dibuja en su color
     * normal; al entrar en el umbral de aviso parpadea en ambar, y en el
     * critico parpadea rapido en rojo y se marca con '!'. Asi se ve de un
     * vistazo cuanto queda sin tener que leer las cifras.
     */
    private static void drawTimer(float x, float y) {
        if (gameTimer.isFinished()) {
            textRenderer.renderer("TIME: END", x, y, 1f, 0.1f, 0.1f);
            return;
        }

        float remaining = gameTimer.getRemainingSeconds();
        float total = Math.max(1f, currentLevelTime());
        boolean critical = remaining <= timeThreshold(GameConfig.TIME_CRITICAL_FRACTION,
                GameConfig.TIME_CRITICAL_MAX_SECONDS, total);
        boolean warning = !critical && remaining <= timeThreshold(
                GameConfig.TIME_WARNING_FRACTION, GameConfig.TIME_WARNING_MAX_SECONDS, total);

        float r = 1f;
        float g = 0.3f;
        float b = 0.3f;
        String label = "TIME: " + formatTime((int) remaining);
        float alpha = 1f;

        if (critical) {
            // Rojo, parpadeo rapido y marca de aviso.
            r = 1f;
            g = 0.1f;
            b = 0.1f;
            label = "!! " + label;
            alpha = blinkAlpha(GameConfig.TIME_CRITICAL_BLINK_HZ);
        } else if (warning) {
            // Ambar, parpadeo lento.
            r = 1f;
            g = 0.72f;
            b = 0.1f;
            alpha = blinkAlpha(GameConfig.TIME_WARNING_BLINK_HZ);
        }

        textRenderer.renderer(label, x, y, r, g, b, alpha);
    }

    /**
     * Umbral de aviso: la fraccion del tiempo total del nivel, limitada al tope
     * en segundos para que en un nivel corto salte antes de tiempo. El minimo
     * entre ambos es el que manda, pero nunca baja de 1s.
     */
    private static float timeThreshold(float fraction, int maxSeconds, float total) {
        return Math.max(1f, Math.min(total * fraction, maxSeconds));
    }

    /** 1.0 en el pico del parpadeo y 0.45 en el valle, para que nunca desaparezca. */
    private static float blinkAlpha(float hz) {
        return 0.725f + 0.275f * (float) Math.cos(elapsedTime * hz * 2.0 * Math.PI);
    }

    /**
     * HUD de partida: contador de llaves, tiempo y avisos.
     *
     * <p>Las tres piezas se apilan en la ESQUINA SUPERIOR IZQUIERDA, midiendo
     * cada bloque a partir del final del anterior. Antes las tres usaban
     * {@code rendererRelativo} con proporciones fijas (0.98, 0.94...), y eso
     * las colocaba pegadas al borde SUPERIOR pero solapadas entre si y con el
     * margen, segun quanta pantalla tuviese; el texto se salia por arriba y el
     * contador de llaves caia encima del tiempo. Aqui la posicion sale de medir
     * el texto, asi que el bloque se lee entero y sin solapes a cualquier
     * resolucion.
     */
    private static void renderPlayingHud(int w, int h) {
        final float margin = 20f;
        float y = margin;

        y = drawKeysCounter(margin, y);
        y += HUD_LINE_GAP;
        drawTimer(margin, y);

        // Al jugar solo se anuncia la P, que es la pausa de verdad. La M es un
        // atajo propio: congela y suelta el raton sin menu, asi que no ocupa la
        // pantalla con un aviso. Si se perdia y no hacia nada, la unica pista
        // esta en el menu de pausa.
        String hints = "P para el menu";
        textRenderer.renderer(hints, w - textRenderer.getTextWidth(hints) - margin, margin, 1f, 1f, 1f);

        drawToast(w, h);
    }

    /** Hueco vertical entre las lineas del HUD, en unidades de interfaz. */
    private static final float HUD_LINE_GAP = 8f;

    /**
     * Contador de llaves, y devuelve la Y por la que puede seguir bajando el
     * siguiente bloque. Solo van las llaves: el mapa y el tiempo son ayudas y
     * no se cuentan (ver {@link DungeonManager#getTotalKeys()}).
     */
    private static float drawKeysCounter(float x, float y) {
        String label = "llaves:(" + dungeonManager.getCollectedKeys()
                + "/" + dungeonManager.getTotalKeys() + ")";
        textRenderer.renderer(label, x, y, 0.3f, 1f, 0.3f);
        return y + textRenderer.getTextHeight(label);
    }

    private static void renderPauseMenu(int w, int h) {
        buttonCerrarMenu.draw(w, h, window.getWindowHandle());
        buttonRestartTime.draw(w, h, window.getWindowHandle());
        buttonSalir.draw(w, h, window.getWindowHandle());

        float buttonsTop = (h - menuTotalHeight()) / 2f;
        centeredText(w, "Menu", buttonsTop - 70f, 1f, 1f, 1f);
        // En el menú (pausa o derrota) se indica el siguiente nivel y se puede
        // continuar cargaándolo rápido.
        centeredText(w, "Siguiente nivel: " + nextLevelName(), buttonsTop - 40f, 0.86f, 0.35f, 0.1f);

        // FPS en la esquina superior derecha.
        String fpsText = "FPS: " + currentFPS;
        textRenderer.renderer(fpsText, w - textRenderer.getTextWidth(fpsText) - 20f, 20f, 0.3f, 1f, 0.3f);

        buttonReiniciar.draw(w, h, window.getWindowHandle());

        // La pista de controles se parte en dos lineas porque en una sola no
        // cabia en pantallas estrechas y se salia del borde. Aqui si aparece la
        // M, que es donde se puede echar la mano sin que estorbe jugando.
        centeredText(w, "WASD - Moverse | Q / E - Girar camara | Mouse - Mirar | Shift - Correr",
                h * 0.82f, 1f, 1f, 1f);
        centeredText(w, "Ctrl - Agacharse | P - Menu | M - Libera raton | 8 / 9 / 0 - Efectos",
                h * 0.86f, 0.8f, 0.8f, 0.8f);
    }

    private static void renderTitleScreen(int w, int h) {
        // El logo sustituye al titulo en texto: la altura sale de la proporción
        // original de la imagen, asi que no se deforma al redimensionar.
        float logoW = w * GameConfig.TITLE_LOGO_WIDTH_RATIO;
        float logoY = h * GameConfig.TITLE_LOGO_TOP_RATIO;
        float logoH = titleLogo.heightForWidth(logoW);
        titleLogo.drawCentered(logoY, logoW, w, h, 1f);

        centeredText(w, "Encuentra los artefactos antes de que se acabe el tiempo",
                logoY + logoH + 18f, 1f, 1f, 1f);

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
        centeredText(w, "WASD - Moverse | Mouse - Rotar cam | P - Menu | Shift - Correr",
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

    private static void cleanup() {
        dungeonManager.cleanup();
        skyRenderer.cleanup();
        wallShader.cleanup();
        propShader.cleanup();
        DungeonManager.clearPropCache();
        visionManager.cleanup();
        uiBatch.cleanup();
        titleLogo.cleanup();
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