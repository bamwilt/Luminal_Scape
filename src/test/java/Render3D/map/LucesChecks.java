package Render3D.map;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.joml.Vector3f;

import Render3D.map.MapConfig.SymbolType;
import Render3D.mesh.MeshBuilder;

/**
 * Comprobaciones headless del techo con luz ({@code ◉}) y de las luces
 * puntuales. No necesita contexto de OpenGL: el techo se comprueba sobre los
 * builders y las luces sobre {@link DungeonManager#collectLights}.
 *
 * <p>Esta clase vive en el paquete de {@link DungeonManager} porque su
 * geometría estática se puede inspeccionar sin GPU solo con los métodos de
 * paquete ({@link DungeonManager#buildStaticGeometry}, {@link
 * DungeonManager#lightCeilingGeometryView}); desde el paquete por defecto no se
 * ven.
 *
 * <p>Cubre:
 * <ol>
 *   <li>El símbolo '◉' es una celda transitable que siempre lleva techo.</li>
 *   <li>Su panel va a la malla APARTE, con la misma forma que el techo normal,
 *       y enciende un foco por celda en la altura correcta.</li>
 *   <li>El techo normal NO contiene las placas: si compartieran malla, el
 *       brillo se le saldría a todo el techo del nivel.</li>
 *   <li>{@link DungeonManager#collectLights} devuelve las más cercanas al
 *       jugador, descarta las lejanas y no se pasa del array del shader.</li>
 *   <li>El {@code #define MAX_LIGHT_COUNT} de los shaders coincide con
 *       {@link MapConfig#MAX_LIGHT_COUNT}, que es el único sitio editable.</li>
 * </ol>
 */
public final class LucesChecks {

    private static int fallos;

    public static void main(String[] args) throws IOException {
        simbolo();
        geometria();
        luces();
        constantes();
        shaders();
        if (fallos == 0) {
            System.out.println("OK: todas las comprobaciones de luces pasan.");
        } else {
            System.out.println("FALLOS: " + fallos);
            System.exit(1);
        }
    }

    // ---- simbolo ---------------------------------------------------------

    private static void simbolo() {
        System.out.println("== simbolo ==");
        SymbolType luz = SymbolType.fromChar('◉');
        ok(luz == SymbolType.LIT_CEILING, "'◉' debe mapear a LIT_CEILING (fue " + luz + ")");
        ok(luz.glyph() == '◉', "el glifo de LIT_CEILING debe seguir siendo '◉'");
        ok(luz.needsFloor(), "un '◉' se pisa: necesita su panel de suelo");
        ok(luz.needsCeiling(), "un '◉' lleva SIEMPRE techo, es la placa que brilla");
        ok(luz.isWalkable(), "un '◉' es transitable");
        ok(luz.isLitCeiling(), "un '◉' se reconoce como techo con luz");
        ok(!luz.isSolid(), "un '◉' no es sólido: se anda por encima");
        ok(!luz.isWall() && !luz.isOpening(), "un '◉' no es muro ni abertura");
        // El resto de símbolos no se han movido ni han cambiado de significado.
        ok(SymbolType.fromChar('◫') == SymbolType.FLOOR_CEILING, "'◫' sigue siendo FLOOR_CEILING");
        ok(SymbolType.fromChar('▒') == SymbolType.FLOOR_ONLY, "'▒' sigue siendo FLOOR_ONLY");
        ok(SymbolType.fromChar('◈') == SymbolType.ITEM, "'◈' sigue siendo ITEM");
    }

    // ---- geometria -------------------------------------------------------

