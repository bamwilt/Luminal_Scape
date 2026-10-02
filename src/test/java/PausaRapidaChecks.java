import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Comprobaciones de la pausa rapida: la M congela la partida y suelta el raton
 * SIN abrir el menu, y al volver a pulsarla la partida sigue donde estaba.
 *
 * <p>Va en un {@code main} a proposito, como las otras regresiones del repo: no
 * hay JUnit y esto solo necesita el codigo compilado. Lo que se comprueba aqui
 * es la regla (que se congela lo que toca, que el menu no aparece y que la
 * pausa de verdad sigue en la P), porque abrir una ventana y soltar el raton no
 * se puede ejecutar sin GLFW; eso lo cubre el smoke con GL.
 */
public final class PausaRapidaChecks {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        laMFreezaYLaPAbreElMenu();
        congeladoNoCambiaLaPantalla();
        noSeSimulaNadaCongelado();
        elMenuSigueEnLaP();

        System.out.println(fallos == 0
                ? "OK: todas las comprobaciones de la pausa rapida pasan."
                : "FALLOS: " + fallos);
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ---- M congela, P abre el menu ---------------------------------------
    private static void laMFreezaYLaPAbreElMenu() throws Exception {
        System.out.println("== M congela, P abre el menu ==");
        String main = leer();

        ok(main.contains("private static boolean frozen = false;"),
                "hay un estado de congelado propio, separado del estado de pausa");
        ok(main.contains("private static void freezeGame()"), "congelar tiene su propia funcion");
        ok(main.contains("private static void unfreezeGame()"), "y tambien quitarla");

        // La M tiene que alternar entre los dos, no llamar a la pausa.
        int m = main.indexOf("GLFW_KEY_M");
        ok(m > 0, "la M se sigue leyendo");
        String bloqueM = main.substring(m, main.indexOf("// P pausa", m));
        ok(bloqueM.contains("frozen"), "la M decide con el estado de congelado");
        ok(!bloqueM.contains("pauseGame()"), "y no llama a la pausa: no aparece el menu");

        // El raton: al congelar se suelta, al volver se fija.
        String freeze = main.substring(main.indexOf("private static void freezeGame()"),
                main.indexOf("private static void unfreezeGame()"));
        ok(freeze.contains("gameTimer.pause()"), "congelar para el tiempo");
        ok(freeze.contains("mouseController.setCaptured(false)"), "y suelta el raton");
        ok(!freeze.contains("state = GameState."), "congelar no cambia el estado de la partida");

        String unfreeze = main.substring(main.indexOf("private static void unfreezeGame()"),
                main.indexOf("private static void pauseGame()"));
        ok(unfreeze.contains("gameTimer.resume()"), "al volver el tiempo sigue donde estaba");
        ok(unfreeze.contains("mouseController.setCaptured(true)"), "y el raton se vuelve a fijar");
        ok(unfreeze.contains("frozen = false"), "el congelado se quita al volver");
        // Si el tiempo se acababa justo al congelar, al volver se pasa de nivel
        // en vez de dejar una partida perdida congelada.
        ok(unfreeze.indexOf("isFinished()") > 0
                        && unfreeze.indexOf("isFinished()") < unfreeze.indexOf("stepLevel(1)"),
                "al volver con el tiempo ya acabado se pasa al siguiente nivel");
    }

    // ---- Congelado se ve igual que jugando ---------------------------------
    private static void congeladoNoCambiaLaPantalla() throws Exception {
        System.out.println("== congelado no cambia la pantalla ==");
        String main = leer();

        // El menu se dibuja con el estado PAUSED. Como congelar no cambia el
        // estado, el menu no puede aparecer por la via de congelar. El segundo
        // switch de renderUI es el que decide que se pinta encima del escenario.
        int primerSwitch = main.indexOf("switch (state) {", main.indexOf("private static void renderUI()"));
        int switchRender = main.indexOf("switch (state) {", primerSwitch + 1);
        ok(switchRender > primerSwitch, "renderUI tiene el switch de las capas y el del contenido");
        String render = main.substring(switchRender, main.indexOf("default:", switchRender));
        ok(render.contains("case PAUSED:") && render.contains("renderPauseMenu(w, h);"),
                "el menu sigue dibujandose solo en PAUSED");

        ok(main.contains("case PLAYING:") && main.contains("renderPlayingHud(w, h);"),
                "congelado se dibuja como PLAYING: el nivel se ve entero");

        int hud = main.indexOf("private static void renderPlayingHud");
        String cuerpo = main.substring(hud, main.indexOf("private static void renderPauseMenu"));
        ok(cuerpo.contains("\"P para el menu\""), "al jugar solo se anuncia la P");
        ok(!cuerpo.contains("PAUSA RAPIDA") && !cuerpo.contains("Pulsa M para volver"),
                "congelado no sale ningun aviso en pantalla");
        ok(!cuerpo.contains("M para liberar") && !cuerpo.contains("M para seguir"),
                "la M no se anuncia en el HUD: es un atajo propio");
        ok(!cuerpo.contains("renderPauseMenu"), "y el menu no se dibuja nunca desde el HUD");
    }

