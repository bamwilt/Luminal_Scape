package Render3D.map;

import Render2D.Minimap;
import Render2D.UIScale;
import Render3D.item.ItemKey;
import Render3D.item.ItemMap;
import Render3D.item.ItemTime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Comprobaciones de lo añadido al final: el item de mapa '☑', el giro de
 * cámara con Q/E, la interfaz que se adapta a la ventana, el difícil que cierra
 * la niebla y el menú de pausa sin los botones viejos.
 *
 * <p>Va en un {@code main} a propósito, como las otras regresiones del repo: no
 * hay JUnit y estas comprobaciones solo necesitan la clase compilada. Lo que
 * necesita una ventana (recoger un item de verdad, pulsar Q o E, dibujar el
 * menú) no se puede comprobar aquí: eso lo cubre el smoke con GL. Lo que aquí se
 * lee de los archivos es la parte que no tiene forma de ejecutarse sin abrir el
 * juego, como una tecla pulsada.
 */
public final class ItemsMapChecks {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        simboloMapa();
        construccionDelItem();
        minimapa();
        resetPorNivel();
        escalaDeLaInterfaz();
        alcanzable();
        soloLasLlavesPasan();
        giroConTeclas();
        menuDePausaLimpio();
        nivelDificil();
        recursosEnElRepo();

        System.out.println(fallos == 0
                ? "OK: todas las comprobaciones de item de mapa pasan."
                : "FALLOS: " + fallos);
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ---- El símbolo '☑' --------------------------------------------------
    private static void simboloMapa() {
        System.out.println("== simbolo ==");
        MapConfig.SymbolType mapa = MapConfig.SymbolType.fromChar('☑');
        ok(mapa != MapConfig.SymbolType.EMPTY, "el simbolo '☑' esta declarado");
        ok(mapa.isItem(), "'☑' es un item");
        ok(mapa != MapConfig.SymbolType.ITEM, "'☑' no es el artefacto");
        ok(!mapa.isSolid(), "'☑' no bloquea el paso");
        ok(mapa.needsFloor(), "'☑' lleva su propio panel de suelo");
        ok(!mapa.needsCeiling(), "'☑' por si solo no es techo");
        ok(MapConfig.SymbolType.ITEM.isItem(), "'◈' tambien cuenta como item");
        ok(!MapConfig.SymbolType.FLOOR_CEILING.isItem(), "el suelo no es un item");

        // Como el artefacto, toma techo si esta dentro de una zona techada.
        char[][] juntoATecho = { "◫◫".toCharArray(), "◫☑".toCharArray() };
        ok(mapa.needsCeilingIn(juntoATecho, 1, 1), "'☑' toma techo junto a un '◫'");
        char[][] alAireLibre = { "□□".toCharArray(), "□☑".toCharArray() };
        ok(!mapa.needsCeilingIn(alAireLibre, 1, 1), "y no lo inventa en campo abierto");
    }

    // ---- Qué celda suelta qué ---------------------------------------------
    private static void construccionDelItem() {
        System.out.println("== construccion ==");
        MapConfig.SymbolType mapa = MapConfig.SymbolType.MAP_ITEM;
        MapConfig.SymbolType clave = MapConfig.SymbolType.ITEM;

        ok(DungeonManager.ItemKind.of(mapa, false) == DungeonManager.ItemKind.MAPA,
                "el '☑' da mapa en normal y dificil");
        ok(DungeonManager.ItemKind.of(mapa, true) == DungeonManager.ItemKind.TIEMPO,
                "y da tiempo en facil");
        ok(DungeonManager.ItemKind.of(clave, false) == DungeonManager.ItemKind.ARTEFACTO,
                "el '◈' da artefacto siempre");
        ok(DungeonManager.ItemKind.of(clave, true) == DungeonManager.ItemKind.ARTEFACTO,
                "tambien en facil: la dificultad no toca los artefactos");
        ok(DungeonManager.ItemKind.of(MapConfig.SymbolType.FLOOR_CEILING, true)
                == DungeonManager.ItemKind.ARTEFACTO,
                "una celda normal no inventa un item de tiempo");
    }

