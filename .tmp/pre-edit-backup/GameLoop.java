package Core;

import Audio.AudioSystem;
import Player.InputPlayer;
import Player.MouseController;
import Player.Player;
import Render2D.Minimap;
import Render2D.QuadBatch;
import Render2D.TextRenderer;
import Render2D.VisionManager;
import Render3D.TextureLoader;
import Render3D.collision.CollisionManager;
import Render3D.item.Item;
import Render3D.item.ItemMap;
import Render3D.item.ItemTime;
import Render3D.map.DungeonManager;
import Render3D.map.LevelTypePreset;
import Render3D.map.MapConfig;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class GameLoop {

    private static Countdown gameTimer;

    private static Player player;
    private static InputPlayer inputPlayer;
    private static MouseController mouseController;
    private static SceneRenderer scene;
    private static float elapsedTime = 0f;
    private static GameWindow window;
    private static TextRenderer textRenderer;
    private static CollisionManager collisionManager;
    private static AudioSystem audioSystem;
    private static DungeonManager dungeonManager;

    /** Qué nivel y qué dificultad están elegidos, y el nivel cargado. */
    private static LevelSession levelSession;
    /** Botones, rótulos y dibujo de las pantallas de título y pausa. */
    private static MenuScreens menus;
    /** Contador de llaves, tiempo y avisos de la partida. */
    private static HudRenderer hud;
    /** Lectura del teclado y el ratón. */
    private static InputHandler inputHandler;

    private static VisionManager visionManager;
    private static QuadBatch loteUi;
    private static Minimap minimap;

    private static GameState state = GameState.TITLE;

    /** Pantalla desde la que se abrio Configuracion, para volver a ella. */
    private static GameState configBackState = GameState.TITLE;

    /** Segundos que da un item según la dificultad (Difícil: -3s). */
    private static int itemBonusSeconds() {
        return levelSession.isDificil()
                ? ConfigGlobal.SEGUNDOS_POR_ITEM - 3
                : ConfigGlobal.SEGUNDOS_POR_ITEM;
    }

    /**
     * Distancia de vista del nivel, con la dificultad por encima.
     *
     * <p>En Dificil manda {@link MapConfig#VIEW_DISTANCE_LOW} aunque el nivel
     * pida mas: la niebla es el handicap, y si el nivel pudiera abrirla el modo
     * no seria distinto de Normal. Solo afecta a lo que ve el player; ni el
     * mapa ni la colision cambian.
     */
    private static float viewDistance() {
        return levelSession.isDificil()
                ? MapConfig.VIEW_DISTANCE_LOW
                : levelSession.data().getViewDistance();
    }

    // Fin de la lista de niveles: muestra "END" y vuelve al título.
    private static float endStartTime = 0f;
    private static final float END_TO_TITLE_DELAY = 3.5f;

    private static int wallTexture;
    private static int floorTexture;
    private static int ceilingTexture;
    private static int lightCeilingTexture;
    private static String loadedFloorTexture;
    private static String loadedCeilingTexture;
    private static String loadedWallTexture;
    private static String loadedLightCeilingTexture;

    /**
     * Partida CONGELADA con M: el juego se para y suelta el raton, pero no sale
     * el menu.
     *
     * <p>Es distinto de la pausa a proposito. La pausa (P) es un estado del
     * juego con su propia pantalla y sus botones; aqui el estado sigue siendo
     * PLAYING y lo unico que se apaga es la simulacion, asi que el player ve
     * el nivel entero tal cual estaba, sin el fondo oscurecido ni el menu
     * encima. Es lo que hace falta cuando hay que salir a otra cosa con el
     * raton (copiar un texto, mirar una window) sin perder de vista la partida
     * ni acordarse de como iba.
     *
     * <p>No es un estado de {@link GameState} porque no cambia nada de como se
     * dibuja la pantalla: se dibuja exactamente igual que en partida.
     */
    private static boolean frozen = false;

    // Estado de entrada de los atajos de efectos (8/9/0), que solo existen aquí.
    private static boolean key8Prev = false;
    private static boolean key9Prev = false;
    private static boolean key0Prev = false;

    private static int currentFPS = 0;
    private static final FpsCounter fpsCounter = new FpsCounter();

    public static void main(String[] args) {
        initApplication();
        audioSystem = new AudioSystem();
        mainLoop();
        cleanup();
    }

    private static void initApplication() {
        window = new GameWindow(ConfigGlobal.ANCHO_VENTANA, ConfigGlobal.ALTO_VENTANA, ConfigGlobal.TITULO_VENTANA);
        window.init();
        window.toggleFullscreen();
        collisionManager = new CollisionManager();

        player = new Player(collisionManager);
        inputPlayer = new InputPlayer(window.getWindowHandle(), player);
        mouseController = new MouseController(player, window.getWindowHandle());
        mouseController.setCaptured(false);

        textRenderer = new TextRenderer(ConfigGlobal.RUTA_FUENTE, ConfigGlobal.TAMANO_FUENTE);

        // La sesión de nivel se crea antes que los menús, que leen de ella los
        // nombres que muestran los selectores.
        levelSession = new LevelSession();
        menus = new MenuScreens(window, textRenderer, levelSession, new MenuScreens.Actions() {
            @Override
            public void startGame() {
                GameLoop.startGame();
            }

            @Override
            public void resumeGame() {
                GameLoop.resumeGame();
            }

            @Override
            public void restartTime() {
                GameLoop.restartTime();
            }

            @Override
            public void stepLevel(int delta) {
                GameLoop.stepLevel(delta);
            }

            @Override
            public void stepDifficulty(int delta) {
                GameLoop.stepDifficulty(delta);
            }

            @Override
            public void returnToTitle() {
                GameLoop.returnToTitle();
            }

            @Override
            public void setResolution(int ancho, int alto) {
                GameLoop.setResolution(ancho, alto);
            }

            @Override
            public void openConfig() {
                GameLoop.openConfig();
            }

            @Override
            public void closeConfig() {
                GameLoop.closeConfig();
            }

            @Override
            public void setMusicVolume(float volumen) {
                GameLoop.setMusicVolume(volumen);
            }

            @Override
            public void setSfxVolume(float volumen) {
                GameLoop.setSfxVolume(volumen);
            }

            @Override
            public void setBrightness(float valor) {
                GameLoop.setBrightness(valor);
            }

            @Override
            public void setContrast(float valor) {
                GameLoop.setContrast(valor);
            }

            @Override
            public void setMouseSensitivity(float recorrido) {
                GameLoop.setMouseSensitivity(recorrido);
            }

            @Override
            public void setMoveSpeed(float recorrido) {
                GameLoop.setMoveSpeed(recorrido);
            }

            @Override
            public void goToTitle() {
                GameLoop.goToTitle();
            }

            @Override
            public void saveConfig() {
                GameLoop.saveConfig();
            }

            @Override
            public void resetConfig() {
                GameLoop.resetConfig();
            }
        });
        hud = new HudRenderer(textRenderer);
        inputHandler = new InputHandler(window, menus, new InputHandler.Actions() {
            @Override
            public void startGame() {
                GameLoop.startGame();
            }

            @Override
            public void pauseGame() {
                GameLoop.pauseGame();
            }

            @Override
            public void resumeGame() {
                GameLoop.resumeGame();
            }

            @Override
            public void toggleFreeze() {
                GameLoop.toggleFreeze();
            }

            @Override
            public void stepLevel(int delta) {
                GameLoop.stepLevel(delta);
            }

            @Override
            public void toggleTestLevel() {
                GameLoop.toggleTestLevel();
            }

            @Override
            public void handleNumKeys(long handle) {
                GameLoop.handleNumKeys(handle);
            }

            @Override
            public void closeConfig() {
                GameLoop.closeConfig();
            }
        });

        textRenderer.setProjection(menus.virtualW(), menus.virtualH());

        initDungeon();
        scene = new SceneRenderer(window, levelSession, dungeonManager);
        initVision();
        initUiLayer();
        menus.refreshLabels();

        window.setOnResizeCallback((w2, h2) -> {
            menus.updateUiScale(w2, h2);
            textRenderer.setProjection(menus.virtualW(), menus.virtualH());
            visionManager.setProjection(menus.virtualW(), menus.virtualH());
            loteUi.setProjection(menus.virtualW(), menus.virtualH());
            menus.layout();
        });

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void initDungeon() {
        loadCurrentLevel();
        dungeonManager = new DungeonManager(levelSession.data().toRows(), wallTexture, floorTexture,
                ceilingTexture, lightCeilingTexture);
        dungeonManager.registerCollisions(collisionManager);
    }

    /**
     * Lee el nivel seleccionado y carga las texturas de ambiente que dicta su
     * preset. Las texturas se cachean por ruta porque {@link TextureLoader}
     * crea un id de OpenGL por llamada: recargarlas en cada cambio de nivel
     * fugaria memoria de GPU.
     */
    private static void loadCurrentLevel() {
        levelSession.reload();
        LevelTypePreset preset = levelSession.data().getPreset();

        String floorPath = preset.getFloorTexture() != null
                ? preset.getFloorTexture() : ConfigGlobal.TEXTURA_SUELO;
        if (!floorPath.equals(loadedFloorTexture)) {
            floorTexture = TextureLoader.loadTexture(floorPath);
            loadedFloorTexture = floorPath;
        }

        String ceilingPath = preset.getCeilingTexture() != null
                ? preset.getCeilingTexture() : ConfigGlobal.TEXTURA_TECHO;
        if (!ceilingPath.equals(loadedCeilingTexture)) {
            ceilingTexture = TextureLoader.loadTexture(ceilingPath);
            loadedCeilingTexture = ceilingPath;
        }

        // El muro tambien es del preset, con el mismo cacheo: sin esto, pasar
        // de los backrooms al pasillo dejaba el muro amarillo del nivel
        // anterior pegado al hormigon nuevo.
        String wallPath = preset.getWallTexture() != null
                ? preset.getWallTexture() : ConfigGlobal.TEXTURA_PARED;
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

    /**
     * Cambia el nivel seleccionado (adelante/atrás) y recarga su layout.
     *
     * <p>La selección la guarda {@link LevelSession}; aquí se recargan las
     * texturas del preset y se recolocan los rótulos del título.
     */
    private static void stepLevel(int delta) {
        levelSession.stepLevel(delta);
        loadCurrentLevel();
        refreshSelectorLabels();
    }

    /** Cambia la dificultad seleccionada (circular). */
    private static void stepDifficulty(int delta) {
        levelSession.stepDifficulty(delta);
        refreshSelectorLabels();
    }

    /**
     * Desbloquea o vuelve a esconder el nivel de pruebas (tecla T en el título).
     */
    private static void toggleTestLevel() {
        levelSession.toggleTestLevel();
        loadCurrentLevel();
        refreshSelectorLabels();
    }

    /**
     * Vuelca el nivel y la dificultad en sus rótulos y recoloca la cuadrícula.
     *
     * <p>El texto del selector es parte de su celda, así que al cambiar de texto
     * cambia de ancho y hay que volver a medir la fila. Sin esta llamada, elegir
     * un nombre de nivel más largo dejaba el texto rozando las flechas.
     */
    private static void refreshSelectorLabels() {
        menus.refreshLabels();
    }

    private static void initVision() {
        visionManager = new VisionManager(ConfigGlobal.RUTA_FUENTE, ConfigGlobal.TAMANO_FUENTE_VISION);
        visionManager.setProjection(menus.virtualW(), menus.virtualH());
    }

    private static void initUiLayer() {
        loteUi = new QuadBatch();
        loteUi.setProjection(menus.virtualW(), menus.virtualH());
        minimap = new Minimap(dungeonManager);
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
        if (levelSession.isFacil()) {
            // En Facil se empieza con el plano en la mano, asi que el '☑' deja
            // tiempo en vez de mapa: un mapa que no revela nada no vale nada.
            minimap.revealAll();
        }
        dungeonManager.setMapItemGivesTime(levelSession.isFacil());
        loadCurrentLevel();
        dungeonManager.applyLayout(levelSession.data().toRows(), wallTexture, floorTexture, ceilingTexture,
                lightCeilingTexture);
        dungeonManager.registerCollisions(collisionManager);

        gameTimer = new Countdown(levelSession.timeSeconds());
        gameTimer.start();
        // Un nivel nuevo nunca arranca congelado: si se entra aqui desde la
        // pausa rapida, el raton tiene que volver a la partida.
        frozen = false;
        Vector3f spawn = dungeonManager.getSpawnPosition();
        if (spawn != null) {
            player.setPosition(spawn);
        } else {
            player.getCamara().setPosition(new Vector3f(
                    ConfigGlobal.CAMARA_X,
                    ConfigGlobal.CAMARA_Y,
                    ConfigGlobal.CAMARA_Z));
        }
        player.getCamara().setYaw(dungeonManager.getSpawnYaw());

        visionManager.showMessage(levelSession.name(), 1f, 0.86f, 0.35f, 0.75f, 2.5f);

        state = GameState.PLAYING;
        mouseController.setCaptured(true);
    }

    /**
     * Congela la partida y suelta el raton, sin abrir el menu.
     *
     * <p>Se para el tiempo y se deja de mover la camara y el player, asi que al
     * volver la partida sigue exactamente donde estaba. El raton se suelta para
     * que el player pueda salir a otra window; la pantalla no cambia, porque el
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
        // Sin este caso, si el player congela justo cuando el tiempo se
        // acababa, al volver seguiria con la partida perdida congelada y el
        // "SE ACABO EL TIEMPO" saltaria tarde. Con el, reanudar lleva al
        // siguiente nivel igual que hace "Continuar" tras la derrota.
        if (gameTimer != null && gameTimer.isFinished()) {
            frozen = false;
            if (levelSession.isLastLevel()) {
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

    /** Alterna el congelado rápido de la M (ver {@link #frozen}). */
    private static void toggleFreeze() {
        if (frozen) {
            unfreezeGame();
        } else {
            freezeGame();
        }
    }

    /** Reinicia solo el tiempo del nivel en curso (botón "Reiniciar Tiempo"). */
    private static void restartTime() {
        gameTimer = new Countdown(levelSession.timeSeconds());
        gameTimer.start();
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
            if (levelSession.isLastLevel()) {
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
        // mapa del anterior revelado y el player tendria delante el plano del
        // nivel que acaba de superar.
        minimap.reset();
        // Sin pantalla de victoria: se carga directamente el siguiente nivel.
        if (levelSession.isLastLevel()) {
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
        // igual: si el player sigue con el tiempo agotado, el plano revelado
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
        levelSession.goToFirst();
        mouseController.setCaptured(false);
    }

    /**
     * Vuelve al titulo desde el menu de pausa.
     *
     * <p>A diferencia de {@link #goToTitle()} (que salta al terminar todos los
     * niveles), aqui se conserva el nivel y la dificultad que el jugador tenia
     * elegidos, para que al volver al principal no pierda la seleccion.
     */
    private static void returnToTitle() {
        state = GameState.TITLE;
        mouseController.setCaptured(false);
    }

    /** Aplica la resolucion elegida en el desplegable del menu de pausa. */
    private static void setResolution(int ancho, int alto) {
        window.setWindowSize(ancho, alto);
    }

    /** Abre Configuracion recordando de donde se venia (titulo o pausa). */
    private static void openConfig() {
        if (state == GameState.TITLE || state == GameState.PAUSED) {
            configBackState = state;
            state = GameState.CONFIG;
            mouseController.setCaptured(false);
        }
    }

    /** Vuelve de Configuracion a la pantalla desde la que se abrio. */
    private static void closeConfig() {
        if (state == GameState.CONFIG) {
            state = configBackState;
        }
    }

    /** Volumen del fondo musical (0..1) desde el slider de Configuracion. */
    private static void setMusicVolume(float volumen) {
        audioSystem.setMusicVolume(volumen);
    }

    /** Volumen de los efectos (0..1) desde el slider de Configuracion. */
    private static void setSfxVolume(float volumen) {
        audioSystem.setSfxVolume(volumen);
    }

    /** Brillo: el slider 0..1 se centra en 0.5, que es el neutro (1.0). */
    private static void setBrightness(float valor) {
        VideoSettings.brillo = 0.5f + valor;
    }

    /** Contraste: el slider 0..1 se centra en 0.5, que es el neutro (1.0). */
    private static void setContrast(float valor) {
        VideoSettings.contraste = 0.5f + valor;
    }

    /** Sensibilidad del raton: el recorrido del slider pasa a grados de yaw por pixel. */
    private static void setMouseSensitivity(float recorrido) {
        if (mouseController != null) {
            mouseController.setSensitivity(AjustesJuego.sensibilidad(recorrido));
        }
    }

    /** Velocidad de movimiento: el recorrido del slider pasa a multiplicador. */
    private static void setMoveSpeed(float recorrido) {
        if (player != null) {
            player.setSpeedScale(AjustesJuego.velocidad(recorrido));
        }
    }

    /**
     * "Guardar configuracion": deja fijos los valores que hay ahora mismo en
     * los sliders.
     *
     * <p>Guardar es recordar, no aplicar: los sliders ya han ido aplicando
     * cada cambio mientras el jugador los movia, asi que aqui solo se anotan
     * para que un restablecer sepa a que volver.
     */
    private static void saveConfig() {
        if (menus == null) {
            return;
        }
        AjustesJuego.guardar(
                menus.getSliderMusica(),
                menus.getSliderEfectos(),
                menus.getSliderBrillo(),
                menus.getSliderContraste(),
                menus.getSliderSensibilidad(),
                menus.getSliderVelocidad());
    }

    /**
     * "Restablecer por defecto": vuelve los seis ajustes a su valor de fabrica.
     *
     * <p>Los aplica de verdad (aqui no valia con acordarse) y ademas refresca
     * los sliders, porque si no el jugador seguiria viendo en la pantalla el
     * valor que tenia antes de pulsar.
     */
    private static void resetConfig() {
        if (menus == null) {
            return;
        }
        float[] fabrica = AjustesJuego.cargar();
        setMusicVolume(fabrica[0]);
        setSfxVolume(fabrica[1]);
        setBrightness(fabrica[2]);
        setContrast(fabrica[3]);
        setMouseSensitivity(fabrica[4]);
        setMoveSpeed(fabrica[5]);
        menus.sincronizarSliders(
                fabrica[0], fabrica[1], fabrica[2], fabrica[3], fabrica[4], fabrica[5]);
    }

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

    /** Muestra un aviso al recoger un item (lo dibuja el HUD). */
    private static void showToast(String text) {
        hud.showToast(text, elapsedTime);
    }

    private static void update() {
        float deltaTime = TimeUtils.getDeltaTime();
        elapsedTime += deltaTime;
        // Los pasos solo suenan mientras la partida está activa (y no congelada: si el
        // juego esta parado, el player tampoco esta caminando).
        float playerSpeed = (state == GameState.PLAYING && !frozen
                && gameTimer != null && !gameTimer.isFinished())
                ? player.getVelocity().length() : 0f;
        audioSystem.update(deltaTime, playerSpeed);

        // Con la partida congelada la pantalla se sigue dibujando, pero aqui no se
        // simula nada: ni camara, ni player, ni recogida de items. El tiempo ya
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
        TimeUtils.setFPS(ConfigGlobal.FPS_OBJETIVO);
        window.loop(() -> {
            TimeUtils.update();
            fpsCounter.update(TimeUtils.getDeltaTime());
            currentFPS = fpsCounter.getCurrentFPS();

            clearScreen();
            visionManager.update(TimeUtils.getDeltaTime());
            inputHandler.handle(state);
            update();

            scene.render(player, elapsedTime, viewDistance());
            renderUI();
            visionManager.render();
        });
    }

    private static void clearScreen() {
        // Se limpia con el color del horizonte del nivel, que es el mismo que
        // usa la niebla: cualquier pixel que no cubra ni el cielo ni la
        // geometria queda del color del fondo y no se ve el corte.
        Vector3f horizon = levelSession.data().getPreset().getSkyHorizonColor();
        GL11.glClearColor(horizon.x, horizon.y, horizon.z, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static void renderUI() {
        // La UI se dibuja entera en unidades de interfaz: el factor de scale
        // se recalcula aqui porque la window puede haber cambiado de tamaño
        // sin que haya pasado por el callback de resize.
        menus.updateUiScale(window.getWidth(), window.getHeight());
        int w = menus.virtualW();
        int h = menus.virtualH();

        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Capas de fondo y minimapa (batch único).
        switch (state) {
            case TITLE:
                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.75f);
                break;
            case PAUSED:
                // El minimapa NO se dibuja en pausa. El menú va sobre un fondo
                // plano, y el plano del nivel encima solo estorba (se solapa con
                // los botones y con la pista de controles) y confunde: en pausa
                // el nivel ya esta en marcha o ya se ha perdido, y el mapa se
                // apaga al salir de uno y de otro (ver winGame/loseGame).
                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.5f);
                break;
            case END:
                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.88f);
                break;
            case PLAYING:
                minimap.draw(loteUi, w, h, player.getPosition(), player.getCamara().getYaw());
                break;
            case CONFIG:
                loteUi.addQuad(0, 0, w, h, 0f, 0f, 0f, 0.82f);
                break;
            default:
                break;
        }
        loteUi.render();

        switch (state) {
            case PLAYING:
                hud.render(w, h, gameTimer, levelSession.timeSeconds(),
                        dungeonManager.getCollectedKeys(), dungeonManager.getTotalKeys(), elapsedTime);
                break;
            case PAUSED:
                menus.renderPause(w, h, currentFPS);
                break;
            case TITLE:
                menus.renderTitle(w, h);
                break;
            case CONFIG:
                menus.renderConfig(w, h);
                break;
            default:
                break;
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    private static void cleanup() {
        dungeonManager.cleanup();
        scene.cleanup();
        DungeonManager.clearPropCache();
        visionManager.cleanup();
        loteUi.cleanup();
        menus.cleanup();
        window.cleanup();
        audioSystem.cleanup();
    }

}