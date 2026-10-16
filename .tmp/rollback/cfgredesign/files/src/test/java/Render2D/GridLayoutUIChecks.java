package Render2D;

import Render2D.layout.GridLayoutUI;
import Render2D.components.UIComponent;

/**
 * Comprobaciones headless de {@link GridLayoutUI}.
 *
 * <p>No necesita contexto de OpenGL: la cuadrica solo reparte rectangulos, asi
 * que se puede comprobar con componentes falsos que solo guardan su_bounds.
 *
 * <p>Cubre lo que estaba rota en los menus:
 * <ol>
 *   <li>Un rotulo mas ancho que sus companeros ESTIRA su columna en lugar de
 *       salirse de su celda.</li>
 *   <li>Ningun par de celdas contiguas se solapa, ni en horizontal ni en
 *       vertical, sea cual sea el tamano de cada componente.</li>
 *   <li>El hueco declarado entre celdas se respeta de verdad.</li>
 *   <li>El elemento {@code i} cae en {@code col = i % columnas},
 *       {@code fila = i / columnas}.</li>
 *   <li>El centro de la cuadrica coincide con el centro de la ventana.</li>
 *   <li>La columna la marca el componente mas ancho y nadie pierde su tamano.</li>
 *   <li>Todo queda dentro de la ventana virtual.</li>
 * </ol>
 */
public final class GridLayoutUIChecks {

    private static int fallos;

    public static void main(String[] args) {
        columnaEstiraConElRotulo();
        sinSolapes();
        huecosRespetados();
        ordenDeLectura();
        centradoEnLaVentana();
        laColumnaLaMarcaElMasAncho();
        todoCabeEnLaVentana();

        if (fallos == 0) {
            System.out.println("OK: todas las comprobaciones de la cuadrica pasan.");
        } else {
            System.out.println("FALLO: " + fallos + " comprobacion(es) de la cuadrica.");
            System.exit(1);
        }
    }

    /**
     * La columna del centro tiene que valer lo que mida su rotulo, no una
     * medida puesta a mano. Es el caso que rompia los menus: el texto era mas
     * ancho que el hueco reservado y se montaba sobre las flechas.
     */
    private static void columnaEstiraConElRotulo() {
        FakeButton prev = new FakeButton(56, 56);
        FakeLabel label = new FakeLabel(260, 30);
        FakeButton next = new FakeButton(56, 56);

        GridLayoutUI grid = new GridLayoutUI(3,
                new UIComponent[]{prev, label, next}, 18f, 0f);
        grid.setOrigen(0, 0).apply();

        // La columna central es la del rotulo: por eso el total sale de el.
        float esperado = 56 + 18 + 260 + 18 + 56;
        cerca("ancho total con rotulo ancho", esperado, grid.getAnchoTotal(), 0.01f);

        // Y el rotulo se queda dentro de su columna: su borde derecho cae en el
        // hueco que precede a la flecha, no mas alla.
        float finRotulo = label.boundsX + label.boundsAncho;
        cerca("borde derecho del rotulo", 334f, finRotulo, 0.01f);
        ok(finRotulo <= next.boundsX,
                "el rotulo no llega a la flecha que tiene al lado");
    }

    /** Ninguna celda puede pisar a la siguiente en ninguna de las dos direcciones. */
    private static void sinSolapes() {
        FakeButton[] cs = new FakeButton[6];
        // Tamaños dispares a proposito: es lo que hacia que un rotulo largo se
        // montara sobre un boton.
        float[] anchos = {40, 300, 44, 90, 250, 40};
        float[] altos = {30, 30, 70, 30, 22, 90};
        for (int i = 0; i < cs.length; i++) {
            cs[i] = new FakeButton(anchos[i], altos[i]);
        }

        GridLayoutUI grid = new GridLayoutUI(3, cs, 15f, 11f);
        grid.setOrigen(0, 0).apply();

        for (int i = 0; i < cs.length; i++) {
            for (int j = i + 1; j < cs.length; j++) {
                boolean solapaX = solapan(cs[i].boundsX, cs[i].boundsX + cs[i].boundsAncho,
                        cs[j].boundsX, cs[j].boundsX + cs[j].boundsAncho);
                boolean solapaY = solapan(cs[i].boundsY, cs[i].boundsY + cs[i].boundsAlto,
                        cs[j].boundsY, cs[j].boundsY + cs[j].boundsAlto);
                ok(!(solapaX && solapaY),
                        "elementos " + i + " y " + j + " no se solapan");
            }
        }
    }

    /** El hueco pedido entre celdas es el hueco que hay. */
    private static void huecosRespetados() {
        FakeButton a = new FakeButton(50, 50);
        FakeButton b = new FakeButton(50, 50);

        GridLayoutUI grid = new GridLayoutUI(2, new UIComponent[]{a, b}, 30f, 0f);
        grid.setOrigen(0, 0).apply();

        cerca("hueco horizontal entre columnas", 30f, b.boundsX - (a.boundsX + a.boundsAncho),
                0.01f);

        GridLayoutUI dosFilas = new GridLayoutUI(1, new UIComponent[]{a, b}, 0f, 40f);
        dosFilas.setOrigen(0, 0).apply();
        cerca("hueco vertical entre filas", 40f, b.boundsY - (a.boundsY + a.boundsAlto),
                0.01f);
    }

    /** El elemento i va a la columna i%columnas y a la fila i/columnas. */
    private static void ordenDeLectura() {
        FakeButton[] cs = new FakeButton[5];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = new FakeButton(40, 40);
        }