    // ---- El minimapa ------------------------------------------------------
    private static void minimapa() {
        System.out.println("== minimapa ==");
        Minimap mm = new Minimap(new DungeonManager());
        ok(!mm.isVisible(), "el minimapa arranca apagado");
        ok(!mm.isRevealedAll(), "y sin revelar");

        mm.setVisible(true);
        ok(mm.isVisible() && !mm.isRevealedAll(), "se puede ver sin estar revelado");

        Minimap otro = new Minimap(new DungeonManager());
        otro.revealAll();
        ok(otro.isVisible(), "revelar el mapa lo enciende");
        ok(otro.isRevealedAll(), "y lo deja revelado entero");
    }

    // ---- El mapa no se hereda de un nivel a otro -------------------------
    private static void resetPorNivel() throws Exception {
        System.out.println("== el mapa no pasa de nivel ==");
        Minimap mm = new Minimap(new DungeonManager());
        mm.revealAll();
        ok(mm.isVisible() && mm.isRevealedAll(), "parto de un mapa revelado");

        mm.reset();
        ok(!mm.isVisible(), "reset lo apaga");
        ok(!mm.isRevealedAll(), "y quita lo revelado");

        String main = leer("src/main/java/main/Main.java");
        ok(main.contains("minimap.reset();"), "Main lo llama");
        int arranque = main.indexOf("private static void startGame()");
        int fin = main.indexOf("private static void winGame()", arranque);
        String cuerpo = main.substring(arranque, fin);
        int reset = cuerpo.indexOf("minimap.reset();");
        int reveal = cuerpo.indexOf("minimap.revealAll();");
        ok(reset > 0, "el reset esta dentro de startGame, que es donde se carga cada nivel");
        ok(reveal > reset, "y el revelado de facil va DESPUES, para que no lo pise");
        ok(cuerpo.indexOf("difficulty == Difficulty.FACIL", reveal) > 0,
                "el revelado solo pasa en facil");
    }

    // ---- El mapa de prueba: plano, con su '☑' y alcanzable --------------
    private static void alcanzable() throws Exception {
        System.out.println("== el mapa de prueba ==");
        // Un nivel tiene que poder terminarse con SUS llaves. Como el '☑' no
        // cuenta para el total (ver soloLasLlavesPassen), da igual que lo lleve:
        // lo que no puede pasar es que se quede sin llaves, o con llaves
        // encerradas, porque entonces el contador llega a un numero que el mapa
        // no tiene y el jugador se queda atrapado sin poder acabarlo.
        for (String nivel : new String[] { "level_01.txt", "level_02.txt", "level_03.txt", "test_mapa.txt" }) {
            // Las filas tienen que medir lo mismo. Si no, LevelLoader rellena
            // las cortas con espacios y cada espacio es un EMPTY: sin muro, sin
            // suelo y sin colision. Un muro olvidado se pierde sin avisar.
            int[] anchos = anchosCrudos(nivel);
            boolean recto = true;
            for (int a : anchos) {
                recto &= a == anchos[0];
            }
            ok(recto, nivel + " es un rectangulo: todas sus filas miden " + anchos[0]);

            String[] filasNivel = mapa(nivel);
            ok(contar(filasNivel, '◈') > 0, nivel + " tiene llaves con las que terminarlo");
            boolean[][] alcanzadoNivel = alcanzable(filasNivel);
            for (int r = 0; r < filasNivel.length; r++) {
                for (int c = 0; c < filasNivel[r].length(); c++) {
                    if (filasNivel[r].charAt(c) == '◈') {
                        ok(alcanzadoNivel[r][c],
                                nivel + ": la llave de (" + r + "," + c + ") se puede coger andando");
                    }
                }
            }
        }

        String[] filas = mapa("test_mapa.txt");
        int ancho = filas[0].length();
        boolean recto = true;
        for (String f : filas) {
            recto &= f.length() == ancho;
        }
        ok(recto, "test_mapa es un rectangulo: todas las filas miden " + ancho);
        ok(contar(filas, '☑') == 1, "test_mapa tiene un '☑'");
        ok(contar(filas, '◈') >= 1, "y tambien llaves, para poder pasar el nivel");

        // Plano de verdad: ni barandas, ni aberturas, ni suelo sin techo, ni
        // celdas sueltas. Solo el rectangulo de suelo con su muro exterior.
        String todo = String.join("", filas);
        ok("◰▒□".indexOf(todo.charAt(0)) < 0 && !todo.contains("◰") && !todo.contains("▒")
                        && !todo.contains("□") && !todo.contains("◧") && !todo.contains("▣"),
                "test_mapa es todo plano: solo suelo y muros, sin desniveles ni aberturas");
        boolean muro = true;
        for (int c = 0; c < ancho; c++) {
            muro &= filas[0].charAt(c) != '◫' && filas[filas.length - 1].charAt(c) != '◫';
        }
        for (int r = 0; r < filas.length; r++) {
            muro &= filas[r].charAt(0) != '◫' && filas[r].charAt(ancho - 1) != '◫';
        }
        ok(muro, "y el perimeter esta cerrado, para que no se salga de la sala");

        boolean[][] alcanzado = alcanzable(filas);
        for (int r = 0; r < filas.length; r++) {
            for (int c = 0; c < filas[r].length(); c++) {
                char ch = filas[r].charAt(c);
                if (ch == '☑' || ch == '◈') {
                    ok(alcanzado[r][c], "el '" + ch + "' de (" + r + "," + c + ") se puede coger");
                }
            }
        }
    }

