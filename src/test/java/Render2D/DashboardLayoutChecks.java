package Render2D;

import Render2D.layout.BorderLayout;
import Render2D.layout.GridLayout;
import Render2D.ui.Panel;
import Render2D.ui.UIComponent;

/**
 * Comprobaciones headless del reparto del dashboard.
 *
 * <p>No necesita OpenGL: las maquetas solo reparten rectangulos, asi que se
 * pueden comprobar con componentes falsos que guardan su rectangulo. Cubre lo
 * que se pidio para el menu principal:
 *
 * <ul>
 *   <li>{@link BorderLayout} deja el dashboard en la franja izquierda con el 34%
 *       del ancho y el centro con el resto.</li>
 *   <li>El {@link GridLayout} de una columna con {@code fillX} da a todos los
 *       botones el mismo ancho, aunque su texto sea distinto.</li>
 *   <li>Las filas sin {@code fillY} conservan su alto natural y el bloque queda
 *       centrado en el panel.</li>
 *   <li>Una fila empaquetada (flecha, rotulo, flecha) mide lo que pide su
 *       contenido y se centra, sin comerse el ancho del panel.</li>
 *   <li>El {@link Panel} reserva su margen interior y mide a sus hijos.</li>
 * </ul>
 */
public final class DashboardLayoutChecks {

    private static int fallos = 0;

    public static void main(String[] args) {
        borderParteElAncho();
        columnaEstiraLosBotones();
        filasNaturalesCentradas();
        filaEmpaquetadaSeCentra();
        panelReservaSuMargen();

        if (fallos > 0) {
            System.out.println("DashboardLayoutChecks: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("DashboardLayoutChecks: OK");
    }

    /** El dashboard ocupa el 34% de la izquierda; el centro, el resto. */
    private static void borderParteElAncho() {
        Fake dashboard = new Fake(0f, 0f);
        BorderLayout border = new BorderLayout().setWest(dashboard, 0.34f);
        border.arrange(0f, 0f, 1000f, 700f);

        approx("dashboard x", 0f, dashboard.x);
        approx("dashboard ancho = 34%", 340f, dashboard.w);
        approx("dashboard alto completo", 700f, dashboard.h);
        approx("centro x", 340f, border.getCenterX());
        approx("centro ancho = 66%", 660f, border.getCenterAncho());
        approx("centro alto completo", 700f, border.getCenterAlto());
    }

    /**
     * Con {@code fillX}, una columna reparte todo el ancho: todos los botones
     * miden lo mismo aunque uno sea "<" y otro "Dificultad: Normal".
     */
    private static void columnaEstiraLosBotones() {
        Fake jugar = new Fake(80f, 52f);
        Fake fila = new Fake(200f, 52f);
        Fake dif = new Fake(160f, 52f);
        GridLayout col = new GridLayout(1, 0f, 22f, true, false)
                .add(jugar).add(fila).add(dif);
        col.arrange(0f, 0f, 340f, 500f);

        approx("ancho jugar", 340f, jugar.w);
        approx("ancho fila", 340f, fila.w);
        approx("ancho dificultad", 340f, dif.w);
        approx("misma x", jugar.x, dif.x);
        approx("alto natural jugar", 52f, jugar.h);
        approx("alto natural fila", 52f, fila.h);
    }

    /**
     * Sin {@code fillY} las filas conservan su alto y el bloque (3 x 52 + 2
     * huecos = 200) queda centrado en el panel de 500.
     */
    private static void filasNaturalesCentradas() {
        Fake a = new Fake(80f, 52f);
        Fake b = new Fake(80f, 52f);
        Fake c = new Fake(80f, 52f);
        GridLayout col = new GridLayout(1, 0f, 22f, true, false)
                .add(a).add(b).add(c);
        col.arrange(0f, 0f, 300f, 500f);

        float alto = 52f * 3f + 22f * 2f;
        float start = (500f - alto) / 2f;
        approx("primera fila y", start, a.y);
        approx("segunda fila y", start + 52f + 22f, b.y);
        approx("tercera fila y", start + (52f + 22f) * 2f, c.y);
    }

    /** La fila de selector mide su contenido y se centra en el ancho del panel. */
    private static void filaEmpaquetadaSeCentra() {
        Fake prev = new Fake(56f, 52f);
        Fake rotulo = new Fake(150f, 20f);
        Fake next = new Fake(56f, 52f);
        GridLayout fila = new GridLayout(3, 18f, 0f, false, false)
                .add(prev).add(rotulo).add(next);
        fila.arrange(0f, 0f, 340f, 52f);

        float bloque = 56f + 150f + 56f + 18f * 2f;
        approx("fila empaquetada = suma + huecos", bloque, fila.getAnchoTotal());
        float start = (340f - bloque) / 2f;
        approx("flecha izquierda x", start, prev.x);
        approx("rotulo x", start + 56f + 18f, rotulo.x);
        approx("flecha derecha x", start + 56f + 18f + 150f + 18f, next.x);
        approx("alto flecha natural", 52f, prev.h);
    }

    /** El panel reserva su margen interior antes de repartir a los hijos. */
    private static void panelReservaSuMargen() {
        Fake hijo = new Fake(50f, 20f);
        Panel panel = new Panel()
                .setPadding(10f, 20f, 30f, 40f)
                .setLayout(new GridLayout(1, 0f, 0f, true, false).add(hijo));
        panel.setBounds(5f, 7f, 300f, 200f);

        approx("panel ancho", 300f, panel.getAncho());
        approx("hijo x = panel + pad izq", 15f, hijo.x);
        // Sin fillY la unica fila (20) se centra en el hueco interior
        // (200 - 30 arriba - 40 abajo = 130): 37 + (130 - 20) / 2 = 92.
        approx("hijo y centrado en el hueco interior", 92f, hijo.y);
        approx("hijo ancho = panel - margenes", 270f, hijo.w);
        check(panel.getLayout().components().contains(hijo),
                "el panel expone a los hijos de su maqueta para poder dibujarlos");

        // Sin bounds asignado, el panel mide a su contenido.
        Panel suelto = new Panel().setPadding(6f).setLayout(
                new GridLayout(1, 0f, 0f, true, false).add(new Fake(50f, 20f)));
        approx("panel suelto ancho natural", 62f, suelto.getAncho());
        approx("panel suelto alto natural", 32f, suelto.getAlto());
    }

    // ---- Utilidades -------------------------------------------------------

    /** Componente falso: guarda su rectangulo. Antes de colocarlo, su tamano natural. */
    private static final class Fake implements UIComponent {
        float x;
        float y;
        float w;
        float h;

        Fake(float ancho, float alto) {
            this.w = ancho;
            this.h = alto;
        }

        @Override
        public void setBounds(float x, float y, float ancho, float alto) {
            this.x = x;
            this.y = y;
            this.w = ancho;
            this.h = alto;
        }

        @Override
        public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        }

        @Override
        public float getAncho() {
            return w;
        }

        @Override
        public float getAlto() {
            return h;
        }
    }

    private static void approx(String que, float esperado, float real) {        if (Math.abs(esperado - real) > 1e-3f) {
            System.out.println("  FALLA " + que + ": esperado " + esperado + ", real " + real);
            fallos++;
        } else {
            System.out.println("  OK    " + que);
        }
    }

    private static void check(boolean condicion, String que) {
        if (condicion) {
            System.out.println("  OK    " + que);
        } else {
            System.out.println("  FALLA " + que);
            fallos++;
        }
    }

    private DashboardLayoutChecks() {
    }
}
