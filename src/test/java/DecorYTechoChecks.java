import Core.ConfigGlobal;
import Render3D.graphics.GlbModel;
import Render3D.map.Atmosphere;
import Render3D.map.LevelData;
import Render3D.map.LevelLoader;
import Render3D.map.MapConfig;
import Render3D.map.PropLayout;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Comprobaciones headless de decoración, techo y niebla. No necesita contexto de
 * OpenGL: solo lee mapas y aplica la misma matemática que los shaders, para que
 * se pueda correr en cualquier momento.
 *
 * Cubre:
 * <ol>
 *   <li>Techo: ninguna celda de suelo en zona abierta se queda con techo, y
 *       ningún spawn o item aislado se queda con un panel flotando.</li>
 *   <li>Decoración mixta (manual + automática): cada '#' del nivel pone su pieza y el automático rellena suelo transitable;
 *       plan es determinista, mezcla manual y automática, usa variedad real
 *       de su celda ni taparía un railing.</li>
 *   <li>Niebla: hay margen cercano y la curva es más suave que la lineal antes
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
                        // Solo puede ser un spawn, item o '#' dentro de zona techada.
                        boolean esRodeada = vecinoTechado(mapa, r, c);
                        ok(esRodeada, "techo heredado sin vecino techado en " + nivel);
                    }
                    if (!techo && !propio) {
                        // Un spawn, item o '#' suelto en el vacío: es el caso que
                        // antes generaba el panel flotante.
                        aisladosSinTecho++;
                    }
                }
            }
        }
        ok(aisladosSinTecho > 0, "debe existir al menos un suelo sin techo propio (el caso arreglado)");
        ok(celdasTechadas > 0, "debe quedar celda techada en los niveles");
        System.out.println("   celdas techadas=" + celdasTechadas + " suelo al aire libre=" + aisladosSinTecho);
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

    // ---- decoración -----------------------------------------------------

    private static void decoracion() throws IOException {
        System.out.println("== decoracion ==");
        ok(MapConfig.PROP_BASE_Y > 0f, "las decoraciones se apoyan sobre el suelo visible, no enterradas");
        ok(MapConfig.PROP_BASE_Y < MapConfig.CAMERA_HEIGHT,
                "la base de la decoracion debe quedar por debajo de la camara");
        ok(MapConfig.PROP_SCALE > 0f, "la scale global de decoracion debe ser positiva");

        // El reparto por grupos: acumulado, creciente y con el último por encima
        // de 1 para que el último grupo cubra siempre el resto del rango.
        float[] ends = ConfigGlobal.PROP_DECOR_GROUP_ENDS;
        ok(ends.length >= 1, "hay al menos un grupo de decoracion");
        ok(ends[ends.length - 1] > 1f,
                "el ultimo umbral de grupo pasa de 1 para cubrir todo el rango");
        for (int i = 1; i < ends.length; i++) {
            ok(ends[i] > ends[i - 1], "los umbrales de grupo son crecientes");
        }

        // Ninguna silla: se compró el modelo por lo que es y solo se quiere planta
        // y mueble. Ni mobiliario grande del resto del pack.
        for (String modelo : PropLayout.modelosDelCatalogo()) {
            String nombre = modelo.substring(modelo.lastIndexOf('/') + 1).toLowerCase();
            ok(!nombre.contains("chair") && !nombre.contains("stool") && !nombre.contains("seat"),
                    "el catalogo no tiene asientos: " + nombre);
            for (String prohibido : new String[] { "cactus", "shelf", "couch", "bed", "drawer",
                    "column", "night stand" }) {
                ok(!nombre.contains(prohibido), "el catalogo no debe incluir " + prohibido
                        + " (encontro " + nombre + ")");
            }
        }
        // Y todo lo que se coloca tiene que existir de verdad en recursos, o la
        // pieza sale invisible en el juego y el fallo no se ve en el test.
        for (String modelo : PropLayout.modelosDelCatalogo()) {
            ok(recursoExiste(modelo), "el recurso del catalogo existe: " + modelo);
        }

        // plan() solo mira el glifo '#': sin marcas no decora nada, y cada marca
        // pone una pieza. Es la garantía de que la decoración es manual.
        ok(PropLayout.plan(new char[][] { { '\u25a1', '\u25a1' } }).isEmpty(),
                "sin '#' no se coloca ninguna pieza");
        List<PropLayout.Placed> una = PropLayout.plan(new char[][] { { '#', '\u25a1' } });
        ok(una.size() == 1 && una.get(0).row == 0 && una.get(0).col == 0,
                "cada '#' pone exactamente una pieza en su celda");

        // Los niveles pueden tener marcas '#' (manual) y el automático rellena
        // suelo transitable sin marcas. El plan es determinista y cada '#'
        // conserva su pieza; el total puede ser mayor al nº de marcas.
        String[] niveles = { "levels/level_01.txt", "levels/level_02.txt", "levels/level_03.txt" };
        for (String nivel : niveles) {
            char[][] mapa = mapa(nivel);
            List<PropLayout.Placed> a = PropLayout.plan(mapa);
            List<PropLayout.Placed> b = PropLayout.plan(mapa);
            ok(a.equals(b), "el plan debe ser determinista en " + nivel);

            int marcas = contarMarcas(mapa);
            ok(a.size() >= marcas, "cada '#' pone una pieza; hay " + a.size()
                    + " piezas totales y " + marcas + " marcas en " + nivel);

            for (PropLayout.Placed p : a) {
                MapConfig.SymbolType aqui = MapConfig.SymbolType.fromChar(mapa[p.row][p.col]);
                boolean enMarca = (aqui == MapConfig.SymbolType.DECORATION);
                boolean enAutomatico = aqui.isWalkable() && aqui != MapConfig.SymbolType.RAILING
                        && !aqui.isSpawn() && !aqui.isItem();
                ok(enMarca || enAutomatico,
                        "pieza en suelo valido en " + nivel + " (" + p.row + "," + p.col + ")");
                ok(p.scale > 0f, "scale positiva");
            }
        }

        // Variedad del catalogo: una sala llena de '#' tiene que sacar mas de un
        // modelo distinto y sacar siempre los mismos. Es la unica parte que el
        // azar determinista decide (que pieza, no donde).
        char[][] sala = new char[8][8];
        for (int r = 0; r < sala.length; r++) {
            for (int c = 0; c < sala[0].length; c++) {
                sala[r][c] = '#';
            }
        }
        List<PropLayout.Placed> planSala = PropLayout.plan(sala);
        ok(planSala.equals(PropLayout.plan(sala)), "el plan es determinista en una sala llena");
        ok(planSala.size() == sala.length * sala[0].length,
                "cada '#' de la sala pone una pieza (piezas=" + planSala.size() + ")");
        Set<String> modelosVistos = new LinkedHashSet<>();
        for (PropLayout.Placed p : planSala) {
            modelosVistos.add(p.model);
        }
        ok(modelosVistos.size() >= 3,
                "debe haber variedad real de piezas (vistos: " + modelosVistos.size() + ")");
        int total = planSala.size();

        // Toda pieza tiene que apoyarse en el suelo, no medio enterrada. Esto se
        // comprueba con el loader de verdad sobre los GLB del catálogo: los
        // archivos del pack nacen a distintas alturas, y sin corregir, la mesa se
        // hundía 6 cm en el panel. Ver GlbModel.groundOnFloor.
        for (String modelo : PropLayout.modelosDelCatalogo()) {
            GlbModel glb = GlbModel.load(modelo);
            if (glb == null || !glb.isSupported()) {
                ok(false, "el modelo se puede leer: " + modelo);
                continue;
            }
            glb.groundOnFloor();
            ok(glb.baseMeters() >= -0.001f && glb.baseMeters() <= 0.001f,
                    "la pieza apoya en el suelo tras corregir (" + modelo + ", base="
                            + glb.baseMeters() + ")");
            // Y tiene que caber en la celda de 2 m con margen para el vecino. El
            // ancho se mide YA con la scale del catálogo, que es lo que se ve.
            float ancho = glb.radiusMeters() * 2f * PropLayout.escalaDe(modelo);
            ok(ancho <= MapConfig.TILE_SIZE * 0.9f,
                    "la pieza cabe en la celda (" + modelo + ", ancho=" + ancho + ")");
        }
        System.out.println("   piezas=" + total + " modelos distintos=" + modelosVistos.size());
    }

    /** Cuántas celdas del mapa llevan el glifo de decoración '#'. */
    private static int contarMarcas(char[][] mapa) {
        int n = 0;
        for (char[] fila : mapa) {
            for (char ch : fila) {
                if (MapConfig.SymbolType.fromChar(ch) == MapConfig.SymbolType.DECORATION) {
                    n++;
                }
            }
        }
        return n;
    }

    // ---- niebla ---------------------------------------------------------

    private static void niebla() {
        System.out.println("== niebla ==");
        ok(MapConfig.VIEW_DISTANCE_LOW == Atmosphere.viewDistance("low"), "low da LOW");
        ok(Atmosphere.viewDistance("normal") == MapConfig.VIEW_DISTANCE_NORMAL,
                "normal da VIEW_DISTANCE_NORMAL");
        ok(Atmosphere.viewDistance("high") == MapConfig.VIEW_DISTANCE_HIGH,
                "high da VIEW_DISTANCE_HIGH");

        float[] distancias = { MapConfig.VIEW_DISTANCE_LOW, MapConfig.VIEW_DISTANCE_NORMAL,
                MapConfig.VIEW_DISTANCE_HIGH };
        for (float vd : distancias) {
            float near = Atmosphere.fogNear(vd);
            ok(near > 0f, "margen cercano positivo para viewDistance=" + vd);
            ok(near < vd, "el margen cercano debe ser menor que la distancia de vista");
            ok(factor(0f, near, vd) == 0f, "sin niebla en los pies (vd=" + vd + ")");
            ok(factor(near * 0.5f, near, vd) == 0f, "campo cercano limpio (vd=" + vd + ")");
            ok(Math.abs(factor(vd, near, vd) - 1f) < 1e-4f, "niebla total en la distancia de vista");
            float medio = vd * 0.5f;
            ok(factor(medio, near, vd) < medio / vd,
                    "la curva nueva debe ser mas clara que la lineal en la mitad (vd=" + vd + ")");
        }
        System.out.println("   distancias low/normal/high = " + MapConfig.VIEW_DISTANCE_LOW + " / "
                + MapConfig.VIEW_DISTANCE_NORMAL + " / " + MapConfig.VIEW_DISTANCE_HIGH);
    }

    /** Curva de niebla (la misma que los shaders) evaluada en un punto. */
    private static float factor(float distancia, float near, float viewDistance) {
        if (distancia <= near) {
            return 0f;
        }
        float t = (distancia - near) / (viewDistance - near);
        return t * t * t;
    }

    // ---- utilidades -----------------------------------------------------

    private static char[][] mapa(String resourcePath) throws IOException {
        LevelData data = LevelLoader.loadLevel(resourcePath);
        String[] rows = data.toRows();
        char[][] map = new char[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            map[r] = rows[r].toCharArray();
        }
        return map;
    }

    /**
     * Si el recurso está en el classpath.
     *
     * <p>Usa el classloader y no el disco a proposito: asi se comprueba lo mismo
     * que vera el juego, con target/classes ya copiado.
     */
    private static boolean recursoExiste(String ruta) {
        return DecorYTechoChecks.class.getClassLoader().getResource(ruta) != null;
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