    private static void geometria() throws IOException {
        System.out.println("== geometria ==");
        int placas = 0;
        for (String nivel : NIVELES) {
            char[][] mapa = mapa(nivel);
            int enNivel = contar(mapa, '◉');
            if (enNivel == 0) {
                continue;
            }
            placas += enNivel;

            DungeonManager dm = new DungeonManager();
            dm.buildStaticGeometry(LevelLoader.loadLevel(nivel).toRows());

            // Un foco por celda '◉', y ninguno de más.
            ok(dm.getLitCeilingCount() == enNivel,
                    nivel + ": un foco por celda '◉' (celdas=" + enNivel
                            + " focos=" + dm.getLitCeilingCount() + ")");

            // El panel luminoso va a su propia malla, con una caja completa por
            // celda (6 caras x 4 vertices, 6 triangulos x 3 indices).
            MeshBuilder placa = dm.lightCeilingGeometryView();
            ok(placa.indexCount() == enNivel * 36,
                    nivel + ": la placa son " + enNivel + " cajas (indices=" + placa.indexCount() + ")");
            ok(placa.vertexCount() == enNivel * 24,
                    nivel + ": la placa son " + enNivel + " cajas (vertices=" + placa.vertexCount() + ")");

            // Y el techo normal se queda con el resto: ni una placa dentro.
            int techoNormal = celdasTechadas(mapa) - enNivel;
            int esperado = techoNormal * 36;
            ok(dm.ceilingGeometryView().indexCount() == esperado,
                    nivel + ": el techo normal solo lleva sus " + techoNormal + " celdas (indices="
                            + dm.ceilingGeometryView().indexCount() + " esperados=" + esperado + ")");

            // Cada foco cae justo bajo su panel, no en el plano del techo.
            float esperadoY = MapConfig.WALL_HEIGHT - MapConfig.FLOOR_THICKNESS
                    - MapConfig.LIGHT_CEILING_DROP;
            boolean alturaOk = true;
            for (Vector3f p : dm.getStaticLightPositions()) {
                if (Math.abs(p.y - esperadoY) > 1e-4f) {
                    alturaOk = false;
                }
            }
            ok(alturaOk, "los focos caen " + MapConfig.LIGHT_CEILING_DROP + " bajo el techo");

            // Y el suelo de la celda '◉' es el de siempre, con su textura: lo
            // único que se separa del '◫' es la cara de arriba.
            int pisos = 0;
            for (int r = 0; r < mapa.length; r++) {
                for (int c = 0; c < mapa[0].length; c++) {
                    if (SymbolType.fromChar(mapa[r][c]).needsFloor()) {
                        pisos++;
                    }
                }
            }
            ok(dm.floorGeometryView().indexCount() == pisos * 36,
                    nivel + ": el suelo cubre las " + pisos + " celdas transitables (indices="
                            + dm.floorGeometryView().indexCount() + ")");
        }
        ok(placas > 0, "debe haber placas de luz en los niveles");
        System.out.println("   placas de luz en los niveles=" + placas);
    }

    /** Cuantas celdas del mapa llevan panel de techo (las '◉' incluidas). */
    private static int celdasTechadas(char[][] mapa) {
        int total = 0;
        for (int r = 0; r < mapa.length; r++) {
            for (int c = 0; c < mapa[0].length; c++) {
                SymbolType t = SymbolType.fromChar(mapa[r][c]);
                if (t.needsFloor() && t.needsCeilingIn(mapa, r, c)) {
                    total++;
                }
            }
        }
        return total;
    }

    // ---- luces -----------------------------------------------------------

    private static void luces() {
        System.out.println("== luces ==");
        String[] filas = { "▣◉◫◙", "▣◫▶◫▣" };
        DungeonManager dm = new DungeonManager();
        dm.buildStaticGeometry(filas);
        ok(dm.getLitCeilingCount() == 1, "el nivel de prueba tiene un solo '◉'");

        int max = MapConfig.MAX_LIGHT_COUNT;
        float[] pos = new float[max * 3];
        float[] col = new float[max * 3];

        // Sin items (no se suben modelos sin GPU), el unico foco es la placa.
        Vector3f camara = new Vector3f(0f, MapConfig.CAMERA_HEIGHT, 0f);
        int cerca = dm.collectLights(camara, pos, col, max);
        ok(cerca == 1, "se ve el foco de la placa (n=" + cerca + ")");
        ok(col[0] > 0f, "el foco tiene color");
        ok(Math.abs(pos[1] - (MapConfig.WALL_HEIGHT - MapConfig.FLOOR_THICKNESS
                - MapConfig.LIGHT_CEILING_DROP)) < 1e-4f,
                "el foco se envia a la altura del panel, no a la del suelo");

        // Muy lejos: se descarta antes de llegar al array.
        float lejos = MapConfig.LIGHT_CULL_DISTANCE * 2f;
        int lejosN = dm.collectLights(new Vector3f(0f, 1f, lejos), pos, col, max);
        ok(lejosN == 0, "un foco a " + (int) lejos + " m se descarta (llego " + lejosN + ")");

        // Nunca mas luces que huecos en el array del shader.
        DungeonManager dm2 = new DungeonManager();
        dm2.buildStaticGeometry(mapaLleno());
        ok(dm2.getLitCeilingCount() == 21, "el mapa de prueba tiene 21 placas");
        int muchasN = dm2.collectLights(new Vector3f(0f, 1f, 0f), pos, col, max);
        ok(muchasN == max, "nunca se mandan mas luces que el array (mandadas=" + muchasN
                + " max=" + max + ")");
    }

    // ---- constantes ------------------------------------------------------

