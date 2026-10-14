package Render2D;

import Render3D.map.MapConfig;

/**
 * Comprobaciones headless de la paleta del minimapa.
 *
 * <p>Devuelve 1 si algo falla, 0 si todo pasa.
 */
public final class MinimapPaletteChecks {

    private static int failures = 0;

    public static void main(String[] args) {
        cadaTipoTieneColor();
        laVentanaEsDelColorDelMuro();
        laPuertaEsUnPeloMasClaraQueElMuro();
        railingEsVioleta();
        murosVentanasPuertasYRailingsSonDistintos();
        laLlaveDestaque();
        coloresEnRango();
        estructuraIncluyeSilueta();

        if (failures > 0) {
            System.out.println("MinimapPaletteChecks: " + failures + " fallo(s)");
            System.exit(1);
        }
        System.out.println("MinimapPaletteChecks: OK");
    }

    /** Ningún símbolo del mapa puede quedarse sin color (nada de NaN ni null). */
    private static void cadaTipoTieneColor() {
        for (MapConfig.SymbolType t : MapConfig.SymbolType.values()) {
            float[] c = Minimap.colorDe(t);
            check(c != null, "color de " + t + " no es null");
            check(c.length == 4, "color de " + t + " tiene 4 canales");
            for (float f : c) {
                check(!Float.isNaN(f), "color de " + t + " sin NaN");
                check(f >= 0f && f <= 1f,
                        "canal de " + t + " en 0..1, vale " + f);
            }
            check(c[3] > 0f, "color de " + t + " es visible (alfa > 0)");
        }
    }

    /**
     * La ventana tiene que ser EXACTAMENTE el color del muro: es una pared con
     * un hueco, no otro tipo de muro, y en el plano forma parte de la misma
     * silueta.
     */
    private static void laVentanaEsDelColorDelMuro() {
        float[] w = Minimap.colorDe(MapConfig.SymbolType.H_WALL);
        float[] win = Minimap.colorDe(MapConfig.SymbolType.WINDOW);
        for (int i = 0; i < 4; i++) {
            check(w[i] == win[i],
                    "canal " + i + ": la ventana tiene el color del muro");
        }
    }

    /**
     * La puerta es del mismo tono que el muro pero un poco más clara. Ni igual
     * (entonces no se ve el paso) ni un color propio (entonces el plano parece
     * un semáforo).
     */
    private static void laPuertaEsUnPeloMasClaraQueElMuro() {
        float[] wall = Minimap.colorDe(MapConfig.SymbolType.H_WALL);
        float[] door = Minimap.colorDe(MapConfig.SymbolType.DOOR);

        check(door[0] > wall[0] && door[1] > wall[1] && door[2] > wall[2],
                "la puerta es más clara que el muro en los tres canales");

        float d = distancia(wall, door);
        // Cerca, pero no idéntica: tiene que verse el paso.
        check(d > 0.02f, "la puerta se distingue del muro (distancia=" + d + ")");
        check(d < 0.35f,
                "la puerta es solo un poco más clara, no otro color (distancia=" + d + ")");
    }

    /**
     * El railing es violeta: más rojo y más azul que verde, y claramente por
     * encima del muro en la componente azul.
     */
    private static void railingEsVioleta() {
        float[] r = Minimap.colorDe(MapConfig.SymbolType.RAILING);
        float[] wall = Minimap.colorDe(MapConfig.SymbolType.H_WALL);

        check(r[2] > r[1] && r[0] > r[1],
                "el railing tira a violeta (rojo y azul por encima del verde): "
                        + "r=" + r[0] + " g=" + r[1] + " b=" + r[2]);
        check(r[2] > wall[2] + 0.15f,
                "el railing se separa del muro en la componente azul");
        check(r[0] > wall[0],
                "el railing tiene más rojo que el muro");
    }

    /**
     * Estructura (muro, ventana y puerta) y railing tienen que distinguirse,
     * pero puertas y ventanas comparten tono a propósito, así que no se comparan
     * entre sí.
     */
    private static void murosVentanasPuertasYRailingsSonDistintos() {
        MapConfig.SymbolType[][] pares = {
                {MapConfig.SymbolType.H_WALL, MapConfig.SymbolType.RAILING},
                {MapConfig.SymbolType.H_WALL, MapConfig.SymbolType.DOOR},
                {MapConfig.SymbolType.WINDOW, MapConfig.SymbolType.RAILING},
                {MapConfig.SymbolType.DOOR, MapConfig.SymbolType.RAILING}
        };
        for (MapConfig.SymbolType[] par : pares) {
            float d = distancia(Minimap.colorDe(par[0]), Minimap.colorDe(par[1]));
            check(d >= 0.05f, par[0] + " y " + par[1]
                    + " se distinguen (distancia=" + d + ")");
        }
    }

    /**
     * La llave tiene que destacar de todo lo demás: es lo que el jugador busca,
     * y el railing violeta se parece a un color de objetivo.
     */
    private static void laLlaveDestaque() {
        MapConfig.SymbolType[] resto = {
                MapConfig.SymbolType.H_WALL,
                MapConfig.SymbolType.WINDOW,
                MapConfig.SymbolType.DOOR,
                MapConfig.SymbolType.RAILING,
                MapConfig.SymbolType.FLOOR_CEILING,
                MapConfig.SymbolType.FLOOR_ONLY
        };
        float[] key = Minimap.colorDe(MapConfig.SymbolType.ITEM);
        for (MapConfig.SymbolType t : resto) {
            float d = distancia(key, Minimap.colorDe(t));
            check(d >= 0.15f, "la llave destaca sobre " + t
                    + " (distancia=" + d + ")");
        }
    }

    private static float distancia(float[] a, float[] b) {
        return (float) Math.sqrt(sq(a[0] - b[0]) + sq(a[1] - b[1]) + sq(a[2] - b[2]));
    }

    /** El muro tiene que destacar contra el fondo del panel. */
    private static void coloresEnRango() {
        float[] wall = Minimap.colorDe(MapConfig.SymbolType.H_WALL);
        float[] floor = Minimap.colorDe(MapConfig.SymbolType.FLOOR_CEILING);
        float d = distancia(wall, floor);
        check(d >= 0.15f, "el muro se distingue del suelo (distancia=" + d + ")");

        float[] floorOnly = Minimap.colorDe(MapConfig.SymbolType.FLOOR_ONLY);
        float d2 = distancia(floorOnly, floor);
        check(d2 >= 0.05f,
                "el suelo sin techo se distingue del suelo con techo (distancia=" + d2 + ")");
    }

    /** La silueta se pinta: muro, ventana y puerta cuentan como estructura. */
    private static void estructuraIncluyeSilueta() {
        check(Minimap.isStructure(MapConfig.SymbolType.H_WALL), "■ es estructura");
        check(Minimap.isStructure(MapConfig.SymbolType.V_WALL), "◙ es estructura");
        check(Minimap.isStructure(MapConfig.SymbolType.WINDOW), "▣ es estructura");
        check(Minimap.isStructure(MapConfig.SymbolType.DOOR), "◧ es estructura");
        check(!Minimap.isStructure(MapConfig.SymbolType.RAILING), "◰ no es muro");
        check(!Minimap.isStructure(MapConfig.SymbolType.EMPTY), "□ no es estructura");
        check(!Minimap.isStructure(MapConfig.SymbolType.FLOOR_CEILING),
                "el suelo no es estructura");
        check(!Minimap.isStructure(null), "null no es estructura");
    }

    private static float sq(float v) {
        return v * v;
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + what);
            failures++;
        }
    }

    private MinimapPaletteChecks() {
    }
}