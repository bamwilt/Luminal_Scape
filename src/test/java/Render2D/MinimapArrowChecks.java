package Render2D;

import Render3D.map.MapConfig;

/**
 * Comprobaciones headless de la flecha del minimapa.
 *
 * <p>Ejecución: se compila el proyecto y se lanza esta clase con el
 * classpath de test. Devuelve 1 si algo falla, 0 si todo pasa.
 *
 * <p>Cubre los dos fallos que tenía la flecha:
 * <ul>
 *   <li>la punta seAdvanced invertía la Z (en el minimapa +Z es ABAJO, igual
 *       que en pantalla, así que invertirla señalaba hacia atrás);</li>
 *   <li>la perpendicular era el intercambio de componentes, que solo es
 *       perpendicular a 45°.</li>
 * </ul>
 */
public final class MinimapArrowChecks {

    private static int failures = 0;

    public static void main(String[] args) {
        centroideSobreElPlayer();
        puntaSigueAlYaw();
        baseEsPerpendicularAlFrente();
        baseSimetricaYATras();
        yawIndefinidoNoRompeNada();
        marcadorSeEncajaALaCelda();
        giroSeEncajaA90();
        soloSePisaElPiso();
        marcadorNuncaSobreMuroOAire();

        if (failures > 0) {
            System.out.println("MinimapArrowChecks: " + failures + " fallo(s)");
            System.exit(1);
        }
        System.out.println("MinimapArrowChecks: OK");
    }

    /**
     * El marcador se encaja al centro de la celda: el índice sale de
     * floor((coord - origen) / TILE_SIZE), para que salte de casilla en casilla
     * aunque el player se mueva fino.
     */
    private static void marcadorSeEncajaALaCelda() {
        float t = MapConfig.TILE_SIZE;
        approx("media celda sigue en la 0", 0f, Minimap.cellIndex(0.9f * t, 0f), 0f);
        approx("la celda 1 empieza en 1*TILE", 1f, Minimap.cellIndex(1.0f * t, 0f), 0f);
        approx("la celda 3 por el medio", 3f, Minimap.cellIndex(3.4f * t, 0f), 0f);
        approx("justo por debajo del origen es la -1", -1f, Minimap.cellIndex(-0.01f, 0f), 0f);
        float origin = -7.5f;
        approx("el origen se resta", 2f, Minimap.cellIndex(origin + 2.4f * t, origin), 0f);
    }

    /**
     * La flecha solo mira en 4 direcciones: cualquier yaw se encaja al múltiplo
     * de 90° más cercano y el resultado queda en [0, 360).
     */
    private static void giroSeEncajaA90() {
        approx("yaw 0", 0f, Minimap.snapYaw90(0f), 0f);
        approx("44 se va a 0", 0f, Minimap.snapYaw90(44f), 0f);
        approx("46 se va a 90", 90f, Minimap.snapYaw90(46f), 0f);
        approx("89 se va a 90", 90f, Minimap.snapYaw90(89f), 0f);
        approx("180 se queda", 180f, Minimap.snapYaw90(180f), 0f);
        approx("270 se queda", 270f, Minimap.snapYaw90(270f), 0f);
        approx("359 se va a 0", 0f, Minimap.snapYaw90(359f), 0f);
        approx("-90 se normaliza a 270", 270f, Minimap.snapYaw90(-90f), 0f);
        approx("450 se normaliza a 90", 90f, Minimap.snapYaw90(450f), 0f);
        for (float yaw = -720f; yaw <= 720f; yaw += 1f) {
            float s = Minimap.snapYaw90(yaw);
            check(Math.abs(s % 90f) < 1e-4f, "el encaje es múltiplo de 90 (yaw=" + yaw + ")");
            check(s >= 0f && s < 360f, "el encaje queda en [0,360) (yaw=" + yaw + " -> " + s + ")");
        }
    }

    /** El centroide tiene que caer en la posición del player, no desplazado. */
    private static void centroideSobreElPlayer() {
        for (float yaw = 0; yaw < 360; yaw += 7.5f) {
            float cx = 137.25f;
            float cy = 402.5f;
            float cellPx = 9.4f;
            float[] v = frente(cx, cy, cellPx, yaw);

            float gx = (v[0] + v[2] + v[4]) / 3f;
            float gy = (v[1] + v[3] + v[5]) / 3f;
            check(!Float.isNaN(gx) && !Float.isNaN(gy),
                    "centroide finito en yaw=" + yaw);
            approx("centroide x yaw=" + yaw, cx, gx, 1e-3f);
            approx("centroide y yaw=" + yaw, cy, gy, 1e-3f);
        }
    }

