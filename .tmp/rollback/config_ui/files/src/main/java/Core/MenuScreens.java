package Core;

import Render2D.ImageQuad;
import Render2D.TextRenderer;
import Render2D.UIScale;
import Render2D.layout.BorderLayout;
import Render2D.layout.GridLayout;
import Render2D.ui.Button;
import Render2D.ui.ComboBox;
import Render2D.ui.Label;
import Render2D.ui.Panel;

/**
 * Todo lo que se dibuja en la pantalla de titulo y en el menu de pausa.
 *
 * <p>La pantalla de titulo se parte con un {@link BorderLayout}: el dashboard
 * (un {@link Panel} de un azul muy oscuro) ocupa la franja izquierda —el 34% del
 * ancho— y el logo vive en el centro. Dentro del dashboard, un {@link GridLayout}
 * de una sola columna con {@code fillX} estira los botones a lo ancho, de modo
 * que todos miden lo mismo. Ademas de Jugar y de los selectores, el dashboard
 * lleva el boton Salir.
 *
 * <p>El menu de pausa es otro panel con su propio color y una columna de
 * botones: Continuar, Reiniciar Tiempo, Volver al menu principal y Reiniciar.
 * Debajo va un {@link ComboBox} con el selector de resolucion.
 *
 * <p>Los elementos de interfaz (botones, paneles, desplegable) viven en
 * {@code Render2D.ui} y las maquetas en {@code Render2D.layout}.
 */
final class MenuScreens {

    /** Acciones de la partida que disparan los botones. */
    interface Actions {
        void startGame();

        void resumeGame();

        /** Reinicia el tiempo del nivel en curso, sin rehacer la partida. */
        void restartTime();

        void stepLevel(int delta);

        void stepDifficulty(int delta);

        /** Deja la partida y vuelve a la pantalla de titulo. */
        void returnToTitle();

        /** Fija el tamano de la window en pixeles reales. */
        void setResolution(int ancho, int alto);
    }

    /** Separacion vertical entre botones de un menu apilado. */
    private static final float MENU_GAP = 22f;

    /** Separacion horizontal dentro de una fila. */
    private static final float HUECO_ENTRE_COLUMNAS = 18f;

    /** Lado de la celda de las flechas {@code < >}, cuadradas. */
    private static final float CELDA_FLECHA_LADO = 56f;

    /** Espacio vertical que se deja entre bloques sueltos de una pantalla. */
    private static final float HUECO_BLOQUES = 24f;

    /** Margen del borde inferior para las pistas de teclado. */
    private static final float MARGEN_PISTAS = 20f;

    /** Fraccion del ancho de pantalla que ocupa el dashboard del titulo. */
    private static final float DASHBOARD_FRACCION = 0.34f;

    /** Alto del boton que abre el selector de resolucion. */
    private static final float COMBO_ALTO = 52f;

    /** Cuantas resoluciones ofrece el desplegable (alto reservado). */
    private static final int RESOLUCIONES = 5;

    private static final String TITLE_PISTA_1 =
            "Presiona ENTER para jugar | < > para elegir nivel";
    private static final String TITLE_PISTA_2 =
            "WASD - Moverse | Mouse - Rotar cam | P - Menu | Shift - Correr";

    /** Lema bajo el logo. Es texto suelto: el titulo reserva su alto al apilar. */
    private static final String TITLE_TAGLINE =
            "Encuentra los artefactos antes de que se acabe el tiempo";

    private final GameWindow window;
    private final TextRenderer text;
    private final LevelSession levels;
    private final Actions actions;
    private final ImageQuad logo;

    // ---- Menu de pausa ----
    private final Button buttonCerrarMenu;
    private final Button buttonRestartTime;
    private final Button buttonVolverMenu;
    private final Button[] menuButtons = new Button[3];
    private final Button buttonReiniciar;
    private final Panel pausaPanel;
    private final ComboBox comboResolucion;

    // ---- Pantalla de titulo ----
    private final Button buttonJugar;
    private final Button buttonSalir;
    private final Button buttonLevelPrev;
    private final Button buttonLevelNext;
    private final Button buttonDiffPrev;
    private final Button buttonDiffNext;

    private final Label labelLevel;
    private final Label labelDiff;

    // ---- Dashboard del titulo ----
    private final BorderLayout titleBorder;
    private final Panel dashboard;
    private final Panel levelRow;
    private final Panel diffRow;