    // ---- Para pasar de nivel solo hacen falta las llaves -----------------
    private static void soloLasLlavesPasan() {
        System.out.println("== solo las llaves hacen pasar el nivel ==");
        // El mapa no puede entrar en el total: si hiciera falta para ganar,
        // un nivel con '☑' amurallado seria imposible de terminar.
        ok(DungeonManager.ItemKind.of(MapConfig.SymbolType.MAP_ITEM, false)
                        == DungeonManager.ItemKind.MAPA,
                "el '☑' es mapa, no llave");
        ok(DungeonManager.ItemKind.of(MapConfig.SymbolType.MAP_ITEM, true)
                        == DungeonManager.ItemKind.TIEMPO,
                "y en facil sigue siendo tiempo, tampoco una llave");
        ok(DungeonManager.ItemKind.of(MapConfig.SymbolType.ITEM, true)
                        == DungeonManager.ItemKind.ARTEFACTO,
                "el '◈' es la unica llave, en cualquier dificultad");

        String dm = leerSilencioso("src/main/java/Render3D/map/DungeonManager.java");
        int creacion = dm.indexOf("totalKeys++;");
        ok(creacion > dm.indexOf("ItemKind.ARTEFACTO") && creacion > dm.indexOf("kind = ItemKind.of"),
                "el total se incrementa solo con la llave");
        ok(dm.contains("recogido instanceof ItemKey") && dm.contains("collectedKeys++;"),
                "y al recogerla: el mapa y el tiempo no suman al contador");

        // Ni siquiera hay un contador de 'todos los items' que incluya al mapa:
        // para el codigo el mapa no es un item mas, es una ayuda que se dibuja.
        ok(!dm.contains("totalItems"),
                "no queda ningun contador de items, asi que el mapa no cuenta en ninguno");
        ok(dm.contains("hasAnyItem"), "lo que se guarda es si hay algo que dibujar");
        ok(dm.contains("private boolean hasAnyItem = false;"),
                "y es un si/no, no un numero");

        String main = leerSilencioso("src/main/java/main/Main.java");
        ok(main.contains("getCollectedKeys() >= dungeonManager.getTotalKeys()"),
                "la victoria se decide con las llaves");
        ok(main.contains("getTotalKeys() > 0"), "y solo si el nivel tiene llaves que recoger");
        ok(!main.contains("getCollectedItems") && !main.contains("getTotalItems"),
                "el HUD, los avisos y la victoria no cuentan con todos los items");
        ok(main.contains("\"llaves:(\"") || main.contains("llaves:("),
                "y el HUD lo dice con llaves, no con items");
    }