        GridLayoutUI grid = new GridLayoutUI(3, cs, 20f, 0f);
        grid.setOrigen(0, 0).apply();

        int filas = grid.getFilas();
        igual("filas con 5 elementos en 3 columnas", 2, filas);

        for (int i = 0; i < cs.length; i++) {
            int colEsperada = i % 3;
            int filaEsperada = i / 3;

            float xEsperado = colEsperada * 60f;
            float yEsperado = filaEsperada * 40f;
            cerca("columna del elemento " + i, xEsperado, cs[i].boundsX, 0.01f);
            cerca("fila del elemento " + i, yEsperado, cs[i].boundsY, 0.01f);
        }
    }

    /** center() deja el bloque en el medio de la ventana, no desplazado. */
    private static void centradoEnLaVentana() {
        FakeButton[] cs = new FakeButton[3];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = new FakeButton(100, 40);
        }

        GridLayoutUI grid = new GridLayoutUI(1, cs, 0f, 10f);
        grid.center(800, 600);

        float izquierda = cs[0].boundsX;
        float derecha = 800 - (cs[2].boundsX + cs[2].boundsAncho);
        cerca("margen izquierdo", derecha, izquierda, 0.01f);

        float arriba = cs[0].boundsY;
        float abajo = 600 - (cs[2].boundsY + cs[2].boundsAlto);
        cerca("margen superior", abajo, arriba, 0.01f);
    }

    /**
     * El componente que mas mide marca la celda. Un componente grande no se
     * recorta ni invade al vecino: ensancha su columna y el vecino se aparta.
     */
    private static void laColumnaLaMarcaElMasAncho() {
        FakeButton pequeno = new FakeButton(40, 40);
        FakeButton grande = new FakeButton(400, 40);

        GridLayoutUI grid = new GridLayoutUI(2, new UIComponent[]{pequeno, grande}, 0f, 0f);
        grid.setOrigen(0, 0).apply();

        // Cada columna vale lo que mide el mas ancho que le toca, asi que el
        // total es la suma de las dos.
        cerca("la columna del pequeno vale su ancho", 40f, pequeno.boundsAncho, 0.01f);
        cerca("la columna del grande vale su ancho", 400f, grande.boundsAncho, 0.01f);
        cerca("ancho total", 440f, grid.getAnchoTotal(), 0.01f);

        // El pequeno conserva su sitio y el grande empieza justo donde acaba.
        cerca("borde derecho del pequeno", 40f, pequeno.boundsX + pequeno.boundsAncho,
                0.01f);
        cerca("columna del grande", 40f, grande.boundsX, 0.01f);

        ok(!solapan(pequeno.boundsX, pequeno.boundsX + pequeno.boundsAncho,
                grande.boundsX, grande.boundsX + grande.boundsAncho),
                "el grande y el pequeno no se pisan");

        // Nadie pierde su tamano: un boton debe seguir siendo el boton que es.
        cerca("el grande conserva su ancho", 400f, grande.getAncho(), 0.01f);
        cerca("el pequeno conserva su ancho", 40f, pequeno.getAncho(), 0.01f);
    }

    /** Nada puede salirse de la ventana virtual. */
    private static void todoCabeEnLaVentana() {
        FakeButton[] cs = new FakeButton[4];
        String[] textos = {"Continuar", "Reiniciar Tiempo", "Salir", "Reiniciar"};
        for (int i = 0; i < cs.length; i++) {
            cs[i] = new FakeButton(40 + textos[i].length() * 18f, 52f);
        }

        GridLayoutUI grid = new GridLayoutUI(2, cs, 20f, 20f);
        grid.center(1024, 768);

        for (int i = 0; i < cs.length; i++) {
            FakeButton c = cs[i];
            ok(c.boundsX >= 0f && c.boundsY >= 0f
                    && c.boundsX + c.boundsAncho <= 1024.01f
                    && c.boundsY + c.boundsAlto <= 768.01f,
                    "elemento " + i + " cabe en la ventana");
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static boolean solapan(float aIni, float aFin, float bIni, float bFin) {
        return aIni < bFin && bIni < aFin;
    }

    private static void ok(boolean condicion, String que) {
        if (condicion) {
            System.out.println("  ok: " + que);
        } else {
            System.out.println("  FALLO: " + que);
            fallos++;
        }
    }

    private static void igual(String que, Object esperado, Object obtenido) {
        ok(esperado.equals(obtenido), que + " (esperado " + esperado + ", obtenido " + obtenido + ")");
    }

    private static void cerca(String que, float esperado, float obtenido, float tolerancia) {
        ok(Math.abs(esperado - obtenido) <= tolerancia,
                que + " (esperado " + esperado + ", obtenido " + obtenido + ")");
    }

    /** Componente de mentira con el_bounds que le asigne la cuadrica. */
    private static class FakeButton implements UIComponent {

        private float ancho;
        private final float alto;
        float boundsX;
        float boundsY;
        float boundsAncho;
        float boundsAlto;

        FakeButton(float ancho, float alto) {
            this.ancho = ancho;
            this.alto = alto;
        }

        @Override
        public void setBounds(float x, float y, float w, float h) {
            this.boundsX = x;
            this.boundsY = y;
            this.boundsAncho = w;
            this.boundsAlto = h;
        }

        @Override
        public float getAncho() {
            return ancho;
        }

        @Override
        public float getAlto() {
            return alto;
        }

        @Override
        public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        }
    }

    /** Rotulo de mentira con un alto propio. */
    private static final class FakeLabel extends FakeButton {

        FakeLabel(float ancho, float alto) {
            super(ancho, alto);
        }
    }
}