    /** Borde superior del bloque de botones del menu de pausa. */
    private float menuBloqueTop;
    /** Borde inferior del bloque del menu de pausa. */
    private float menuBloqueBottom;
    /** X del borde izquierdo del logo del titulo, centrado en su region. */
    private float titleLogoX;
    /** Ancho con el que se dibuja el logo del titulo. */
    private float titleLogoW;
    /** Y del borde superior del logo del titulo. */
    private float titleLogoY;
    /** Centro horizontal de la region central, para centrar el lema. */
    private float titleCentroX;
    /** Y donde se dibuja el lema del titulo (arriba de su caja de texto). */
    private float titleTextoY;
    /** Y donde empieza el bloque de las dos pistas de teclado del titulo. */
    private float titlePistasTop;

    /**
     * Píxeles de window por unidad de interfaz.
     *
     * <p>Toda la IU se dibuja en unidades virtuales ({@link UIScale}) y este
     * factor es el que las estira a la window real. Se recalcula en cada resize
     * y también al construir la pantalla, porque el tamaño solo se puede leer de
     * la window ya construida.
     */
    private float uiScale;

    MenuScreens(GameWindow window, TextRenderer text, LevelSession levels, Actions actions) {
        this.window = window;
        this.text = text;
        this.levels = levels;
        this.actions = actions;
        this.logo = new ImageQuad(ConfigGlobal.TEXTURA_LOGO);
        this.uiScale = UIScale.of(window.getWidth(), window.getHeight());

        // ---- Menu de pausa ----
        buttonCerrarMenu = new Button("Continuar", text);
        buttonCerrarMenu.setOnClickListener(() -> actions.resumeGame());

        buttonRestartTime = new Button("Reiniciar Tiempo", text);
        buttonRestartTime.setOnClickListener(() -> actions.restartTime());

        buttonVolverMenu = new Button("Volver al menu principal", text);
        buttonVolverMenu.setOnClickListener(() -> actions.returnToTitle());

        menuButtons[0] = buttonCerrarMenu;
        menuButtons[1] = buttonRestartTime;
        menuButtons[2] = buttonVolverMenu;

        buttonReiniciar = new Button("Reiniciar", text);
        buttonReiniciar.setOnClickListener(() -> actions.startGame());

        // El panel de pausa tiene su propio color y reparte los botones a lo
        // ancho: todos salen del mismo tamano. Es el mismo modelo que el
        // dashboard del titulo.
        pausaPanel = new Panel()
                .setBackground(0.06f, 0.09f, 0.16f, 0.90f)
                .setBorder(0.30f, 0.48f, 0.72f, 0.55f, 2f)
                .setPadding(18f, 22f, 22f, 22f)
                .setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                        .add(buttonCerrarMenu)
                        .add(buttonRestartTime)
                        .add(buttonVolverMenu)
                        .add(buttonReiniciar));

        comboResolucion = new ComboBox("Resolucion", text);
        comboResolucion.addOption("800x600", () -> actions.setResolution(800, 600));
        comboResolucion.addOption("1024x768", () -> actions.setResolution(1024, 768));
        comboResolucion.addOption("1280x720", () -> actions.setResolution(1280, 720));
        comboResolucion.addOption("1600x900", () -> actions.setResolution(1600, 900));
        comboResolucion.addOption("1920x1080", () -> actions.setResolution(1920, 1080));

        // ---- Pantalla de titulo ----
        buttonJugar = new Button("Jugar", text);
        buttonJugar.setOnClickListener(() -> actions.startGame());

        buttonSalir = new Button("Salir", text);
        buttonSalir.setOnClickListener(() -> System.exit(0));

