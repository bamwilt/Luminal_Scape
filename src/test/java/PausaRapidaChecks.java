import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Comprobaciones de la pausa rapida: la M congela la partida y suelta el raton,
 * sin abrir el menu, y al volver a pulsarla sigue donde estaba. La pausa con
 * menu sigue en la P.
 *
 * <p>Va en un {@code main} a proposito, como las otras regresiones del repo: no
 * hay JUnit y estas comprobaciones solo leen el texto de las clases. Tras sacar
 * la entrada, el HUD y los menus de {@link GameLoop} en clases propias, cada
 * comprobacion mira el archivo donde vive ahora su trozo de codigo.
 */
public final class PausaRapidaChecks {

    private static final String CORE = "src/main/java/Core/";

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
        String main = leer("GameLoop.java");

        ok(main.contains("private static boolean frozen = false;"),
                "hay un estado de congelado propio, separado del estado de pausa");
        ok(main.contains("private static void freezeGame()"), "congelar tiene su propia funcion");
        ok(main.contains("private static void unfreezeGame()"), "y tambien quitarla");

        // La M se lee en InputHandler y tiene que alternar el congelado, no
        // llamar a la pausa con menu.
        String input = leer("InputHandler.java");
        int m = input.indexOf("GLFW_KEY_M");
        ok(m > 0, "la M se sigue leyendo");
        String bloqueM = input.substring(m, input.indexOf("// P pausa", m));
        ok(bloqueM.contains("toggleFreeze()"), "la M alterna el estado de congelado");
        ok(!bloqueM.contains("pauseGame()"), "y no llama a la pausa: no aparece el menu");

        // El raton: al congelar se suelta, al volver se fija.
        String freeze = main.substring(main.indexOf("private static void freezeGame()"),
                main.indexOf("private static void unfreezeGame()"));
        ok(freeze.contains("gameTimer.pause()"), "congelar para el tiempo");
        ok(freeze.contains("mouseController.setCaptured(false)"), "y suelta el raton");
        ok(!freeze.contains("state = GameState."), "congelar no cambia el estado de la partida");

        String unfreeze = main.substring(main.indexOf("private static void unfreezeGame()"),
                main.indexOf("private static void toggleFreeze()"));
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
        String main = leer("GameLoop.java");

        // El menu se dibuja con el estado PAUSED. Como congelar no cambia el
        // estado, el menu no puede aparecer por la via de congelar. El segundo
        // switch de renderUI es el que decide que se pinta encima del escenario.
        int primerSwitch = main.indexOf("switch (state) {", main.indexOf("private static void renderUI()"));
        int switchRender = main.indexOf("switch (state) {", primerSwitch + 1);
        ok(switchRender > primerSwitch, "renderUI tiene el switch de las capas y el del contenido");
        String render = main.substring(switchRender, main.indexOf("default:", switchRender));
        ok(render.contains("case PAUSED:") && render.contains("menus.renderPause("),
                "el menu sigue dibujandose solo en PAUSED");

        ok(main.contains("case PLAYING:") && main.contains("hud.render(w, h,"),
                "congelado se dibuja como PLAYING: el nivel se ve entero");

        String hud = leer("HudRenderer.java");
        ok(hud.contains("\"P para el menu\""), "al jugar solo se anuncia la P");
        ok(!hud.contains("PAUSA RAPIDA") && !hud.contains("Pulsa M para volver"),
                "congelado no sale ningun aviso en pantalla");
        ok(!hud.contains("M para liberar") && !hud.contains("M para seguir"),
                "la M no se anuncia en el HUD: es un atajo propio");
        ok(!hud.contains("renderPause"), "y el menu no se dibuja nunca desde el HUD");
    }

    // ---- Congelado no simula nada ------------------------------------------
    private static void noSeSimulaNadaCongelado() throws Exception {
        System.out.println("== congelado no simula nada ==");
        String main = leer("GameLoop.java");

        int update = main.indexOf("private static void update()");
        String cuerpo = main.substring(update, main.indexOf("private static void mainLoop()"));
        ok(cuerpo.contains("state == GameState.PLAYING && !frozen"),
                "la simulacion solo corre en partida y sin congelar");
        ok(cuerpo.contains("!frozen") && cuerpo.indexOf("player.getVelocity()") > 0
                        && cuerpo.indexOf("!frozen") < cuerpo.indexOf("player.getVelocity()"),
                "los pasos tampoco suenan congelado");

        // Los tres llamados que mueven el juego tienen que quedar dentro del
        // bloque congelado: camara, player y recogida.
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
        String input = leer("InputHandler.java");
        int p = input.indexOf("GLFW_KEY_P");
        ok(p > 0, "la P se sigue leyendo");
        String bloqueP = input.substring(p, input.indexOf("switch (state)", p));
        ok(bloqueP.contains("pauseGame()"), "la P abre la pausa con menu");
        ok(bloqueP.contains("resumeGame()"), "y la reanuda");

        // Las pistas de teclado se han movido a la pantalla de Configuracion: el
        // menu de pausa y el titulo ya no las llevan. La M sigue siendo un atajo
        // del juego, pero solo se anuncia en la lista de controles.
        String menus = leer("MenuScreens.java");
        int menu = menus.indexOf("void renderPause(");
        String cuerpoMenu = menus.substring(menu, menus.indexOf("void renderTitle("));
        ok(!cuerpoMenu.contains("M - Libera raton"),
                "el menu de pausa ya no lleva las pistas de teclado");

        int controles = menus.indexOf("CONTROLES");
        ok(controles > 0, "la lista de controles sigue declarada");
        String listaControles = menus.substring(controles, menus.indexOf("};", controles));
        ok(listaControles.contains("M - Libera raton"),
                "la M se anuncia en la lista de controles");
        int config = menus.indexOf("void renderConfig(");
        String cuerpoConfig = menus.substring(config, menus.indexOf("void cleanup("));
        ok(cuerpoConfig.contains("for (String linea : CONTROLES)"),
                "y esas pistas se dibujan solo en la pantalla de Configuracion");

        int titulo = menus.indexOf("void renderTitle(");
        String cuerpoTitulo = menus.substring(titulo, menus.indexOf("private void centeredText"));
        ok(!cuerpoTitulo.contains("M - Libera"), "el titulo no anuncia la M");

        String todo = leer("GameLoop.java") + leer("InputHandler.java") + leer("MenuScreens.java")
                + leer("HudRenderer.java");
        ok(!todo.contains("M / P - Menu") && !todo.contains("M/P - Menu"),
                "no queda ninguna pista que siga diciendo que M abre el menu");
        ok(!todo.contains("Presiona M para abrir el menu"), "ni el aviso viejo del HUD");
        ok(!todo.contains("TITLE_PISTA"), "y no quedan las pistas del titulo");
    }

    // ---- Utilidades -------------------------------------------------------
    private static String leer(String archivo) throws Exception {
        return Files.readString(Paths.get(CORE + archivo));
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
