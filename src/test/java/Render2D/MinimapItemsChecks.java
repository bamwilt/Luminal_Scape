package Render2D;

import Render3D.map.MapConfig;
import Render2D.components.Minimap;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Comprobaciones headless de que las llaves desaparecen del minimapa al
 * recogerse, y de que siguen viéndose mientras están en el nivel.
 *
 * <p>Devuelve 1 si algo falla, 0 si todo pasa.
 *
 * <p>El caso que motivó esto: el símbolo '◈' está escrito en el mapa de forma
 * permanente, así que al recoger la llave el minimapa seguía pintando la celda
 * en dorado aunque la llave ya no estuviera en el mundo. Los puntos flotantes sí
 * desaparecían (van por la lista viva de items), pero la celda no.
 */
public final class MinimapItemsChecks {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        laLlaveSePintaMientrasEsteEnElNivel();
        laLlaveDesapareceAlRecogerse();
        elHuecoSePintaComoSuelo();
        elMapaTambienDesaparece();
        losPuntosFlotantesVienenDeItemsVivos();
        elCodigoUsaItemsVivos();

        if (failures > 0) {
            System.out.println("MinimapItemsChecks: " + failures + " fallo(s)");
            System.exit(1);
        }
        System.out.println("MinimapItemsChecks: OK");
    }

    /** Con la llave en el nivel, su celda lleva el color de llave. */
    private static void laLlaveSePintaMientrasEsteEnElNivel() {
        float[] key = Minimap.colorDe(MapConfig.SymbolType.ITEM);
        float[] pintado = Minimap.colorDeCelda(MapConfig.SymbolType.ITEM, true);
        for (int i = 0; i < 3; i++) {
            approx("celda de llave con item presente (canal " + i + ")",
                    key[i], pintado[i], 1e-4f);
        }
    }

    /**
     * Al recogerla la celda deja de llevar el color de llave. Esta es la
     * regresión que falla si se vuelve a pintar solo desde el símbolo del mapa.
     */
    private static void laLlaveDesapareceAlRecogerse() {
        float[] key = Minimap.colorDe(MapConfig.SymbolType.ITEM);
        float[] pintado = Minimap.colorDeCelda(MapConfig.SymbolType.ITEM, false);
        check(!esIgual(pintado, key),
                "celda de llave SIN item ya no lleva el color de llave (quedó "
                        + pintado[0] + "/" + pintado[1] + "/" + pintado[2] + ")");
    }

    /** Lo que queda de la celda es suelo normal, y sigue viéndose. */
    private static void elHuecoSePintaComoSuelo() {
        float[] suelo = Minimap.colorDe(MapConfig.SymbolType.FLOOR_CEILING);
        float[] pintado = Minimap.colorDeCelda(MapConfig.SymbolType.ITEM, false);
        for (int i = 0; i < 3; i++) {
            approx("el hueco se pinta como suelo (canal " + i + ")",
                    suelo[i], pintado[i], 1e-4f);
        }
        // Y tiene que seguir siendo visible: si la celda se quitara del plano,
        // el hueco del color del panel delataría por dónde ha ido el player.
        // Se comprueba el alfa, porque 'vacío' y 'suelo' comparten color a
        // propósito: lo que no se dibuja no es el vacío, es la celda entera.
        check(pintado[3] > 0f, "el hueco se ve (alfa " + pintado[3] + ")");
    }

    /** El '☑' es un item más: también desaparece cuando se recoge. */
    private static void elMapaTambienDesaparece() {
        float[] mapa = Minimap.colorDe(MapConfig.SymbolType.MAP_ITEM);
        float[] sinItem = Minimap.colorDeCelda(MapConfig.SymbolType.MAP_ITEM, false);
        check(!esIgual(sinItem, mapa), "el '☑' desaparece al recogerlo");
        float[] conItem = Minimap.colorDeCelda(MapConfig.SymbolType.MAP_ITEM, true);
        check(esIgual(conItem, mapa), "el '☑' se ve mientras sigue en el nivel");
    }

    /** Los puntos flotantes tienen que salir de la lista de items, no del mapa. */
    private static void losPuntosFlotantesVienenDeItemsVivos() throws Exception {
        String src = leer("src/main/java/Render2D/components/Minimap.java");
        check(src.contains("for (Vector3f item : dungeon.getItemPositions())"),
                "los puntos se dibujan desde getItemPositions(), la lista viva");
        // Si el bucle usara el grid, se pintarían llaves ya recogidas.
        int puntos = src.indexOf("addQuadCentered(");
        check(puntos > 0, "los puntos flotantes se siguen dibujando");
        check(src.indexOf("getCellAt", puntos) < 0 || src.indexOf("getCellAt", puntos) > src.indexOf("addQuadCentered(", puntos),
                "los puntos flotantes no se sacan del símbolo del mapa");
    }

    /**
     * El dibujado tiene que mirar si el item sigue ahí. Se comprueba sobre el
     * código porque la lista de items vive en DungeonManager y construirla pide
     * OpenGL, que no hay en una prueba headless.
     */
    private static void elCodigoUsaItemsVivos() throws Exception {
        String src = leer("src/main/java/Render2D/components/Minimap.java");
        check(src.contains("boolean[][] occupied"),
                "hay una matriz de celdas con item vivo");
        check(src.contains("occupied[ir][ic] = true"),
                "se rellena desde las posiciones de los items");
        check(src.contains("colorDeCelda(type, occupied[r][c])"),
                "el color de la celda depende de si el item sigue ahí");
        check(src.contains("if (isItem(type) && !occupied)"),
                "la regla se aplica solo a celdas de item, no a muros ni suelo");

        String dungeon = leer("src/main/java/Render3D/map/DungeonManager.java");
        check(dungeon.contains("items.remove(recogido)"),
                "recoger un item lo quita de la lista, así deja de pintarse");
    }

    private static String leer(String ruta) throws Exception {
        return Files.readString(Path.of(ruta));
    }

    private static boolean esIgual(float[] a, float[] b) {
        for (int i = 0; i < 3; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-4f) {
                return false;
            }
        }
        return true;
    }

    private static void approx(String what, float expected, float actual, float tol) {
        if (Math.abs(expected - actual) > tol) {
            System.out.println("FAIL " + what + ": esperado " + expected
                    + ", obtenido " + actual);
            failures++;
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + what);
            failures++;
        }
    }

    private MinimapItemsChecks() {
    }
}