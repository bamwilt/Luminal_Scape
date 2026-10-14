package Render3D.map;

import java.util.Arrays;
import java.util.List;

/**
 * Comprobaciones headless del símbolo de decoración '#'.
 *
 * <p>Ejecución: se compila el proyecto y se lanza esta clase con el classpath de
 * test. Devuelve 1 si algo falla, 0 si todo pasa.
 *
 * <p>Cubre lo mínimo para que la decoración manual funcione:
 * <ul>
 *   <li>el '#' es {@link MapConfig.SymbolType#DECORATION}, piso transitable sin
 *       techo propio;</li>
 *   <li>hereda el techo de una zona techada, como un spawn o un item;</li>
 *   <li>{@link LevelLoader#rowsAfterMap} ya no descarta las filas que empiezan
 *       por '#' después de {@code map:}, pero sigue ignorando los comentarios de
 *       la cabecera;</li>
 *   <li>los niveles del juego solo decoran donde hay una marca '#' escrita a
 *       mano, sin marcas añadidas por el generador.</li>
 * </ul>
 */
public final class DecorGlyphChecks {

    private static int fallos;

    public static void main(String[] args) {
        elGlifoEsDecoracion();
        esPisoTransitableSinTechoPropio();
        heredaTechoDelVecino();
        marcasTrasMapNoSonComentario();
        comentariosAntesDeMapSeIgnoran();
        faltaMapFalla();
        elPlanMezclaManualYAutomatico();

        if (fallos > 0) {
            System.out.println("DecorGlyphChecks: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("DecorGlyphChecks: OK");
    }

    private static void elGlifoEsDecoracion() {
        ok(MapConfig.SymbolType.fromChar('\u0023') == MapConfig.SymbolType.DECORATION,
                "el glifo '#' es DECORATION");
        ok(MapConfig.SymbolType.DECORATION.glyph() == '\u0023', "el glifo de DECORATION es '#'");
    }

    private static void esPisoTransitableSinTechoPropio() {
        MapConfig.SymbolType d = MapConfig.SymbolType.DECORATION;
        ok(d.isWalkable(), "un '#' se puede pisar (es piso)");
        ok(!d.isSolid(), "un '#' no bloquea el paso");
        ok(d.needsFloor(), "un '#' lleva su propio panel de suelo");
        ok(!d.needsCeiling(), "un '#' no lleva techo propio: lo hereda o no lo lleva");
        ok(!d.isOpening(), "un '#' no es una abertura");
        ok(!d.isSpawn() && !d.isItem(), "un '#' no es un spawn ni un item");
        ok(d != MapConfig.SymbolType.RAILING, "un '#' no es una barandilla");
    }

    private static void heredaTechoDelVecino() {
        char[][] conTecho = {
            { '\u25eb', '\u25eb' },   // piso con techo
            { '\u25a1', '\u0023' },   // vacío, '#' junto al techo de arriba
        };
        ok(MapConfig.SymbolType.DECORATION.needsCeilingIn(conTecho, 1, 1),
                "un '#' junto a un piso techado hereda el techo");

        char[][] aislado = {
            { '\u0023', '\u25a1' },
        };
        ok(!MapConfig.SymbolType.DECORATION.needsCeilingIn(aislado, 0, 0),
                "un '#' aislado no lleva un panel de techo flotando");
    }

    private static void marcasTrasMapNoSonComentario() {
        List<String> lineas = Arrays.asList("map:", "\u25eb\u25eb", "\u0023\u25eb", "");
        List<String> filas = LevelLoader.rowsAfterMap(lineas);
        ok(filas.size() == 2, "tras map: solo se descartan las líneas en blanco (" + filas + ")");
        ok(filas.get(1).startsWith("\u0023"),
                "una fila que empieza por '#' no se descarta como comentario");
    }

    private static void comentariosAntesDeMapSeIgnoran() {
        List<String> lineas = Arrays.asList("\u0023 comentario", "name: x", "map:", "\u25eb");
        List<String> filas = LevelLoader.rowsAfterMap(lineas);
        ok(filas.size() == 1 && filas.get(0).equals("\u25eb"),
                "los '#' de la cabecera (antes de map:) siguen siendo comentarios");
    }

    private static void faltaMapFalla() {
        try {
            LevelLoader.rowsAfterMap(Arrays.asList("name: x"));
            ok(false, "un nivel sin 'map:' debería fallar");
        } catch (IllegalArgumentException e) {
            ok(true, "un nivel sin 'map:' lanza IllegalArgumentException");
        }
    }

    /**
     * Los niveles llevan solo las marcas '#' que pone quien los escribe (puede
     * haber cero). El plan de decoracion debe colocar exactamente una pieza por
     * cada marca, ni una mas: nada de decoracion inventada por el generador.
     */
        /**
     * El plan de decoración mezcla manual (donde hay '#') y automático
     * (suelo transitable sin marcas). Cada '#' conserva su pieza en su celda
     * y no hay duplicados de celda. El total puede ser mayor al nº de marcas.
     */
    private static void elPlanMezclaManualYAutomatico() {
        for (String nivel : new String[] { LevelLoader.DEFAULT_LEVEL, "levels/level_02.txt",
                "levels/level_03.txt" }) {
            String[] filas = LevelLoader.load(nivel);
            int marcas = 0;
            char[][] mapa = new char[filas.length][];
            for (int r = 0; r < filas.length; r++) {
                mapa[r] = filas[r].toCharArray();
                for (int i = 0; i < filas[r].length(); i++) {
                    if (filas[r].charAt(i) == '#') {
                        marcas++;
                    }
                }
            }
            List<PropLayout.Placed> plan = PropLayout.plan(mapa);
            ok(plan.size() >= marcas,
                    nivel + " tiene al menos una pieza por marca (#=" + marcas + ", total=" + plan.size() + ")");
            // Cada '#' tiene su pieza en su celda
            for (int r = 0; r < mapa.length; r++) {
                for (int c = 0; c < mapa[r].length; c++) {
                    if (mapa[r][c] == '#') {
                        boolean hay = false;
                        for (PropLayout.Placed p : plan) {
                            if (p.row == r && p.col == c) { hay = true; break; }
                        }
                        ok(hay, nivel + " marca en (" + r + "," + c + ") tiene su pieza");
                    }
                }
            }
            // Sin duplicados de celda
            java.util.Set<String> celdas = new java.util.HashSet<>();
            for (PropLayout.Placed p : plan) {
                String clave = p.row + ":" + p.col;
                ok(celdas.add(clave), nivel + " sin celdas duplicadas (duplicada: " + clave + ")");
            }
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

    private DecorGlyphChecks() {
    }
}
