package Core;

import Render2D.components.ImageQuad;
import Render2D.components.TextRenderer;
import Render2D.components.UIScale;
import Render2D.layout.BorderLayout;
import Render2D.layout.GridLayout;
import Render2D.components.Button;
import Render2D.components.BarChanger;
import Render2D.components.TextChanger;
import Render2D.components.ComboBox;
import Render2D.components.Label;
import Render2D.components.Panel;
import Render2D.components.Slider;

/**
 * Todo lo que se dibuja en la pantalla de titulo y en el menu de pausa.
 *
 * <p>La pantalla de titulo se parte con un {@link BorderLayout}: el dashboard
 * (un {@link Panel} de un azul muy oscuro) ocupa la franja izquierda —el 34% del
 * ancho— y el logo vive en el centro. Dentro del dashboard, un {@link GridLayout}
 * de una sola columna con {@code fillX} estira los botones a lo ancho, de modo
 * que todos miden lo mismo. Ademas de Jugar y de los selectores, el dashboard
 * lleva los botones Configuracion y Salir.
 *
 * <p>El menu de pausa es otro panel con su propio color y una columna de
 * botones: Continuar, Reiniciar Tiempo, Volver al menu principal, Configuracion
 * y Reiniciar. El selector de resolucion no vive aqui: solo se cambia desde la
 * pantalla de Configuracion, que es donde estan el resto de ajustes.
 *
 * <p>La pantalla de Configuracion es un panel partido en dos columnas: a la
 * izquierda los sliders (musica, efectos, brillo, contraste, sensibilidad del
 * raton y velocidad de movimiento) y a la derecha el selector de resolucion,
 * la lista de controles y los dos botones de guardar. El boton Volver cierra la
 * pantalla desde abajo.
 *
 * <p>Los elementos de interfaz (botones, paneles, desplegable) viven en
 * {@code Render2D.components} y las maquetas en {@code Render2D.layout}.
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

        /** Abre la pantalla de Configuracion. */
        void openConfig();

        /** Cierra Configuracion y vuelve a la pantalla anterior. */
        void closeConfig();

        void setMusicVolume(float volumen);

        void setSfxVolume(float volumen);

        void setBrightness(float valor);

        void setContrast(float valor);

        /**
         * Sensibilidad del raton, como recorrido 0..1 del slider.
         *
         * <p>El recorrido se traduce a grados de yaw por pixel con
         * {@link AjustesJuego#sensibilidad(float)}: quien recibe el valor ya
         * sabe en que unidad trabaja.
         */
        void setMouseSensitivity(float recorrido);

        /** Multiplicador de velocidad del player, como recorrido 0..1. */
        void setMoveSpeed(float recorrido);

        /** Vuelve a la pantalla de inicio y resetea al primer nivel. */
        void goToTitle();

        /** Deja fijos los valores actuales de los seis ajustes. */
        void saveConfig();

        /** Vuelve los seis ajustes a su valor de fabrica y lo aplica. */
        void resetConfig();
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

    /** Alto de cada fila de slider (barra fina + nombre debajo). */
    private static final float CONFIG_SLIDER_ALTO = 44f;
    /** Hueco vertical entre filas de sliders de Configuracion. */
    private static final float CONFIG_HUECO = 16f;
    /** Relleno interior del panel padre de Configuracion. */
    private static final float CONFIG_PADDING = 24f;
    /** Hueco entre secciones de la columna izquierda. */
    private static final float CONFIG_GAP = 16f;
    /** Relleno interior de las tarjetas (controles y ajustes). */
    private static final float CONFIG_CARD_PAD = 14f;
    /** Relleno interior del panel de barras (columna derecha). */
    private static final float SLIDERS_PAD = 18f;
    /** Alto de los botones de Configuracion. */
    private static final float CONFIG_BOTON_ALTO = 48f;
    /** Hueco entre lineas de la lista de controles. */
    private static final float CONFIG_LINEA_HUECO = 10f;

    /** Rotulos de la lista de controles, en el orden en que se apilan. */
    private static final String[] CONTROLES = {
            "WASD - Moverse",
            "Mouse - Mirar",
            "Q / E - Girar camara",
            "Shift - Correr",
            "Ctrl - Agacharse",
            "P - Menu",
            "M - Libera raton",
    };

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

    // ---- Configuracion ----
    private final Button buttonConfigTitulo;
    private final Button buttonConfigPausa;
    /** Unica salida: cierra Configuracion y vuelve de donde se abrio. */
    private final Button buttonVolverConfig;
    /** Deja fijos los valores actuales de los seis ajustes. */
    private final Button buttonGuardar;
    /** Devuelve los seis ajustes a su valor de fabrica. */
    private final Button buttonRestablecer;
    /** Panel padre de la pantalla de Configuracion (BorderLayout). */
    private final Panel configPanel;
    /** Rotulo de la pantalla, en la franja superior del panel padre. */
    private final Label configTitulo;
    /** Columna derecha: las barras, en una sola columna (todas del mismo ancho). */
    private final Panel slidersPanel;
    /** Columna izquierda: controles arriba, ajustes al centro, Volver abajo. */
    private final Panel leftPanel;
    /** Subpanel central de la izquierda: Resolucion, Guardar y Restablecer. */
    private final Panel ajustesPanel;
    /** Controles: lista de atajos en la franja superior de la izquierda. */
    private final Panel controlesPanel;
    /** Desplegable de resolucion dentro del panel de Configuracion. */
    private final ComboBox comboResolucionConfig;
    private final Slider sliderMusica;
    private final Slider sliderEfectos;
    private final Slider sliderBrillo;
    private final Slider sliderContraste;
    private final Slider sliderSensibilidad;
    private final Slider sliderVelocidad;
    /** X e Y del encabezado de la lista de controles. */
    private float controlesX;
    private float controlesEncabezadoY;
    /** Tope (Y) de la primera linea de la lista de controles. */
    private float controlesTop;

    // ---- Pantalla de titulo ----
    private final Button buttonJugar;
    private final Button buttonSalir;

    // Changers para selectores (reemplazan labelLevel/labelDiff + levelRow/diffRow)
    private final TextChanger levelChanger;
    private final TextChanger diffChanger;

    // ---- Dashboard del titulo ----
    private final BorderLayout titleBorder;
    private final Panel dashboard;

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

    /** Píxeles de window por unidad de interfaz. */
    private float uiScale;

    MenuScreens(GameWindow window, TextRenderer text, LevelSession levels, Actions actions) {
        this.window = window;
        this.text = text;
        this.levels = levels;
        this.actions = actions;
        this.logo = new ImageQuad(ConfigGlobal.TEXTURA_LOGO);
        this.uiScale = Render2D.components.UIScale.of(window.getWidth(), window.getHeight());

        // ---- Menu de pausa ----
        buttonCerrarMenu = new Button("Continuar", text);
        buttonCerrarMenu.setColorInt(34, 139, 34, 255);
        buttonCerrarMenu.setOnClickListener(() -> actions.resumeGame());

        buttonRestartTime = new Button("Reiniciar Tiempo", text);
        buttonRestartTime.setColorInt(255, 140, 0, 255);
        buttonRestartTime.setOnClickListener(() -> actions.restartTime());

        buttonVolverMenu = new Button("Volver al menu principal", text);
        buttonVolverMenu.setColorInt(70, 130, 180, 255);
        buttonVolverMenu.setOnClickListener(() -> actions.returnToTitle());

        menuButtons[0] = buttonCerrarMenu;
        menuButtons[1] = buttonRestartTime;
        menuButtons[2] = buttonVolverMenu;

        buttonReiniciar = new Button("Reiniciar", text);
        buttonReiniciar.setColorInt(34, 139, 34, 255);
        buttonReiniciar.setOnClickListener(() -> actions.startGame());

        // El panel de pausa tiene su propio color y reparte los botones a lo
        // ancho: todos salen del mismo tamano. Es el mismo modelo que el
        // dashboard del titulo.
        pausaPanel = new Panel()
                .setBackground(0.06f, 0.09f, 0.16f, 1.00f)
                .setBorder(0.30f, 0.48f, 0.72f, 1.0f, 2f)
                .setPadding(18f, 22f, 22f, 22f)
                .setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                        .add(buttonCerrarMenu)
                        .add(buttonRestartTime)
                        .add(buttonVolverMenu)
                        .add(buttonReiniciar));

        buttonConfigTitulo = new Button("Configuracion", text);
        buttonConfigTitulo.setColorInt(70, 130, 180, 255);
        buttonConfigTitulo.setOnClickListener(() -> actions.openConfig());

        buttonConfigPausa = new Button("Configuracion", text);
        buttonConfigPausa.setColorInt(70, 130, 180, 255);
        buttonConfigPausa.setOnClickListener(() -> actions.openConfig());

        // Salida unica: los tres botones de antes (Salir al menu / Pantalla de
        // inicio / Volver) hacian lo mismo de dos en dos, asi que queda uno.
        buttonVolverConfig = new Button("Volver", text);
        buttonVolverConfig.setColorInt(139, 34, 34, 255); // ROJO: unica salida
        buttonVolverConfig.setOnClickListener(() -> actions.closeConfig());

        buttonGuardar = new Button("Guardar", text);
        buttonGuardar.setColorInt(70, 130, 180, 255); // AZUL: accion estandar
        buttonGuardar.setOnClickListener(() -> actions.saveConfig());

        buttonRestablecer = new Button("Restablecer", text);
        buttonRestablecer.setColorInt(218, 165, 32, 255); // AMBAR: accion media
        buttonRestablecer.setOnClickListener(() -> actions.resetConfig());

        // El panel de Configuracion tiene su propio desplegable de resolucion:
        // las mismas cinco opciones y las mismas acciones que el menu de pausa.
        comboResolucionConfig = comboResolucion();

        // Los sliders se construyen con su valor de partida ANTES de enganchar
        // el onChange, para no disparar acciones del juego al montar la pantalla.
        // El valor de partida coincide con el de fabrica de AjustesJuego, para
        // que arrancar y "Restablecer" dejen la misma barra.
        sliderMusica = new Slider("Musica", text)
                .setFillColor(0.30f, 0.70f, 1f, 1f)
                .setPaso(0.05f)
                .setValor(1f);
        sliderMusica.setOnChange(actions::setMusicVolume);

        sliderEfectos = new Slider("Efectos", text)
                .setFillColor(0.35f, 0.90f, 0.45f, 1f)
                .setPaso(0.05f)
                .setValor(1f);
        sliderEfectos.setOnChange(actions::setSfxVolume);

        sliderBrillo = new Slider("Brillo", text)
                .setFillColor(1f, 0.90f, 0.35f, 1f)
                .setPaso(0.05f)
                .setValor(0.5f);
        sliderBrillo.setOnChange(actions::setBrightness);

        sliderContraste = new Slider("Contraste", text)
                .setFillColor(1f, 0.55f, 0.30f, 1f)
                .setPaso(0.05f)
                .setValor(0.5f);
        sliderContraste.setOnChange(actions::setContrast);

        // Los dos ajustes de control: el slider mueve su recorrido 0..1 y la
        // traduccion a la unidad real vive en AjustesJuego, compartida con
        // GameLoop, que es quien la aplica. Los valores por defecto se fijan
        // antes del onChange, como en los otros cuatro.
        sliderSensibilidad = new Slider("Sensibilidad", text)
                .setFillColor(0.62f, 0.85f, 1f, 1f)
                .setPaso(0.05f)
                .setValor(AjustesJuego.recorridoSensibilidad(AjustesJuego.SENSIBILIDAD_DEFECTO));
        sliderSensibilidad.setOnChange(actions::setMouseSensitivity);

        sliderVelocidad = new Slider("Velocidad", text)
                .setFillColor(0.70f, 1f, 0.80f, 1f)
                .setPaso(0.05f)
                .setValor(AjustesJuego.recorridoVelocidad(AjustesJuego.VELOCIDAD_DEFECTO));
        sliderVelocidad.setOnChange(actions::setMoveSpeed);

        // --- Tarjetas de la pantalla de Configuracion ---
        // Controles (franja superior de la columna izquierda).
        controlesPanel = new Panel()
                .setBackground(0.08f, 0.11f, 0.19f, 0.96f)
                .setBorder(0.26f, 0.42f, 0.66f, 0.9f, 1.5f)
                .setCornerRadius(8f)
                .setPadding(CONFIG_CARD_PAD);

        // Ajustes (centro de la izquierda): Resolucion + Guardar + Restablecer,
        // en una sola columna para que todos midan lo mismo.
        ajustesPanel = new Panel()
                .setBackground(0.08f, 0.11f, 0.19f, 0.96f)
                .setBorder(0.26f, 0.42f, 0.66f, 0.9f, 1.5f)
                .setCornerRadius(8f)
                .setPadding(CONFIG_CARD_PAD)
                .setLayout(new GridLayout(1, 0f, 12f, true, true)
                        .add(comboResolucionConfig)
                        .add(buttonGuardar)
                        .add(buttonRestablecer));

        // Columna izquierda: controles (arriba), ajustes (centro) y Volver
        // (abajo). Transparente, con un hueco a la derecha que la separa de las
        // barras. El hueco entre secciones sale de los dos huecos de CONFIG_GAP.
        leftPanel = new Panel()
                .setBackground(0f, 0f, 0f, 0f)
                .setPadding(0f, CONFIG_GAP, 0f, 0f)
                .setLayout(new BorderLayout()
                        .setNorth(controlesPanel, 0f)
                        .setCenter(ajustesPanel)
                        .setSouth(buttonVolverConfig, 0f));

        // Columna derecha: un panel para las barras. Una sola columna con fillX
        // hace que todas midan lo mismo; el nombre va debajo de cada barra.
        slidersPanel = new Panel()
                .setBackground(0.08f, 0.11f, 0.19f, 0.96f)
                .setBorder(0.26f, 0.42f, 0.66f, 0.9f, 1.5f)
                .setCornerRadius(8f)
                .setPadding(SLIDERS_PAD, SLIDERS_PAD, SLIDERS_PAD, SLIDERS_PAD)
                .setLayout(new GridLayout(1, 0f, CONFIG_HUECO, true, true)
                        .add(sliderMusica).add(sliderEfectos).add(sliderBrillo)
                        .add(sliderContraste).add(sliderSensibilidad).add(sliderVelocidad));

        // Rotulo de la pantalla.
        configTitulo = new Label("Configuracion", text).setAlign(Label.Align.CENTER);
        configTitulo.setColor(1f, 1f, 1f);

        // Panel padre con BorderLayout: el titulo arriba, las barras a la
        // derecha (EAST) y la columna izquierda (CENTER) con sus tres secciones.
        configPanel = new Panel()
                .setBackground(0.04f, 0.05f, 0.08f, 0.98f)
                .setBorder(0.35f, 0.50f, 0.75f, 1.0f, 2f)
                .setCornerRadius(12f)
                .setPadding(CONFIG_PADDING)
                .setLayout(new BorderLayout()
                        .setNorth(configTitulo, 0f)
                        .setEast(slidersPanel, 0f)
                        .setCenter(leftPanel));

        // ---- Pantalla de titulo ----
        buttonJugar = new Button("Jugar", text);
        buttonJugar.setColorInt(34, 139, 34, 255);
        buttonJugar.setOnClickListener(() -> actions.startGame());

        buttonSalir = new Button("Salir", text);
        buttonSalir.setColorInt(139, 34, 34, 255);
        buttonSalir.setOnClickListener(() -> System.exit(0));

        // TextChanger para Nivel: botones < > con label central que ocupa el resto del ancho
        levelChanger = TextChanger.builder(text, "<", ">", CELDA_FLECHA_LADO)
                .label("Nivel: --")
                .labelColor(0f, 1f, 1f)
                .onLeft(() -> actions.stepLevel(-1))
                .onRight(() -> actions.stepLevel(1))
                .build();

        // TextChanger para Dificultad: botones < > con label central
        diffChanger = TextChanger.builder(text, "<", ">", CELDA_FLECHA_LADO)
                .label("Dificultad: --")
                .labelColor(1f, 0.86f, 0.35f)
                .onLeft(() -> actions.stepDifficulty(-1))
                .onRight(() -> actions.stepDifficulty(1))
                .build();

        // El dashboard: azul muy oscuro y una sola columna que estira los
        // botones a lo ancho (grid full) para que todos midan lo mismo y
        // ocupen TODO el ancho (sin padding horizontal).
        dashboard = new Panel()
                .setBackground(0.00f, 0.01f, 0.05f, 0.85f)
                .setPadding(1f, 1f, 1f, 1f)  // Mínimo padding (1px) en todos los lados
                .setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                        .add(buttonJugar)
                        .add(levelChanger)
                        .add(diffChanger)
                        .add(buttonConfigTitulo)
                        .add(buttonSalir));

        // El panel de pausa tambien es una columna que reparte el ancho.
        pausaPanel.setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                .add(buttonCerrarMenu)
                .add(buttonRestartTime)
                .add(buttonVolverMenu)
                .add(buttonConfigPausa)
                .add(buttonReiniciar));

        titleBorder = new BorderLayout().setWest(dashboard, DASHBOARD_FRACCION);
    }

    /** Actualiza los rotulos de los selectores y recoloca todo. */
    void refreshLabels() {
        levelChanger.setLabelText("Nivel: " + levels.name());
        diffChanger.setLabelText("Dificultad: " + levels.difficultyLabel());
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

        // ---- Menu de pausa ------------------------------------------------
        // El panel se mide solo (padding + alto natural de sus botones) y se
        // centra. El desplegable de resolucion ya no vive aqui, asi que no hay
        // que reservarle sitio a la lista de opciones.
        float panelW = Math.min(620f, w * 0.62f);
        float panelH = pausaPanel.getAlto();
        float panelY = Math.max(HUECO_BLOQUES, (h - panelH) / 2f);
        float panelX = (w - panelW) / 2f;

        pausaPanel.setBounds(panelX, panelY, panelW, panelH);

        menuBloqueTop = panelY;
        menuBloqueBottom = panelY + panelH;

        for (Button b : todosLosBotones()) {
            b.actualizarPosicion(w, h);
        }

        // ---- Configuracion ------------------------------------------------
        // Panel padre con BorderLayout: titulo arriba, barras a la derecha
        // (EAST) y columna izquierda (CENTER). Dentro de la izquierda, otro
        // BorderLayout reparte controles (arriba), ajustes (centro) y Volver
        // (abajo). Las barras van en un GridLayout de una sola columna con
        // fillX: todas miden lo mismo.
        float cfgW = Math.min(860f, w * 0.84f);
        float tituloAlto = text.measureText("Configuracion").alto();
        float controlesH = altoControles();
        float ajustesH = 2f * CONFIG_CARD_PAD + 3f * CONFIG_BOTON_ALTO + 2f * 12f;
        float volverH = CONFIG_BOTON_ALTO;
        float slidersH = 2f * SLIDERS_PAD + 6f * CONFIG_SLIDER_ALTO + 5f * CONFIG_HUECO;
        float columnaIzqH = controlesH + ajustesH + volverH + 2f * CONFIG_GAP;
        float contenidoH = Math.max(slidersH, columnaIzqH);
        float innerH = tituloAlto + CONFIG_GAP + contenidoH;
        float altoPanel = innerH + CONFIG_PADDING * 2f;
        float cfgX = (w - cfgW) / 2f;
        float cfgY = Math.max(HUECO_BLOQUES, (h - altoPanel) / 2f);

        // Fracciones del BorderLayout padre y del de la columna izquierda. El
        // centro de la izquierda se queda con el resto (ajustes + los huecos).
        configPanel.setLayout(new BorderLayout()
                .setNorth(configTitulo, tituloAlto / innerH)
                .setEast(slidersPanel, 0.40f)
                .setCenter(leftPanel));
        leftPanel.setLayout(new BorderLayout()
                .setNorth(controlesPanel, controlesH / contenidoH)
                .setCenter(ajustesPanel)
                .setSouth(buttonVolverConfig, volverH / contenidoH));

        configPanel.setBounds(cfgX, cfgY, cfgW, altoPanel);

        // Tras repartir, la tarjeta de controles ya tiene su rectangulo: de ahi
        // salen las posiciones del texto de la lista de atajos.
        controlesX = controlesPanel.getX() + CONFIG_CARD_PAD;
        controlesEncabezadoY = controlesPanel.getY() + CONFIG_CARD_PAD;
        controlesTop = controlesEncabezadoY
                + text.measureText("Controles").alto() + CONFIG_LINEA_HUECO;

        // Posiciones de ventana para el raton de los sliders.
        for (Slider s : configSliders()) {
            s.actualizarPosicion(w, h);
        }
    }

    /** Alto de la tarjeta de controles (encabezado + lista + relleno). */
    private float altoControles() {
        float alto = 2f * CONFIG_CARD_PAD;
        alto += text.measureText("Controles").alto() + CONFIG_LINEA_HUECO;
        for (String linea : CONTROLES) {
            alto += text.measureText(linea).alto() + CONFIG_LINEA_HUECO;
        }
        return alto;
    }

    /**
     * Desplegable de resolucion con las cinco opciones del juego.
     *
     * <p>Lo usa solo la pantalla de Configuracion: el menu de pausa no lleva
     * resolucion. El boton enseña la opcion elegida, asi que compartirlo con
     * otra pantalla haria que una contara la eleccion de la otra.
     */
    private ComboBox comboResolucion() {
        ComboBox combo = new ComboBox("Resolucion", text);
        combo.addOption("800x600", () -> actions.setResolution(800, 600));
        combo.addOption("1024x768", () -> actions.setResolution(1024, 768));
        combo.addOption("1280x720", () -> actions.setResolution(1280, 720));
        combo.addOption("1600x900", () -> actions.setResolution(1600, 900));
        combo.addOption("1920x1080", () -> actions.setResolution(1920, 1080));
        return combo;
    }

    /** Todos los botones, para pasarles la scale y el tamano de ventana. */
    private Button[] todosLosBotones() {
        Button[] todos = new Button[15];
        todos[0] = buttonCerrarMenu;
        todos[1] = buttonRestartTime;
        todos[2] = buttonVolverMenu;
        todos[3] = buttonReiniciar;
        todos[4] = buttonJugar;
        todos[5] = buttonSalir;
        todos[6] = levelChanger.getLeftButton();
        todos[7] = levelChanger.getRightButton();
        todos[8] = diffChanger.getLeftButton();
        todos[9] = diffChanger.getRightButton();
        todos[10] = buttonConfigTitulo;
        todos[11] = buttonConfigPausa;
        todos[12] = buttonVolverConfig;
        todos[13] = buttonGuardar;
        todos[14] = buttonRestablecer;
        return todos;
    }

    /** Los sliders de Configuracion, para iterar sobre ellos por igual. */
    private Slider[] configSliders() {
        return new Slider[] { sliderMusica, sliderEfectos, sliderBrillo, sliderContraste,
                sliderSensibilidad, sliderVelocidad };
    }

    /** Valor actual del slider de musica. */
    float getSliderMusica() {
        return sliderMusica.getValue();
    }

    /** Valor actual del slider de efectos. */
    float getSliderEfectos() {
        return sliderEfectos.getValue();
    }

    /** Valor actual del slider de brillo. */
    float getSliderBrillo() {
        return sliderBrillo.getValue();
    }

    /** Valor actual del slider de contraste. */
    float getSliderContraste() {
        return sliderContraste.getValue();
    }

    /** Valor actual del slider de sensibilidad del raton. */
    float getSliderSensibilidad() {
        return sliderSensibilidad.getValue();
    }

    /** Valor actual del slider de velocidad de movimiento. */
    float getSliderVelocidad() {
        return sliderVelocidad.getValue();
    }

    /**
     * Recalcula la scale de la interfaz y la reparte a los botones.
     */
    void updateUiScale(int w, int h) {
        uiScale = Render2D.components.UIScale.of(w, h);
        for (Button b : todosLosBotones()) {
            b.setScale(uiScale);
        }
        comboResolucionConfig.setScale(uiScale);
        for (Slider s : configSliders()) {
            s.setScale(uiScale);
        }
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
        buttonConfigPausa.handleMouseEvent(handle);
    }

    /** Pasa el raton a los botones de la pantalla de titulo. */
    void handleTitleMouse(long handle) {
        buttonJugar.handleMouseEvent(handle);
        buttonSalir.handleMouseEvent(handle);
        levelChanger.handleMouseEvent(handle);
        diffChanger.handleMouseEvent(handle);
        buttonConfigTitulo.handleMouseEvent(handle);
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
    }

    void renderTitle(int w, int h) {
        long handle = window.getWindowHandle();

        // El dashboard dibuja su recuadro y, dentro, los botones y rotulos que
        // ha colocado su GridLayout.
        dashboard.dibujar(w, h, handle);

        logo.draw(titleLogoX, titleLogoY, titleLogoW, logo.heightForWidth(titleLogoW),
                w, h, 1f);

        centeredTextAt(titleCentroX, TITLE_TAGLINE, titleTextoY, 1f, 1f, 1f);
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

    /** Pasa el raton a los sliders, al desplegable y a los botones de Configuracion. */
    void handleConfigMouse(long handle) {
        for (Slider s : configSliders()) {
            s.handleMouseEvent(handle);
        }
        comboResolucionConfig.handleMouseEvent(handle);
        buttonVolverConfig.handleMouseEvent(handle);
        buttonGuardar.handleMouseEvent(handle);
        buttonRestablecer.handleMouseEvent(handle);
    }

    /**
     * Lleva los seis sliders a los valores dados sin avisar a nadie.
     *
     * <p>Es lo que tiene que llamar quien acabe de aplicar esos valores (al
     * guardar o al restablecer): el ajuste ya esta hecho, asi que solo falta
     * que la barra se ponga donde toca. Como {@link #renderConfig} repinta los
     * sliders en cada frame, el jugador ve el cambio sin mas.
     */
    void sincronizarSliders(float musica, float efectos, float brillo, float contraste,
                            float sensibilidad, float velocidad) {
        sliderMusica.setValorSilencioso(musica);
        sliderEfectos.setValorSilencioso(efectos);
        sliderBrillo.setValorSilencioso(brillo);
        sliderContraste.setValorSilencioso(contraste);
        sliderSensibilidad.setValorSilencioso(sensibilidad);
        sliderVelocidad.setValorSilencioso(velocidad);
    }

    void renderConfig(int w, int h) {
        long handle = window.getWindowHandle();

        // El panel padre pinta su caja, su titulo y las dos columnas; dentro,
        // cada tarjeta pinta su fondo y sus hijos (sliders, resolucion, botones).
        configPanel.dibujar(w, h, handle);

        // Lista de controles dentro de su tarjeta (texto dibujado a mano; la
        // tarjeta ya la ha pintado el panel padre).
        text.drawTope("Controles", controlesX, controlesEncabezadoY, 1f, 1f, 1f);
        float y = controlesTop;
        for (String linea : CONTROLES) {
            text.drawTope(linea, controlesX, y, 0.88f, 0.92f, 1f);
            y += text.measureText(linea).alto() + CONFIG_LINEA_HUECO;
        }

        // Pista de teclado abajo del todo.
        text.drawCenteredH("P - Volver", w, h - MARGEN_PISTAS, 0.8f, 0.8f, 0.8f);

        // Las opciones del desplegable, si esta abierto, se pintan en la capa
        // POPUPS (renderConfigPopups), por encima de todo.
    }

    /**
     * Dibuja los popups de la pantalla de Configuracion (ComboBox opciones abiertas).
     * Se llama desde la capa POPUPS para que queden por encima de todo.
     */
    void renderConfigPopups(int w, int h, long handle) {
        if (comboResolucionConfig.estaAbierto()) {
            comboResolucionConfig.dibujarOpciones(w, h, handle);
        }
    }

    void cleanup() {
        logo.cleanup();
    }
}