    // ---- Congelado no simula nada ------------------------------------------
    private static void noSeSimulaNadaCongelado() throws Exception {
        System.out.println("== congelado no simula nada ==");
        String main = leer();

        int update = main.indexOf("private static void update()");
        String cuerpo = main.substring(update, main.indexOf("private static void mainLoop()"));
        ok(cuerpo.contains("state == GameState.PLAYING && !frozen"),
                "la simulacion solo corre en partida y sin congelar");
        ok(cuerpo.contains("!frozen") && cuerpo.indexOf("player.getVelocity()") > 0
                        && cuerpo.indexOf("!frozen") < cuerpo.indexOf("player.getVelocity()"),
                "los pasos tampoco suenan congelado");

        // Los tres llamados que mueven el juego tienen que quedar dentro del
        // bloque congelado: camara, jugador y recogida.
        int ifNoFrozna = cuerpo.indexOf("state == GameState.PLAYING && !frozen");
        for (String que : new String[] { "mouseController.update()", "inputPlayer.update(deltaTime)",
                "dungeonManager.update(player.getPosition(), deltaTime)" }) {
            int donde = cuerpo.indexOf(que);
            ok(donde > ifNoFrozna, que + " no se llama congelado");
        }

        // Abrir el menu desde una partida congelada manda sobre el congelado.
        int pause = main.indexOf("private static void pauseGame()");
        String cuerpoPause = main.substring(pause, main.indexOf("\n    }", pause));
        ok(cuerpoPause.contains("frozen = false;"),
                "abrir el menu limpia el congelado: al reanudar se vuelve a partida normal");

        int start = main.indexOf("private static void startGame()");
        String cuerpoStart = main.substring(start, main.indexOf("\n    }", start));
        ok(cuerpoStart.contains("frozen = false;"),
                "un nivel nuevo nunca arranca congelado");
    }

    // ---- La pausa de menu sigue en la P -----------------------------------
    private static void elMenuSigueEnLaP() throws Exception {
        System.out.println("== la pausa de menu sigue en la P ==");
        String main = leer();
        int p = main.indexOf("GLFW_KEY_P");
        ok(p > 0, "la P se sigue leyendo");
        String bloqueP = main.substring(p, main.indexOf("switch (state)", p));
        ok(bloqueP.contains("pauseGame()"), "la P abre la pausa con menu");
        ok(bloqueP.contains("resumeGame()"), "y la reanuda");

        // La M se explica en el menu de pausa, que es donde se la puede usar sin que
// estorbe, pero no en el HUD ni en el titulo.
        int menu = main.indexOf("private static void renderPauseMenu");
        String cuerpoMenu = main.substring(menu, main.indexOf("private static void renderTitleScreen"));
        ok(cuerpoMenu.contains("M - Libera raton"), "el menu de pausa explica la M");

        int titulo = main.indexOf("private static void renderTitleScreen");
        String cuerpoTitulo = main.substring(titulo, main.indexOf("private static void drawToast"));
        ok(!cuerpoTitulo.contains("M - Libera"), "el titulo no anuncia la M");

        ok(!main.contains("M / P - Menu") && !main.contains("M/P - Menu"),
                "no queda ninguna pista que siga diciendo que M abre el menu");
        ok(!main.contains("Presiona M para abrir el menu"), "ni el aviso viejo del HUD");
    }

    // ---- Utilidades -------------------------------------------------------
    private static String leer() throws Exception {
        return Files.readString(Paths.get("src/main/java/main/Main.java"));
    }

    private static void ok(boolean condicion, String que) {
        System.out.println((condicion ? "  OK    " : "  FALLA ") + que);
        if (!condicion) {
            fallos++;
        }
    }

    private PausaRapidaChecks() {
    }
}