    /**
     * La punta tiene que estar en la dirección del frente, y no en la contraria.
     * Esta es la comprobación que cazaba la Z invertida.
     */
    private static void puntaSigueAlYaw() {
        for (float yaw = 0; yaw < 360; yaw += 7.5f) {
            float cx = 50f;
            float cy = 60f;
            float cellPx = 10f;
            float[] v = frente(cx, cy, cellPx, yaw);

            double rad = Math.toRadians(yaw);
            float fwdX = (float) Math.cos(rad);
            float fwdY = (float) Math.sin(rad);

            float tipX = v[0] - cx;
            float tipY = v[1] - cy;
            // Producto escalar: positivo si la punta va hacia delante.
            float dot = tipX * fwdX + tipY * fwdY;
            check(dot > 0f, "la punta va hacia delante en yaw=" + yaw
                    + " (dot=" + dot + ")");

            // Y tiene que ir justo en la dirección, sin desvío lateral.
            float len = (float) Math.hypot(tipX, tipY);
            float cross = Math.abs(tipX * fwdY - tipY * fwdX);
            check(cross < 1e-3f * Math.max(1f, len),
                    "la punta no se desvía en yaw=" + yaw + " (cross=" + cross + ")");
        }

        // Casos que se deben leer de un vistazo en el informe.
        float[] este = frente(0f, 0f, 10f, 0f);
        approx("yaw 0 apunta a +X (punta x)", 7.5f, este[0], 1e-3f);
        approx("yaw 0 punta y = origen", 0f, este[1], 1e-3f);

        float[] abajo = frente(0f, 0f, 10f, 90f);
        approx("yaw 90 punta y = +Y (abajo)", 7.5f, abajo[1], 1e-3f);
        approx("yaw 90 punta x = origen", 0f, abajo[0], 1e-3f);

        float[] arriba = frente(0f, 0f, 10f, -90f);
        approx("yaw -90 punta y = -Y (arriba)", -7.5f, arriba[1], 1e-3f);

        float[] izquierda = frente(0f, 0f, 10f, 180f);
        approx("yaw 180 punta x = -X", -7.5f, izquierda[0], 1e-3f);
    }

    /**
     * Los dos vértices de la base tienen que estar a la misma distancia del
     * frente y en los dos lados: si la perpendicular no lo es, la cabeza se
     * deforma al girar.
     */
    private static void baseEsPerpendicularAlFrente() {
        for (float yaw = 0; yaw < 360; yaw += 3.75f) {
            float cx = 20f;
            float cy = 30f;
            float cellPx = 8f;
            float[] v = frente(cx, cy, cellPx, yaw);

            double rad = Math.toRadians(yaw);
            float fwdX = (float) Math.cos(rad);
            float fwdY = (float) Math.sin(rad);

            float b1x = v[2] - cx;
            float b1y = v[3] - cy;
            float b2x = v[4] - cx;
            float b2y = v[5] - cy;

            float dot1 = b1x * fwdX + b1y * fwdY;
            float dot2 = b2x * fwdX + b2y * fwdY;
            check(dot1 < 0f, "la base queda ATRÁS del player en yaw=" + yaw
                    + " (dot1=" + dot1 + ")");
            check(dot2 < 0f, "la base queda ATRÁS del player en yaw=" + yaw
                    + " (dot2=" + dot2 + ")");
            approx("ambos vértices de base al mismo fondo (yaw=" + yaw + ")",
                    dot1, dot2, 1e-3f);

            // Perpendicularidad estricta: el avance a lo largo del frente debe
            // ser idéntico en los dos (ya comprobado) y la separación lateral
            // debe ser simétrica y de magnitud ARROW_HALF_WIDTH * cellPx.
            float halfWidth = cellPx * 0.42f;
            float side1 = Math.abs(b1x * -fwdY + b1y * fwdX);
            float side2 = Math.abs(b2x * -fwdY + b2y * fwdX);
            approx("anchura de base lado 1 (yaw=" + yaw + ")", halfWidth, side1, 1e-3f);
            approx("anchura de base lado 2 (yaw=" + yaw + ")", halfWidth, side2, 1e-3f);

            // Los dos lados deben caer en sentidos opuestos.
            float s1 = b1x * -fwdY + b1y * fwdX;
            float s2 = b2x * -fwdY + b2y * fwdX;
            check(s1 * s2 < 0f, "la base se abre a los dos lados en yaw=" + yaw);
        }
    }

    /** La punta tiene que ser el vértice más alejado del player. */
    private static void baseSimetricaYATras() {
        for (float yaw = 0; yaw < 360; yaw += 7.5f) {
            float cx = 12f;
            float cy = 12f;
            float cellPx = 11f;
            float[] v = frente(cx, cy, cellPx, yaw);

            float dTip = dist(cx, cy, v[0], v[1]);
            float dB1 = dist(cx, cy, v[2], v[3]);
            float dB2 = dist(cx, cy, v[4], v[5]);

            check(dTip > dB1 && dTip > dB2,
                    "la punta es la más lejana en yaw=" + yaw
                            + " (" + dTip + " vs " + dB1 + "/" + dB2 + ")");
            approx("base simétrica (yaw=" + yaw + ")", dB1, dB2, 1e-3f);
            approx("distancia de la punta = ARROW_LENGTH * cellPx (yaw=" + yaw + ")",
                    cellPx * 0.75f, dTip, 1e-3f);
        }
    }

