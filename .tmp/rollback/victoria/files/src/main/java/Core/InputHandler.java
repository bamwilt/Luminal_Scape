package Core;

import org.lwjgl.glfw.GLFW;

/**
 * Lee el teclado y el raton y despacha cada pulsacion a una accion del juego.
 *
 * <p>Es la unica clase que consulta GLFW para la partida. Antes esto estaba en
 * {@link GameLoop#handleInput()}, mezclado con el estado de la partida, los
 * botones y la lista de niveles; aqui solo se detecta el flanco (tecla que pasa
 * de no estar a estar) y se avisa por {@link Actions}.
 *
 * <p>Las teclas con estado (para no repetir la accion mientras se mantienen
 * pulsadas) viven aqui: son memoria de la entrada, no de la partida.
 */
final class InputHandler {

    /**
     * Lo que la entrada puede pedirle a la partida.
     *
     * <p>Se pasa en el constructor para que esta clase no sepa como se juega,
     * solo que teclas producen que. Las acciones que no disparan nada por si
     * mismas se dejan fuera a proposito.
     */
    interface Actions {
        void startGame();

        void pauseGame();

        void resumeGame();

        /** Alterna la partida congelada con M (congela/descongela sin menu). */
        void toggleFreeze();

        void stepLevel(int delta);

        void toggleTestLevel();

        /** Alterna el modo desarrollador (tecla T). */
        void toggleDeveloperMode();

        /** Atajos 8/9/0 de los efectos de vision (solo en partida, modo desarrollador). */
        void handleNumKeys(long handle);

        /** Cierra la pantalla de Configuracion y vuelve de donde se vino. */
        void closeConfig();
    }

    private final GameWindow window;
    private final MenuScreens menus;
    private final Actions actions;

    // Estado de entrada: el flanco de cada tecla (true mientras esta pulsada).
    private boolean menuKeyPressed = false;
    private boolean escKeyPressed = false;
    private boolean enterKeyPressed = false;
    private boolean leftKeyPressed = false;
    private boolean rightKeyPressed = false;
    private boolean uKeyPressed = false;

    InputHandler(GameWindow window, MenuScreens menus, Actions actions) {
        this.window = window;
        this.menus = menus;
        this.actions = actions;
    }

    void handle(GameState state) {
        long handle = window.getWindowHandle();

        // M congela la partida y suelta el raton, sin abrir el menu: el nivel se ve
        // entero y al volver a pulsarlo sigue donde estaba. La pausa con menu es
        // la P de abajo.
        boolean mPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (mPressed && !menuKeyPressed && state == GameState.PLAYING) {
            actions.toggleFreeze();
        }
        menuKeyPressed = mPressed;

        // P pausa/libera el mouse (y reanuda). ESC esta reservado.
        boolean pPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_P) == GLFW.GLFW_PRESS;
        if (pPressed && !escKeyPressed) {
            if (state == GameState.PLAYING) {
                actions.pauseGame();
            } else if (state == GameState.PAUSED) {
                actions.resumeGame();
            } else if (state == GameState.CONFIG) {
                actions.closeConfig();
            }
        }
        escKeyPressed = pPressed;

        // U alterna test level + developer mode (funciona en CUALQUIER estado)
        boolean uPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_U) == GLFW.GLFW_PRESS;
        if (uPressed && !uKeyPressed) {
            actions.toggleTestLevel();
            actions.toggleDeveloperMode();
        }
        uKeyPressed = uPressed;

        // handleNumKeys ahora gestiona modo desarrollador (1-4) y efectos (8,9,0)
        // se llama en todos los estados donde tiene sentido; la función verifica developerMode.
        switch (state) {
            case PLAYING:
                actions.handleNumKeys(handle);
                break;
            case PAUSED:
                actions.handleNumKeys(handle);
                menus.handlePausedMouse(handle);
                break;
            case TITLE:
                actions.handleNumKeys(handle);
                handleTitle(handle);
                break;
            case CONFIG:
                actions.handleNumKeys(handle);
                menus.handleConfigMouse(handle);
                break;
            default:
                break;
        }
    }

    private void handleTitle(long handle) {
        boolean enterPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        if (enterPressed && !enterKeyPressed) {
            actions.startGame();
        }
        enterKeyPressed = enterPressed;

        menus.handleTitleMouse(handle);

        boolean leftPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
        if (leftPressed && !leftKeyPressed) {
            actions.stepLevel(-1);
        }
        leftKeyPressed = leftPressed;

        boolean rightPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;
        if (rightPressed && !rightKeyPressed) {
            actions.stepLevel(1);
        }
        rightKeyPressed = rightPressed;


    }
}