        // Las flechas son cuadradas: ancho minimo = alto minimo.
        buttonLevelPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);
        buttonLevelPrev.setOnClickListener(() -> actions.stepLevel(-1));

        buttonLevelNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);
        buttonLevelNext.setOnClickListener(() -> actions.stepLevel(1));

        buttonDiffPrev = new Button("<", text, 20f, 16f, CELDA_FLECHA_LADO);
        buttonDiffPrev.setOnClickListener(() -> actions.stepDifficulty(-1));

        buttonDiffNext = new Button(">", text, 20f, 16f, CELDA_FLECHA_LADO);
        buttonDiffNext.setOnClickListener(() -> actions.stepDifficulty(1));

        labelLevel = new Label("Nivel: --", text);
        labelLevel.setColor(0f, 1f, 1f);
        labelDiff = new Label("Dificultad: --", text);
        labelDiff.setColor(1f, 0.86f, 0.35f);

        // Filas de selector: flecha, rotulo, flecha. La fila mide lo que pide su
        // contenido y se centra en el ancho del dashboard, para que las flechas
        // queden estrechas y el rotulo no se recorte sin necesidad.
        levelRow = new Panel()
                .setBackground(0.05f, 0.07f, 0.14f, 0.9f)
                .setBorder(0.20f, 0.30f, 0.45f, 0.6f, 1f)
                .setPadding(6f, 6f, 4f, 4f)
                .setLayout(new GridLayout(3, HUECO_ENTRE_COLUMNAS, 0f, false, false)
                        .add(buttonLevelPrev).add(labelLevel).add(buttonLevelNext));
        diffRow = new Panel()
                .setBackground(0.07f, 0.05f, 0.12f, 0.9f)
                .setBorder(0.34f, 0.26f, 0.50f, 0.6f, 1f)
                .setPadding(6f, 6f, 4f, 4f)
                .setLayout(new GridLayout(3, HUECO_ENTRE_COLUMNAS, 0f, false, false)
                        .add(buttonDiffPrev).add(labelDiff).add(buttonDiffNext));

        // El dashboard: azul muy oscuro, con borde y una sola columna que estira
        // los botones a lo ancho (grid full) para que todos midan lo mismo y
        // ocupen todo el ancho posible.
        dashboard = new Panel()
                .setBackground(0.02f, 0.03f, 0.08f, 0.94f)
                .setBorder(0.30f, 0.45f, 0.70f, 0.6f, 2f)
                .setPadding(22f, 22f, 26f, 26f)
                .setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                        .add(buttonJugar)
                        .add(levelRow)
                        .add(diffRow)
                        .add(buttonSalir));

        // El panel de pausa tambien es una columna que reparte el ancho.
        pausaPanel.setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                .add(buttonCerrarMenu)
                .add(buttonRestartTime)
                .add(buttonVolverMenu)
                .add(buttonReiniciar));

        titleBorder = new BorderLayout().setWest(dashboard, DASHBOARD_FRACCION);
    }

    /** Actualiza los rotulos de los selectores y recoloca todo. */
    void refreshLabels() {
        labelLevel.setText("Nivel: " + levels.name());
        labelDiff.setText("Dificultad: " + levels.difficultyLabel());
        layout();
    }

    /**
     * Coloca todo lo que se puede mover con las medidas virtuales actuales.
     */
    void layout() {
        int w = virtualW();
        int h = virtualH();

        // ---- Titulo: dashboard a la izquierda, logo en el centro ----------
        titleBorder.arrange(0f, 0f, w, h);
        float centroX = titleBorder.getCenterX();
        float centroW = titleBorder.getCenterAncho();
        titleCentroX = centroX + centroW / 2f;

        titleLogoW = Math.min(w * ConfigGlobal.LOGO_ANCHURA_FRACCION, centroW * 0.92f);
        titleLogoX = centroX + (centroW - titleLogoW) / 2f;
        titleLogoY = h * ConfigGlobal.LOGO_TOP_FRACCION;

        titleTextoY = titleLogoY + logo.heightForWidth(titleLogoW) + HUECO_BLOQUES;

        titlePistasTop = h - MARGEN_PISTAS
                - text.measureText(TITLE_PISTA_2).alto()
                - MENU_GAP
                - text.measureText(TITLE_PISTA_1).alto();

        // ---- Menu de pausa ------------------------------------------------
        // El panel se mide solo (padding + alto natural de sus botones) y se
        // centra reservando tambien el hueco que ocupa el desplegable abierto.
        float panelW = Math.min(620f, w * 0.62f);
        float panelH = pausaPanel.getAlto();
        float altoDesplegado = RESOLUCIONES * (34f + 2f);
        float contenido = panelH + HUECO_BLOQUES + COMBO_ALTO + HUECO_BLOQUES + altoOpciones();
        float panelY = Math.max(HUECO_BLOQUES, (h - contenido) / 2f);
        float panelX = (w - panelW) / 2f;

        pausaPanel.setBounds(panelX, panelY, panelW, panelH);
        comboResolucion.setBounds(panelX, panelY + panelH + HUECO_BLOQUES, panelW, COMBO_ALTO);

        menuBloqueTop = panelY;
        menuBloqueBottom = panelY + panelH;

        for (Button b : todosLosBotones()) {
            b.actualizarPosicion(w, h);
        }
    }

    /** Alto de las 5 opciones del desplegable, para reservarle sitio. */
    private float altoOpciones() {
        return RESOLUCIONES * (COMBO_ALTO * 0.65f);
    }

    /** Todos los botones, para pasarles la scale y el tamano de ventana. */
    private Button[] todosLosBotones() {
        Button[] todos = new Button[10];
        todos[0] = buttonCerrarMenu;
        todos[1] = buttonRestartTime;
        todos[2] = buttonVolverMenu;
        todos[3] = buttonReiniciar;
        todos[4] = buttonJugar;
        todos[5] = buttonSalir;
        todos[6] = buttonLevelPrev;
        todos[7] = buttonLevelNext;
        todos[8] = buttonDiffPrev;
        todos[9] = buttonDiffNext;
        return todos;
    }

    /**
     * Recalcula la scale de la interfaz y la reparte a los botones.
     */
    void updateUiScale(int w, int h) {
        uiScale = UIScale.of(w, h);
        for (Button b : todosLosBotones()) {
            b.setScale(uiScale);
        }
        comboResolucion.setScale(uiScale);
    }

    /** Ancho de la window en unidades de interfaz. */
    int virtualW() {
        return UIScale.virtualW(window.getWidth(), uiScale);
    }

    /** Alto de la window en unidades de interfaz. */
    int virtualH() {
        return UIScale.virtualH(window.getHeight(), uiScale);
    }

    /** Pasa el raton a los botones del menu de pausa. */
    void handlePausedMouse(long handle) {
        for (Button b : menuButtons) {
            b.handleMouseEvent(handle);
        }
        buttonReiniciar.handleMouseEvent(handle);
        comboResolucion.handleMouseEvent(handle);
    }

    /** Pasa el raton a los botones de la pantalla de titulo. */
    void handleTitleMouse(long handle) {
        buttonJugar.handleMouseEvent(handle);
        buttonSalir.handleMouseEvent(handle);
        buttonLevelPrev.handleMouseEvent(handle);
        buttonLevelNext.handleMouseEvent(handle);
        buttonDiffPrev.handleMouseEvent(handle);
        buttonDiffNext.handleMouseEvent(handle);
    }

    void renderPause(int w, int h, int currentFPS) {
        long handle = window.getWindowHandle();

        // El panel de pausa pinta su caja y sus botones.
        pausaPanel.dibujar(w, h, handle);

        String titulo = "Menu";
        String siguiente = "Siguiente nivel: " + levels.nextName();

        float y = menuBloqueTop - HUECO_BLOQUES - text.measureText(siguiente).alto();
        centeredText(w, siguiente, y, 0.86f, 0.35f, 0.1f);

        y -= HUECO_BLOQUES + text.measureText(titulo).alto();
        centeredText(w, titulo, y, 1f, 1f, 1f);

        // FPS en la esquina superior derecha.
        text.drawRightAligned("FPS: " + currentFPS, w - 20f, 20f, 0.3f, 1f, 0.3f);

        String p1 = "WASD - Moverse | Q / E - Girar camara | Mouse - Mirar | Shift - Correr";
        String p2 = "Ctrl - Agacharse | P - Menu | M - Libera raton | 8 / 9 / 0 - Efectos";

        float yPista = h - MARGEN_PISTAS - text.measureText(p2).alto()
                - MENU_GAP - text.measureText(p1).alto();
        text.drawCenteredH(p1, w, yPista, 1f, 1f, 1f);
        text.drawCenteredH(p2, w, yPista + text.measureText(p1).alto() + MENU_GAP,
                0.8f, 0.8f, 0.8f);

        // El desplegable se dibuja al final: sus opciones abiertas van por
        // encima de cualquier otra cosa que haya en la pantalla.
        comboResolucion.dibujar(w, h, handle);
    }

    void renderTitle(int w, int h) {
        long handle = window.getWindowHandle();

        // El dashboard dibuja su recuadro y, dentro, los botones y rotulos que
        // ha colocado su GridLayout.
        dashboard.dibujar(w, h, handle);

        logo.draw(titleLogoX, titleLogoY, titleLogoW, logo.heightForWidth(titleLogoW),
                w, h, 1f);

        centeredTextAt(titleCentroX, TITLE_TAGLINE, titleTextoY, 1f, 1f, 1f);

        text.drawCenteredH(TITLE_PISTA_1, w, titlePistasTop, 1f, 1f, 1f);
        text.drawCenteredH(TITLE_PISTA_2, w,
                titlePistasTop + text.measureText(TITLE_PISTA_1).alto() + MENU_GAP,
                0.8f, 0.8f, 0.8f);
    }

    /**
     * Centra un texto en una X concreta.
     */
    private void centeredTextAt(float centroX, String value, float y, float r, float g, float b) {
        text.drawBase(value, centroX - text.getTextWidth(value) / 2f, y, r, g, b);
    }

    /**
     * Centra un texto en el ancho completo de la ventana.
     */
    private void centeredText(float windowWidth, String value, float y, float r, float g, float b) {
        text.drawBase(value, (windowWidth - text.getTextWidth(value)) / 2f, y, r, g, b);
    }

    void cleanup() {
        logo.cleanup();
    }
}