    // ---- La interfaz se adapta -------------------------------------------
    private static void escalaDeLaInterfaz() {
        System.out.println("== escala de la interfaz ==");
        ok(Math.abs(UIScale.of(1280, 720) - 1f) < 1e-4f,
                "a la resolucion de referencia la escala es 1");
        ok(UIScale.of(1920, 1080) > 1f, "en pantalla grande la interfaz crece");
        ok(UIScale.of(640, 360) < 1f, "en pantalla pequena se encoge");
        ok(UIScale.of(2560, 720) == UIScale.of(1280, 720),
                "manda el lado que aprieta: una ventana ancha y baja no agranda la IU");
        ok(UIScale.of(1, 1) >= UIScale.MIN_SCALE, "hay tope inferior");
        ok(UIScale.of(0, 0) == 1f, "una ventana sin tamaño no rompe nada");

        ok(UIScale.virtualW(1920, UIScale.of(1920, 1080)) >= UIScale.REF_W,
                "el ancho virtual nunca baja del de referencia");
        ok(UIScale.virtualH(1080, UIScale.of(1920, 1080)) >= UIScale.REF_H,
                "el alto virtual nunca baja del de referencia");
        ok(UIScale.virtualW(640, UIScale.of(640, 360)) > 640,
                "en ventana pequena el ancho virtual es mayor que el real");
    }

    // ---- Q / E ------------------------------------------------------------
    private static void giroConTeclas() throws Exception {
        System.out.println("== Q / E ==");
        ok(MapConfig.KEY_YAW_SPEED > 0f, "la velocidad de giro es positiva");
        ok(MapConfig.KEY_YAW_SPEED <= 360f, "y no da mas de una vuelta por segundo");

        String entrada = leer("src/main/java/Player/InputPlayer.java");
        ok(entrada.contains("GLFW_KEY_Q"), "InputPlayer lee la Q");
        ok(entrada.contains("GLFW_KEY_E"), "InputPlayer lee la E");
        ok(entrada.contains("rotateYaw"), "y gira con rotateYaw");
        ok(entrada.contains("KEY_YAW_SPEED"), "a la velocidad del panel");

        String camara = leer("src/main/java/Player/Camera.java");
        ok(camara.contains("rotateYaw"), "la camara sabe sumar giro");
        ok(camara.contains("setYaw(yaw + grados)"),
                "rotateYaw se apoya en setYaw, que recalcula direccion y vista");
    }

    // ---- El menú de pausa se ha limpiado ---------------------------------
    private static void menuDePausaLimpio() throws Exception {
        System.out.println("== menu de pausa ==");
        String main = leer("src/main/java/main/Main.java");
        ok(!main.contains("buttonMinusFPS"), "no queda el boton -FPS");
        ok(!main.contains("buttonPlusFPS"), "no queda el boton +FPS");
        ok(!main.contains("setFPS(30)") && !main.contains("setFPS(60)"), "ni los FPS que ponian");
        ok(!main.contains("buttonMinimapToggle"), "no queda el boton de minimapa ON/OFF");
        ok(!main.contains("buttonMinimapMinus") && !main.contains("buttonMinimapPlus"),
                "no quedan los botones de tamano del minimapa");
        ok(main.contains("new Button[3]"), "el menu de pausa se queda con 3 botones");
        ok(main.contains("Q / E - Girar camara"), "y la pista de controles menciona Q y E");
    }

    // ---- Difícil cierra la niebla ----------------------------------------
    private static void nivelDificil() throws Exception {
        System.out.println("== dificil ==");
        String main = leer("src/main/java/main/Main.java");
        int inicio = main.indexOf("private static float viewDistance()");
        ok(inicio > 0, "hay una regla unica de distancia de vista");
        String regla = main.substring(inicio, main.indexOf("\n    }", inicio));
        ok(regla.contains("DIFICIL"), "esa regla mira la dificultad");
        ok(regla.contains("VIEW_DISTANCE_LOW"), "y en dificil manda la vista mas corta");
        ok(main.contains("float viewDistance = viewDistance();"),
                "el shader recibe esa regla, no la del nivel");
    }

    // ---- El modelo está en el repo ---------------------------------------
    private static void recursosEnElRepo() {
        System.out.println("== recursos ==");
        ok(Files.exists(Paths.get("src/main/resources", ItemMap.MODEL)),
                "el modelo del mapa esta en recursos (" + ItemMap.MODEL + ")");
        ok(ItemMap.SCALE > 0f, "el mapa tiene escala");
        ok(ItemTime.MODEL.equals(ItemKey.MODEL),
                "el item de tiempo usa el modelo del artefacto");
    }