    /** Un frente de longitud cero no debe producir NaN en pantalla. */
    private static void yawIndefinidoNoRompeNada() {
        float[] v = Minimap.arrowVertices(5f, 5f, 10f, 0f, 0f);
        for (float f : v) {
            check(!Float.isNaN(f) && !Float.isInfinite(f),
                    "frente cero produce un valor finito: " + f);
        }
        approx("sin frente, mira a +X", 5f + 7.5f, v[0], 1e-3f);
    }

    /** Solo son piso las celdas sobre las que se está de pie. */
    private static void soloSePisaElPiso() {
        MapConfig.SymbolType[] piso = {
            MapConfig.SymbolType.FLOOR_CEILING, MapConfig.SymbolType.FLOOR_ONLY,
            MapConfig.SymbolType.LIT_CEILING, MapConfig.SymbolType.DECORATION,
            MapConfig.SymbolType.SPAWN_N, MapConfig.SymbolType.ITEM
        };
        for (MapConfig.SymbolType t : piso) {
            check(Minimap.esPiso(t), "es piso: " + t);
        }
        MapConfig.SymbolType[] noPiso = {
            MapConfig.SymbolType.EMPTY, MapConfig.SymbolType.H_WALL,
            MapConfig.SymbolType.V_WALL, MapConfig.SymbolType.WINDOW,
            MapConfig.SymbolType.DOOR, MapConfig.SymbolType.RAILING
        };
        for (MapConfig.SymbolType t : noPiso) {
            check(!Minimap.esPiso(t), "no es piso: " + t);
        }
        check(!Minimap.esPiso(null), "null no es piso");
    }

    /**
     * El marcador se encaja al piso más cercano: si el centro del player cae
     * dentro de un muro (al ir pegado a él) o sobre el aire, el triángulo salta a
     * la celda pisable más próxima en vez de pintarse sobre el muro.
     */
    private static void marcadorNuncaSobreMuroOAire() {
        MapConfig.SymbolType W = MapConfig.SymbolType.H_WALL;
        MapConfig.SymbolType F = MapConfig.SymbolType.FLOOR_CEILING;
        MapConfig.SymbolType V = MapConfig.SymbolType.EMPTY;

        // 3x3 con una sola celda de piso rodeada de muro y aire.
        MapConfig.SymbolType[][] isla = {
            { W, W, W },
            { W, F, W },
            { V, V, V },
        };
        Minimap.CellQuery q = (r, c) -> isla[r][c];

        int[] enPiso = Minimap.nearestFloorCell(1.5f, 1.4f, 3, 3, q);
        check(enPiso != null && enPiso[0] == 1 && enPiso[1] == 1,
                "dentro del piso se queda en el piso");

        int[] desdeMuro = Minimap.nearestFloorCell(1.5f, 2.3f, 3, 3, q);
        check(desdeMuro != null && desdeMuro[0] == 1 && desdeMuro[1] == 1,
                "centro en el muro -> salta al piso contiguo");

        int[] desdeAire = Minimap.nearestFloorCell(2.5f, 1.5f, 3, 3, q);
        check(desdeAire != null && desdeAire[0] == 1 && desdeAire[1] == 1,
                "centro sobre el aire -> salta al piso");

        // Fila con dos pisos: se elige el de centro más próximo.
        MapConfig.SymbolType[][] fila = { { F, W, V, F, W } };
        Minimap.CellQuery qf = (r, c) -> fila[r][c];
        int[] izq = Minimap.nearestFloorCell(0.5f, 0.2f, 1, 5, qf);
        check(izq != null && izq[1] == 0, "se elige el piso de la izquierda");
        int[] der = Minimap.nearestFloorCell(0.5f, 3.4f, 1, 5, qf);
        check(der != null && der[1] == 3, "se elige el piso de la derecha");
        int[] medio = Minimap.nearestFloorCell(0.5f, 1.5f, 1, 5, qf);
        check(medio != null && medio[1] == 0,
                "desde el muro intermedio gana el piso más cercano (izquierda)");

        // Sin ningún piso no hay a dónde saltar.
        MapConfig.SymbolType[][] sinPiso = { { W, V, W } };
        check(Minimap.nearestFloorCell(0.5f, 1.5f, 1, 3, (r, c) -> sinPiso[r][c]) == null,
                "sin piso devuelve null y no se pinta el marcador");
    }

    private static float[] frente(float cx, float cy, float cellPx, float yaw) {
        double rad = Math.toRadians(yaw);
        return Minimap.arrowVertices(cx, cy, cellPx,
                (float) Math.cos(rad), (float) Math.sin(rad));
    }

    private static float dist(float x0, float y0, float x1, float y1) {
        return (float) Math.hypot(x1 - x0, y1 - y0);
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

    private MinimapArrowChecks() {
    }
}