    private static void constantes() {
        System.out.println("== constantes ==");
        ok(MapConfig.LIGHT_CEILING_EMISSION > 0f, "la placa tiene que brillar algo");
        ok(MapConfig.LIGHT_CEILING_INTENSITY > 0f, "la placa tiene que iluminar algo");
        ok(MapConfig.LIGHT_CEILING_DROP > 0f, "el foco va por debajo del panel, no en su plano");
        ok(MapConfig.LIGHT_CEILING_DROP < MapConfig.WALL_HEIGHT,
                "el foco no puede bajar por debajo del suelo");
        ok(MapConfig.MAX_LIGHT_COUNT > 0, "hay al menos un hueco para luces");

        // La aureola del item es dorada: mas rojo que verde, y verde que azul.
        ok(MapConfig.ITEM_LIGHT_R > MapConfig.ITEM_LIGHT_G
                        && MapConfig.ITEM_LIGHT_G > MapConfig.ITEM_LIGHT_B,
                "la luz del item es dorada (R>G>B)");
        ok(MapConfig.ITEM_LIGHT_INTENSITY > 0f, "la luz del item tiene intensidad");
        ok(MapConfig.ITEM_LIGHT_INTENSITY < 1f, "la luz del item es tenue, no un foco de patio");
        ok(MapConfig.LIGHT_ATTEN_LINEAR > 0f && MapConfig.LIGHT_ATTEN_QUADRATIC > 0f,
                "la caida por distancia tiene que ser positiva");
        ok(MapConfig.LIGHT_CULL_DISTANCE > 0f, "el descarte por distancia tiene que ser positivo");
        // El techo con luz se distingue del item: uno casi blanco, el otro dorado.
        ok(MapConfig.LIGHT_CEILING_G > MapConfig.LIGHT_CEILING_B,
                "la placa es mas calida que neutra");
    }

    // ---- shaders ---------------------------------------------------------

    /**
     * El tamaño del array de luces vive en un {@code #define} de cada shader
     * porque GLSL no admite un tamaño de array leido de fuera. Es el unico
     * punto donde MapConfig y el shader se pueden desincronizar, asi que se
     * comprueba aqui contra el fuente de verdad.
     */
    private static void shaders() throws IOException {
        System.out.println("== shaders ==");
        for (String shader : new String[] { "shaders/wall_fragment.glsl", "shaders/prop_fragment.glsl",
                "shaders/item_fragment.glsl" }) {
            String fuente = recurso(shader);
            int def = define(fuente);
            ok(def == MapConfig.MAX_LIGHT_COUNT,
                    shader + ": MAX_LIGHT_COUNT del shader (" + def + ") debe ser "
                            + MapConfig.MAX_LIGHT_COUNT);
            ok(fuente.contains("uniform vec3 u_lightPos["), shader + ": declara el array de posiciones");
            ok(fuente.contains("uniform vec3 u_lightColor["), shader + ": declara el array de colores");
            ok(fuente.contains("uniform int u_lightCount"), shader + ": declara el numero de luces");
        }
        // Solo el muro lleva emision: la placa se dibuja con el shader del
        // escenario, y las props no tienen por que brillar.
        ok(recurso("shaders/wall_fragment.glsl").contains("uniform float u_emission"),
                "wall_fragment.glsl declara la emision de la placa");
    }

    private static int define(String fuente) {
        for (String linea : fuente.split("\n")) {
            String limpia = linea.trim();
            if (limpia.startsWith("#define MAX_LIGHT_COUNT")) {
                return Integer.parseInt(limpia.split("\\s+")[2]);
            }
        }
        return -1;
    }

    // ---- utiles ----------------------------------------------------------

    private static final String[] NIVELES = { "levels/level_01.txt", "levels/level_02.txt",
            "levels/level_03.txt" };

    private static char[][] mapa(String nivel) throws IOException {
        String[] filas = LevelLoader.loadLevel(nivel).toRows();
        char[][] g = new char[filas.length][];
        for (int i = 0; i < filas.length; i++) {
            g[i] = filas[i].toCharArray();
        }
        return g;
    }

    /** Mapa de 3x9 con 21 placas '◉' y borde cerrado, para forzar el tope. */
    private static String[] mapaLleno() {
        String[] filas = new String[3];
        for (int r = 0; r < filas.length; r++) {
            StringBuilder fila = new StringBuilder();
            for (int c = 0; c < 9; c++) {
                if (c == 0) {
                    fila.append('◙');
                } else if (c == 8) {
                    fila.append('▣');
                } else {
                    fila.append('◉');
                }
            }
            filas[r] = fila.toString();
        }
        return filas;
    }

    private static int contar(char[][] mapa, char simbolo) {
        int total = 0;
        for (char[] fila : mapa) {
            for (char c : fila) {
                if (c == simbolo) {
                    total++;
                }
            }
        }
        return total;
    }

    private static String recurso(String ruta) throws IOException {
        try (InputStream in = LucesChecks.class.getClassLoader().getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IOException("Recurso no encontrado: " + ruta);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
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

    private LucesChecks() {
    }
}
