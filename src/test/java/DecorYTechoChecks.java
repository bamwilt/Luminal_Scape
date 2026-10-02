import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import Render3D.map.Atmosphere;
import Render3D.map.LevelData;
import Render3D.map.LevelLoader;
import Render3D.map.MapConfig;
import Render3D.map.PropLayout;

/**
 * Comprobaciones headless de las tres cosas del ultimo cambio. No necesita
 * contexto de OpenGL: solo lee mapas y aplica la misma matematica que los
 * shaders, para que se pueda correr en cualquier momento.
 *
 * Cubre:
 * <ol>
 *   <li>Techo: ninguna celda de suelo en zona abierta se queda con techo, y
 *       ningun spawn o item aislado se queda con un panel flotando.</li>
 *   <li>Decoracion: el plan es determinista, usa una variedad real de piezas,
 *       y ninguna pieza se sale de su celda ni pisa railing, spawn o vacio.</li>
 *   <li>Niebla: hay margen cercano y la curva es mas suave que la lineal antes
 *       del fondo, pero sigue llegando a 1 en la distancia de vista.</li>
 * </ol>
 */
public final class DecorYTechoChecks {

    private static int fallos;

    public static void main(String[] args) throws IOException {
        techo();
        decoracion();
        niebla();
        if (fallos == 0) {
            System.out.println("OK: todas las comprobaciones de decoracion, techo y niebla pasan.");
        } else {
            System.out.println("FALLOS: " + fallos);
            System.exit(1);
        }
    }

    // ---- techo ----------------------------------------------------------

    private static void techo() throws IOException {
        System.out.println("== techo ==");
        int aisladosSinTecho = 0;
        int celdasTechadas = 0;
        for (String nivel : new String[] { "levels/level_01.txt", "levels/level_02.txt",
                "levels/level_03.txt" }) {
            char[][] mapa = mapa(nivel);
            for (int r = 0; r < mapa.length; r++) {
                for (int c = 0; c < mapa[0].length; c++) {
                    MapConfig.SymbolType t = MapConfig.SymbolType.fromChar(mapa[r][c]);
                    if (!t.needsFloor()) {
                        continue;
                    }
                    boolean techo = t.needsCeilingIn(mapa, r, c);
                    boolean propio = t.needsCeiling();
                    if (techo) {
                        celdasTechadas++;
                    }
                    if (propio && !techo) {
                        ok(false, "celda techada sin techo en " + nivel + " (" + r + "," + c + ")");
                    }
                    if (techo && !propio) {
                        // Solo puede ser un spawn o item dentro de zona techada.
                        boolean esRodeada = vecinoTechado(mapa, r, c);
                        ok(esRodeada, "techo heredado sin vecino techado en " + nivel);
                    }
                    if (!techo && !propio) {
                        // Un spawn o item suelto en el vacio: es el caso que
                        // antes generaba el panel flotante.
                        aisladosSinTecho++;
                    }
                }
            }
        }
        ok(aisladosSinTecho > 0, "debe existir al menos un spawn/item sin techo propio (el caso arreglado)");
        ok(celdasTechadas > 0, "debe quedar celda techada en los niveles");
        System.out.println("   celdas techadas=" + celdasTechadas + " spawn/item al aire libre=" + aisladosSinTecho);
    }

    private static boolean vecinoTechado(char[][] mapa, int r, int c) {
        int[][] lados = { { r - 1, c }, { r + 1, c }, { r, c - 1 }, { r, c + 1 } };
        for (int[] l : lados) {
            int lr = l[0];
            int lc = l[1];
            if (lr < 0 || lr >= mapa.length || lc < 0 || lc >= mapa[0].length) {
                continue;
            }
            if (MapConfig.SymbolType.fromChar(mapa[lr][lc]).needsCeiling()) {
                return true;
            }
        }
        return false;
    }

    // ---- decoracion -----------------------------------------------------