    // ---- Utilidades -------------------------------------------------------
    /** Mapa del nivel tal cual esta en el archivo, sin rellenar. */
    private static String[] mapa(String nivel) throws Exception {
        String texto = leer("src/main/resources/levels/" + nivel);
        String[] filas = texto.split("map:\n", 2)[1].replaceAll("\\s+$", "").split("\n");
        int ancho = 0;
        for (String f : filas) {
            ancho = Math.max(ancho, f.length());
        }
        String[] rectas = new String[filas.length];
        for (int i = 0; i < filas.length; i++) {
            rectas[i] = filas[i].length() < ancho ? filas[i] + " ".repeat(ancho - filas[i].length()) : filas[i];
        }
        return rectas;
    }

    /**
     * Ancho de todas las filas del mapa, tal cual estan en el archivo.
     *
     * <p>Sin rellenar: {@link LevelLoader} rellena las filas cortas con
     * espacios y {@code MapConfig} convierte ese espacio en {@code EMPTY}, asi
     * que una fila que se olvida de un glifo no da error: se pierde en
     * silencio un muro, un suelo o una ventana. Esto solo mira los anchos.
     */
    private static int[] anchosCrudos(String nivel) throws Exception {
        String texto = leer("src/main/resources/levels/" + nivel);
        String[] filas = texto.split("map:\n", 2)[1].replaceAll("\\s+$", "").split("\n");
        int[] anchos = new int[filas.length];
        for (int i = 0; i < filas.length; i++) {
            anchos[i] = filas[i].length();
        }
        return anchos;
    }

    private static int contar(String[] filas, char simbolo) {
        int n = 0;
        for (String f : filas) {
            for (int i = 0; i < f.length(); i++) {
                if (f.charAt(i) == simbolo) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Celdas a las que se llega desde el spawn, andando.
     *
     * <p>Solo se pisan las celdas con suelo o con baranda: un hueco ('□') se
     * puede andar por encima porque el juego no tiene gravedad, pero no es una
     * ruta que el nivel pretenda, asi que para esto cuenta como pared. Las
     * puertas ('◧') si se pasan. El espacio tambien cuenta como pared, y no
     * solo porque suene raro: {@code MapConfig.SymbolType.fromChar(' ')} cae en
     * el {@code default} y devuelve {@code EMPTY}, o sea que una celda de
     * espacio no tiene ni suelo ni muro ni colision. El jugador se la puede
     * cruzar, pero es un agujero al vacio, no una ruta del nivel.
     */
    private static boolean[][] alcanzable(String[] filas) {
        int filasN = filas.length;
        int cols = filas[0].length();
        int[][] origen = null;
        for (int r = 0; r < filasN; r++) {
            for (int c = 0; c < cols; c++) {
                if ("▲▼▶◀".indexOf(filas[r].charAt(c)) >= 0) {
                    origen = new int[][] { { r, c } };
                }
            }
        }
        boolean[][] visto = new boolean[filasN][cols];
        if (origen == null) {
            return visto;
        }
        java.util.ArrayDeque<int[]> cola = new java.util.ArrayDeque<>();
        cola.add(origen[0]);
        visto[origen[0][0]][origen[0][1]] = true;
        int[][] lados = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!cola.isEmpty()) {
            int[] celda = cola.poll();
            for (int[] lado : lados) {
                int r = celda[0] + lado[0];
                int c = celda[1] + lado[1];
                if (r < 0 || r >= filasN || c < 0 || c >= cols || visto[r][c]) {
                    continue;
                }
                char ch = filas[r].charAt(c);
                if ("■◙▣□".indexOf(ch) >= 0) {
                    continue;
                }
                visto[r][c] = true;
                cola.add(new int[] { r, c });
            }
        }
        return visto;
    }

    private static String leer(String ruta) throws Exception {
        return Files.readString(Paths.get(ruta));
    }

    /** Como {@link #leer(String)}, pero sin ensuciar la firma con la excepcion. */
    private static String leerSilencioso(String ruta) {
        try {
            return leer(ruta);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void ok(boolean condicion, String que) {
        if (condicion) {
            System.out.println("  OK    " + que);
        } else {
            System.out.println("  FALLA " + que);
            fallos++;
        }
    }

    private ItemsMapChecks() {
    }
}
