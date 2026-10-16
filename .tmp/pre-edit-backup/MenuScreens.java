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
import Render2D.ui.Slider;

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
 * la lista de controles y los dos botones de salida. El boton Volver cierra la
 * pantalla desde abajo.
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

    /** Alto de cada slider de Configuracion. */
    private static final float CONFIG_SLIDER_ALTO = 76f;
    /**
     * Hueco vertical entre sliders de Configuracion.
     *
     * <p>Se bajo de 18 a 14 al anadir los dos sliders de control: con seis
     * sliders y la lista de controles, 18 dejaba el panel demasiado alto para
     * caber en 720 unidades de interfaz.
     */
    private static final float CONFIG_HUECO = 14f;
    /** Relleno interior del panel de Configuracion. */
    private static final float CONFIG_PADDING = 28f;
    /** Alto de los botones de la parte baja de Configuracion. */
    private static final float CONFIG_BOTON_ALTO = 48f;
    /** Hueco entre lineas de la lista de controles. */
    private static final float CONFIG_LINEA_HUECO = 12f;

    /** Rotulos de la lista de controles, en el orden en que se apilan. */
    private static final String[] CONTROLES = {
            "WASD - Moverse",
            "Mouse - Mirar",
            "Q / E - Girar camara",
            "Shift - Correr",
            "Ctrl - Agacharse",
            "P - Menu",
            "M - Libera raton",
            "8 / 9 / 0 - Efectos",
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
    private final Button buttonVolverConfig;
    /** Vuelve al titulo conservando el nivel elegido. */
    private final Button buttonSalirMenu;
    /** Vuelve al titulo reseteando al primer nivel. */
    private final Button buttonIrInicio;
    /** Deja fijos los valores actuales de los seis ajustes. */
    private final Button buttonGuardar;
    /** Devuelve los seis ajustes a su valor de fabrica. */
    private final Button buttonRestablecer;
    private final Panel configPanel;
    /** Desplegable de resolucion dentro del panel de Configuracion. */
    private final ComboBox comboResolucionConfig;
    private final Slider sliderMusica;
    private final Slider sliderEfectos;
    private final Slider sliderBrillo;
    private final Slider sliderContraste;
    private final Slider sliderSensibilidad;
    private final Slider sliderVelocidad;
    /** Y del rotulo de la pantalla de Configuracion. */
    private float configTituloY;
    /** X e Y del bloque de controles de la columna derecha. */
    private float controlesX;
    private float controlesEncabezadoY;
    /** Tope (Y) de la primera linea de la lista de controles. */
    private float controlesTop;

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

        buttonConfigTitulo = new Button("Configuracion", text);
        buttonConfigTitulo.setOnClickListener(() -> actions.openConfig());

        buttonConfigPausa = new Button("Configuracion", text);
        buttonConfigPausa.setOnClickListener(() -> actions.openConfig());

        buttonVolverConfig = new Button("Volver", text);
        buttonVolverConfig.setOnClickListener(() -> actions.closeConfig());

        buttonSalirMenu = new Button("Salir al menu", text);
        buttonSalirMenu.setOnClickListener(() -> actions.returnToTitle());

        // La diferencia con "Salir al menu" es solo el destino: esta vuelve al
        // primer nivel, la otra deja elegido el que el jugador tenia puesto.
        buttonIrInicio = new Button("Pantalla de inicio", text);
        buttonIrInicio.setOnClickListener(() -> actions.goToTitle());

        buttonGuardar = new Button("Guardar configuracion", text);
        buttonGuardar.setOnClickListener(() -> actions.saveConfig());

        buttonRestablecer = new Button("Restablecer por defecto", text);
        buttonRestablecer.setOnClickListener(() -> actions.resetConfig());

        // El panel de Configuracion tiene su propio desplegable de resolucion:
        // las mismas cinco opciones y las mismas acciones que el menu de pausa.
        comboResolucionConfig = comboResolucion();

        // Los sliders se construyen con su valor de partida ANTES de enganchar
        // el onChange, para no disparar acciones del juego al montar la pantalla.
        sliderMusica = new Slider("Musica", text)
                .setFillColor(0.30f, 0.70f, 1f, 0.96f)
                .setPaso(0.05f)
                .setFactor(1.5f)
                .setValor(1f);
        sliderMusica.setOnChange(actions::setMusicVolume);

        sliderEfectos = new Slider("Efectos", text)
                .setFillColor(0.35f, 0.90f, 0.45f, 0.96f)
                .setPaso(0.05f)
                .setFactor(1.5f)
                .setValor(1f);
        sliderEfectos.setOnChange(actions::setSfxVolume);

        sliderBrillo = new Slider("Brillo", text)
                .setFillColor(1f, 0.90f, 0.35f, 0.96f)
                .setPaso(0.05f)
                .setFactor(1.5f)
                .setValor(0.5f);
        sliderBrillo.setOnChange(actions::setBrightness);

        sliderContraste = new Slider("Contraste", text)
                .setFillColor(1f, 0.55f, 0.30f, 0.96f)
                .setPaso(0.05f)
                .setFactor(1.5f)
                .setValor(0.5f);
        sliderContraste.setOnChange(actions::setContrast);

        // Los dos ajustes de control: el slider mueve su recorrido 0..1 y la
        // traduccion a la unidad real vive en AjustesJuego, compartida con
        // GameLoop, que es quien la aplica. Los valores por defecto se fijan
        // antes del onChange, como en los otros cuatro.
        sliderSensibilidad = new Slider("Sensibilidad", text)
                .setFillColor(0.62f, 0.85f, 1f, 0.96f)
                .setPaso(0.05f)
                .setValor(AjustesJuego.recorridoSensibilidad(AjustesJuego.SENSIBILIDAD_DEFECTO));
        sliderSensibilidad.setOnChange(actions::setMouseSensitivity);

        sliderVelocidad = new Slider("Velocidad", text)
                .setFillColor(0.70f, 1f, 0.80f, 0.96f)
                .setPaso(0.05f)
                .setValor(AjustesJuego.recorridoVelocidad(AjustesJuego.VELOCIDAD_DEFECTO));
        sliderVelocidad.setOnChange(actions::setMoveSpeed);

        configPanel = new Panel()
                .setBackground(0.02f, 0.03f, 0.08f, 0.94f);

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
                .setBackground(0.05f, 0.07f, 0.14f, 1.0f)
                .setBorder(0.20f, 0.30f, 0.45f, 1.0f, 1f)
                .setPadding(6f, 6f, 4f, 4f)
                .setLayout(new GridLayout(3, HUECO_ENTRE_COLUMNAS, 0f, false, false)
                        .add(buttonLevelPrev).add(labelLevel).add(buttonLevelNext));
        diffRow = new Panel()
                .setBackground(0.07f, 0.05f, 0.12f, 1.0f)
                .setBorder(0.34f, 0.26f, 0.50f, 1.0f, 1f)
                .setPadding(6f, 6f, 4f, 4f)
                .setLayout(new GridLayout(3, HUECO_ENTRE_COLUMNAS, 0f, false, false)
                        .add(buttonDiffPrev).add(labelDiff).add(buttonDiffNext));

        // El dashboard: azul muy oscuro y una sola columna que estira los
        // botones a lo ancho (grid full) para que todos midan lo mismo y
        // ocupen todo el ancho posible. Fondo totalmente opaco.
        dashboard = new Panel()
                .setBackground(0.00f, 0.01f, 0.05f, 1.00f)
                .setPadding(22f, 22f, 26f, 26f)
                .setLayout(new GridLayout(1, 0f, MENU_GAP, true, false)
                        .add(buttonJugar)
                        .add(levelRow)
                        .add(diffRow)
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
        // El panel se parte en dos columnas: a la izquierda los sliders y a la
        // derecha la resolucion, los controles y los dos botones de guardar.
        // Las tres salidas van en la fila del pie, a todo el ancho del panel.
        // En una sola columna no cabia todo (seis sliders mas la lista de
        // controles pasaban de las 1000 unidades de alto), y partido asi las dos
        // columnas quedan casi de la misma altura. El alto del panel sale de la
        // columna mas alta, de modo que el reparto se recalcula solo si se
        // añade o se quita un elemento.
        float cfgW = Math.min(700f, w * 0.72f);
        float interiorW = cfgW - CONFIG_PADDING * 2f;
        float colW = (interiorW - CONFIG_HUECO) / 2f;
        float tituloAlto = text.measureText("Configuracion").alto();
        float altoColumnas = Math.max(altoColumnaSliders(), altoColumnaDerecha());
        float altoPanel = CONFIG_PADDING * 2f + tituloAlto + CONFIG_HUECO
                + altoColumnas + CONFIG_HUECO + CONFIG_BOTON_ALTO;
        float cfgX = (w - cfgW) / 2f;
        float cfgY = Math.max(HUECO_BLOQUES, (h - altoPanel) / 2f);

        configPanel.setBounds(cfgX, cfgY, cfgW, altoPanel);
        configTituloY = cfgY + CONFIG_PADDING + tituloAlto;

        float colIzqX = cfgX + CONFIG_PADDING;
        float colDerX = colIzqX + colW + CONFIG_HUECO;
        float colY = configTituloY + CONFIG_HUECO;

        // Columna izquierda: los sliders apilados, todos del mismo alto.
        Slider[] sliders = configSliders();
        for (int i = 0; i < sliders.length; i++) {
            sliders[i].setBounds(colIzqX, colY + i * (CONFIG_SLIDER_ALTO + CONFIG_HUECO),
                    colW, CONFIG_SLIDER_ALTO);
            sliders[i].actualizarPosicion(w, h);
        }

        // Columna derecha: resolucion arriba, debajo la lista de controles y,
        // pegados al pie de la columna, guardar y restablecer.
        comboResolucionConfig.setBounds(colDerX, colY, colW, COMBO_ALTO);

        controlesX = colDerX;
        controlesEncabezadoY = colY + COMBO_ALTO + CONFIG_HUECO;
        controlesTop = controlesEncabezadoY
                + text.measureText("Controles").alto() + CONFIG_HUECO;

        float pieY = colY + altoColumnas;
        buttonGuardar.setBounds(colDerX, pieY - 2f * CONFIG_BOTON_ALTO - CONFIG_HUECO,
                colW, CONFIG_BOTON_ALTO);
        buttonRestablecer.setBounds(colDerX, pieY - CONFIG_BOTON_ALTO, colW, CONFIG_BOTON_ALTO);

        // La fila del pie reune las tres salidas: cabe entera en el ancho que
        // sobraba (137 + 209 + 252 + dos huecos = 627 de 644), asi que se
        // reparten ese ancho a partes iguales sin encogerse.
        float pieAncho = (interiorW - 2f * CONFIG_HUECO) / 3f;
        buttonSalirMenu.setBounds(colIzqX, pieY + CONFIG_HUECO, pieAncho, CONFIG_BOTON_ALTO);
        buttonIrInicio.setBounds(colIzqX + pieAncho + CONFIG_HUECO, pieY + CONFIG_HUECO,
                pieAncho, CONFIG_BOTON_ALTO);
        buttonVolverConfig.setBounds(colIzqX + 2f * (pieAncho + CONFIG_HUECO), pieY + CONFIG_HUECO,
                pieAncho, CONFIG_BOTON_ALTO);
        buttonSalirMenu.actualizarPosicion(w, h);
        buttonIrInicio.actualizarPosicion(w, h);
        buttonVolverConfig.actualizarPosicion(w, h);
    }

    /** Alto de la columna de sliders de Configuracion. */
    private float altoColumnaSliders() {
        int n = configSliders().length;
        return n * CONFIG_SLIDER_ALTO + (n - 1) * CONFIG_HUECO;
    }

    /**
     * Alto de la columna derecha de Configuracion: resolucion, encabezado y
     * lineas de controles, y los dos botones del pie (guardar y restablecer).
     *
     * <p>Son los de guardar, no los de salida: esos han bajado a la fila del
     * pie, que sobraba en ancho y asi la columna se queda como estaba (470
     * frente a los 526 de los sliders) y el panel no crece ni un pixel.
     */
    private float altoColumnaDerecha() {
        float alto = COMBO_ALTO + CONFIG_HUECO;
        alto += text.measureText("Controles").alto() + CONFIG_HUECO;
        for (String linea : CONTROLES) {
            alto += text.measureText(linea).alto() + CONFIG_LINEA_HUECO;
        }
        return alto + CONFIG_HUECO + CONFIG_BOTON_ALTO + CONFIG_HUECO + CONFIG_BOTON_ALTO;
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
        Button[] todos = new Button[17];
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
        todos[10] = buttonConfigTitulo;
        todos[11] = buttonConfigPausa;
        todos[12] = buttonVolverConfig;
        todos[13] = buttonSalirMenu;
        todos[14] = buttonIrInicio;
        todos[15] = buttonGuardar;
        todos[16] = buttonRestablecer;
        return todos;
    }

    /** Los sliders de Configuracion, para iterar sobre ellos por igual. */
    private Slider[] configSliders() {
        return new Slider[] { sliderMusica, sliderEfectos, sliderBrillo, sliderContraste,
                sliderSensibilidad, sliderVelocidad };
    }

    /** Valor actual del slider de musica. */
    float getSliderMusica() {
        return sliderMusica.getValor();
    }

    /** Valor actual del slider de efectos. */
    float getSliderEfectos() {
        return sliderEfectos.getValor();
    }

    /** Valor actual del slider de brillo. */
    float getSliderBrillo() {
        return sliderBrillo.getValor();
    }

    /** Valor actual del slider de contraste. */
    float getSliderContraste() {
        return sliderContraste.getValor();
    }

    /** Valor actual del slider de sensibilidad del raton. */
    float getSliderSensibilidad() {
        return sliderSensibilidad.getValor();
    }

    /** Valor actual del slider de velocidad de movimiento. */
    float getSliderVelocidad() {
        return sliderVelocidad.getValor();
    }

    /**
     * Recalcula la scale de la interfaz y la reparte a los botones.
     */
    void updateUiScale(int w, int h) {
        uiScale = UIScale.of(w, h);
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
        buttonLevelPrev.handleMouseEvent(handle);
        buttonLevelNext.handleMouseEvent(handle);
        buttonDiffPrev.handleMouseEvent(handle);
        buttonDiffNext.handleMouseEvent(handle);
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
        buttonSalirMenu.handleMouseEvent(handle);
        buttonIrInicio.handleMouseEvent(handle);
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

        configPanel.dibujar(w, h, handle);

        centeredText(w, "Configuracion", configTituloY, 1f, 1f, 1f);

        for (Slider s : configSliders()) {
            s.dibujar(w, h, handle);
        }

        // Lista de controles: el encabezado va en el mismo estilo que el titulo
        // de la pantalla y las lineas se apilan midiendo la tinta de cada una,
        // igual que hace el Slider con su rotulo.
        text.drawTope("Controles", controlesX, controlesEncabezadoY, 1f, 1f, 1f);
        float y = controlesTop;
        for (String linea : CONTROLES) {
            text.drawTope(linea, controlesX, y, 0.88f, 0.92f, 1f);
            y += text.measureText(linea).alto() + CONFIG_LINEA_HUECO;
        }

        buttonVolverConfig.dibujar(w, h, handle);
        buttonGuardar.dibujar(w, h, handle);
        buttonRestablecer.dibujar(w, h, handle);
        buttonSalirMenu.dibujar(w, h, handle);
        buttonIrInicio.dibujar(w, h, handle);

        text.drawCenteredH("P - Volver", w,
                h - MARGEN_PISTAS, 0.8f, 0.8f, 0.8f);

        // El desplegable se dibuja al final, igual que en el menu de pausa: sus
        // opciones abiertas tienen que tapar lo que haya debajo. El principal va
        // aqui, en su sitio dentro de la pantalla; las opciones se pintan en una
        // llamada aparte al cierre del metodo, despues de todo lo demas.
        comboResolucionConfig.dibujar(w, h, handle);

        // Ultimo trazo del frame: el desplegable, si esta abierto, por encima
        // del panel, de los sliders y de los botones.
        if (comboResolucionConfig.estaAbierto()) {
            comboResolucionConfig.dibujarOpciones(w, h, handle);
        }
    }

    void cleanup() {
        logo.cleanup();
    }
}