    private static void decoracion() throws IOException {
        System.out.println("== decoracion ==");
        // Las piezas se apoyan en la cara visible del suelo, no en y=0: si
        // PROP_BASE_Y fuera 0 quedarian medio enterradas.
        ok(MapConfig.PROP_BASE_Y >= MapConfig.FLOOR_THICKNESS,
                "las decoraciones se apoyan sobre el suelo visible, no enterradas");
        ok(MapConfig.PROP_BASE_Y < MapConfig.CAMERA_HEIGHT,
                "la base de la decoracion debe quedar por debajo de la camara");
        ok(MapConfig.PROP_SCALE > 0f, "la escala global de decoracion debe ser positiva");
        ok(MapConfig.PROP_DENSITY > 0f && MapConfig.PROP_DENSITY <= 1f,
                "la densidad de decoracion debe estar en (0,1]");
        String[] niveles = { "levels/level_01.txt", "levels/level_02.txt", "levels/level_03.txt" };
        java.util.Set<String> modelosVistos = new java.util.LinkedHashSet<>();
        int total = 0;
        for (String nivel : niveles) {
            char[][] mapa = mapa(nivel);
            java.util.List<PropLayout.Placed> a = PropLayout.plan(mapa);
            java.util.List<PropLayout.Placed> b = PropLayout.plan(mapa);
            ok(a.equals(b), "el plan debe ser determinista en " + nivel);

            for (PropLayout.Placed p : a) {
                total++;
                modelosVistos.add(p.model);
                MapConfig.SymbolType aqui = MapConfig.SymbolType.fromChar(mapa[p.row][p.col]);
                ok(aqui.isWalkable() && aqui != MapConfig.SymbolType.RAILING,
                        "no se decora una celda no transitable en " + nivel);
                ok(!aqui.isSpawn() && aqui != MapConfig.SymbolType.ITEM,
                        "no se decora un spawn ni un item en " + nivel);
                ok(p.scale > 0f, "escala positiva");
                // Ninguna pieza pegada a un railing.
                int[][] lados = { { p.row - 1, p.col }, { p.row + 1, p.col }, { p.row, p.col - 1 },
                        { p.row, p.col + 1 } };
                for (int[] l : lados) {
                    int lr = l[0];
                    int lc = l[1];
                    if (lr < 0 || lr >= mapa.length || lc < 0 || lc >= mapa[0].length) {
                        continue;
                    }
                    ok(MapConfig.SymbolType.fromChar(mapa[lr][lc]) != MapConfig.SymbolType.RAILING,
                            "ninguna pieza pegada a un railing en " + nivel);
                }
            }
        }
        ok(total > 0, "debe haber decoracion");
        ok(modelosVistos.size() >= 3,
                "debe haber variedad real de piezas (vistos: " + modelosVistos.size() + ")");
        // El catalogo es solo decoracion de sobra: nada de cactus, repisas ni
        // mobiliario grande.
        for (String prohibido : new String[] { "Cactus", "Shelf", "Couch", "Bed", "Drawer",
                "Column", "Night Stand" }) {
            for (String modelo : modelosVistos) {
                ok(!modelo.contains(prohibido), "el catalogo no debe incluir " + prohibido
                        + " (encontro " + modelo + ")");
            }
        }
        System.out.println("   piezas=" + total + " modelos distintos=" + modelosVistos.size());
    }

    // ---- niebla ---------------------------------------------------------

    private static void niebla() {
        System.out.println("== niebla ==");
        // Mas calidad, mas lejos: 10 / 15 / 20.
        ok(MapConfig.VIEW_DISTANCE_LOW < MapConfig.VIEW_DISTANCE_NORMAL,
                "low debe ver menos que normal");
        ok(MapConfig.VIEW_DISTANCE_NORMAL < MapConfig.VIEW_DISTANCE_HIGH,
                "normal debe ver menos que high");
        // Las tres palabras dan exactamente su constante del panel. Los valores
        // NO se escriben aqui a proposito: los cambia quien edita MapConfig y el
        // test solo vigila que low/normal/high sigan apuntando a lo correcto.
        ok(Atmosphere.viewDistance("low") == MapConfig.VIEW_DISTANCE_LOW,
                "low da VIEW_DISTANCE_LOW");
        ok(Atmosphere.viewDistance("normal") == MapConfig.VIEW_DISTANCE_NORMAL,
                "normal da VIEW_DISTANCE_NORMAL");
        ok(Atmosphere.viewDistance("high") == MapConfig.VIEW_DISTANCE_HIGH,
                "high da VIEW_DISTANCE_HIGH");
        System.out.println("   distancias low/normal/high = " + MapConfig.VIEW_DISTANCE_LOW + " / "
                + MapConfig.VIEW_DISTANCE_NORMAL + " / " + MapConfig.VIEW_DISTANCE_HIGH);

        float[] distancias = { MapConfig.VIEW_DISTANCE_LOW, MapConfig.VIEW_DISTANCE_NORMAL,
                MapConfig.VIEW_DISTANCE_HIGH };
        for (float vd : distancias) {
            float near = Atmosphere.fogNear(vd);
            ok(near > 0f, "margen cercano positivo para viewDistance=" + vd);
            ok(near < vd, "el margen cercano debe ser menor que la distancia de vista");
            // A los pies no hay niebla.
            ok(factor(0f, near, vd) == 0f, "sin niebla en los pies (vd=" + vd + ")");
            ok(factor(near * 0.5f, near, vd) == 0f, "campo cercano limpio (vd=" + vd + ")");
            // Llega a 1 justo en la distancia de vista.
            ok(Math.abs(factor(vd, near, vd) - 1f) < 1e-4f, "niebla total en la distancia de vista");
            // Y es mas suave que la lineal antigua en el campo medio.
            float medio = vd * 0.5f;
            ok(factor(medio, near, vd) < medio / vd,
                    "la curva nueva debe ser mas clara que la lineal en la mitad (vd=" + vd + ")");
        }
        System.out.println("   margen = " + Math.round(MapConfig.FOG_NEAR_RATIO * 100)
                + "% de la distancia de vista");
    }

    /** Mismo calculo que los shaders: rampa cubica entre el margen y el fondo. */
    private static float factor(float dist, float near, float vd) {
        float span = Math.max(vd - near, 0.001f);
        float t = Math.min(Math.max((dist - near) / span, 0f), 1f);
        return t * t * t;
    }

    // ---- utiles ---------------------------------------------------------

    private static char[][] mapa(String nivel) throws IOException {
        LevelData data = LevelLoader.loadLevel(nivel);
        String[] filas = data.toRows();
        char[][] g = new char[filas.length][];
        for (int i = 0; i < filas.length; i++) {
            g[i] = filas[i].toCharArray();
        }
        return g;
    }

    private static void ok(boolean condicion, String que) {
        if (condicion) {
            System.out.println("  OK    " + que);
        } else {
            System.out.println("  FALLA " + que);
            fallos++;
        }
    }

    private DecorYTechoChecks() {
    }
}
