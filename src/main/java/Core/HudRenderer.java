package Core;

import ConfigGlobal.TimeConfig;

import Render2D.components.TextRenderer;
import Render2D.components.ProgressBar;

/**
 * HUD de partida: contador de llaves, tiempo y avisos ("toast").
 *
 * <p>Es lo único que se dibuja por encima del escenario mientras se juega, y por
 * eso queda aparte de {@link GameLoop}: no toca el bucle, ni la cámara, ni la
 * colisión; solo necesita el {@link TextRenderer} y, cada frame, el temporizador
 * y los contadores que le pasa el llamante.
 *
 * <p>El aviso al recoger un item vive aquí porque su estado (qué texto y desde
 * cuándo) es parte del HUD, no de la partida.
 */
final class HudRenderer {

    /** Hueco vertical entre las líneas del HUD, en unidades de interfaz. */
    private static final float LINE_GAP = 8f;

    /** Margen desde el borde superior izquierdo. */
    private static final float MARGIN = 20f;

    /** Y donde se ve el aviso al recoger un item (centro de la línea). */
    private static final float TOAST_Y_CENTRO = 90f;

    private static final float TOAST_DURATION = 2.2f;

    /** Barra de progreso de llaves bajo el contador, en unidades de interfaz. */
    private static final float BARRA_ANCHO = 220f;
    private static final float BARRA_ALTO = 14f;

    private final TextRenderer text;
    /** Barra que muestra las llaves cogidas sobre el total: 2 de 10, 20%. */
    private final ProgressBar barraLlaves = new ProgressBar();

    private String toastText;
    private float toastStartTime;

    HudRenderer(TextRenderer text) {
        this.text = text;
    }

    /** Muestra un aviso centrado abajo durante unos segundos. */
    void showToast(String message, float now) {
        toastText = message;
        toastStartTime = now;
    }

    /**
     * Dibuja el HUD de partida: llaves, tiempo, la pista de la P y el aviso.
     *
     * <p>Las tres piezas se apilan en la ESQUINA SUPERIOR IZQUIERDA, midiendo
     * cada bloque a partir del final del anterior. Antes las tres usaban
     * proporciones fijas de pantalla y se solapaban entre sí y con el margen;
     * midiendo el texto, el bloque se lee entero a cualquier resolución.
     */
    void render(int w, int h, Countdown timer, int levelSeconds, int collectedKeys, int totalKeys,
            float now) {
        float y = MARGIN;

        y = drawKeysCounter(collectedKeys, totalKeys, MARGIN, y);
        y += LINE_GAP;
        drawKeysProgress(collectedKeys, totalKeys, MARGIN, y, w, h);
        y += BARRA_ALTO + LINE_GAP;
        drawTimer(timer, levelSeconds, MARGIN, y, now);

        // Al jugar solo se anuncia la P, que es la pausa de verdad. La M es un
        // atajo propio: congela y suelta el ratón sin menú, así que no ocupa la
        // pantalla con un aviso. Si se perdía y no hacía nada, la única pista
        // está en el menú de pausa.
        String hints = "P para el menu";
        text.drawTope(hints, w - text.getTextWidth(hints) - MARGIN, MARGIN, 1f, 1f, 1f);

        drawToast(w, h, now);
    }

    /**
     * Contador de tiempo. Con tiempo de sobra se dibuja en su color normal; al
     * entrar en el umbral de aviso parpadea en ámbar, y en el crítico parpadea
     * rápido en rojo y se marca con '!'. Así se ve de un vistazo cuánto queda
     * sin tener que leer las cifras.
     */
    private void drawTimer(Countdown timer, int levelSeconds, float x, float y, float now) {
        if (timer.isFinished()) {
            text.drawTope("TIME: END", x, y, 1f, 0.1f, 0.1f);
            return;
        }

        float remaining = timer.getRemainingSeconds();
        float total = Math.max(1f, levelSeconds);
        boolean critical = remaining <= timeThreshold(TimeConfig.CRITICO_TIEMPO_FRACCION,
                TimeConfig.CRITICO_TIEMPO_MAX_SEGUNDOS, total);
        boolean warning = !critical && remaining <= timeThreshold(
                TimeConfig.AVISO_TIEMPO_FRACCION, TimeConfig.AVISO_TIEMPO_MAX_SEGUNDOS, total);

        float r = 1f;
        float g = 0.3f;
        float b = 0.3f;
        String label = "TIME: " + formatTime((int) remaining);
        float alpha = 1f;

        if (critical) {
            // Rojo, parpadeo rapido y marca de aviso.
            r = 1f;
            g = 0.1f;
            b = 0.1f;
            label = "!! " + label;
            alpha = blinkAlpha(now, TimeConfig.CRITICO_TIEMPO_PARPADEO_HZ);
        } else if (warning) {
            // Ambar, parpadeo lento.
            r = 1f;
            g = 0.72f;
            b = 0.1f;
            alpha = blinkAlpha(now, TimeConfig.AVISO_TIEMPO_PARPADEO_HZ);
        }

        text.drawTope(label, x, y, r, g, b, alpha);
    }

    /**
     * Contador de llaves, y devuelve la Y por la que puede seguir bajando el
     * siguiente bloque. Solo van las llaves: el mapa y el tiempo son ayudas y
     * no se cuentan.
     */
    private float drawKeysCounter(int collectedKeys, int totalKeys, float x, float y) {
        String label = "llaves:(" + collectedKeys + "/" + totalKeys + ")";
        // y es el tope de la tinta, y lo que se devuelve es su borde inferior, de
        // modo que la linea siguiente se apila contra lo que de verdad ocupa esta.
        text.drawTope(label, x, y, 0.3f, 1f, 0.3f);
        return y + text.measureText(label).alto();
    }

    /**
     * Barra de progreso de las llaves.
     *
     * <p>Es el mismo dato que el contador de texto, pero de un vistazo: la
     * fraccion es {@code collected / total}, o sea que con 10 llaves y 2 cogidas
     * la barra queda al 20%.
     */
    private void drawKeysProgress(int collectedKeys, int totalKeys, float x, float y,
            int windowWidth, int windowHeight) {
        barraLlaves.setValue(collectedKeys, totalKeys);
        barraLlaves.setBounds(x, y, BARRA_ANCHO, BARRA_ALTO);
        barraLlaves.dibujar(windowWidth, windowHeight, 0L);
    }

    private void drawToast(int w, int h, float now) {
        if (toastText == null) {
            return;
        }
        float t = now - toastStartTime;
        float fadeOutStart = TOAST_DURATION - 0.7f;
        if (t > TOAST_DURATION) {
            toastText = null;
            return;
        }
        float alpha = 1f;
        if (t < 0.3f) {
            alpha = t / 0.3f;
        } else if (t > fadeOutStart) {
            alpha = 1f - (t - fadeOutStart) / 0.7f;
        }
        text.drawCentered(toastText, w / 2f, h - TOAST_Y_CENTRO, 1f, 0.95f, 0.6f, alpha);
    }

    /**
     * Umbral de aviso: la fracción del tiempo total del nivel, limitada al tope
     * en segundos para que en un nivel corto salte antes de tiempo. El mínimo
     * entre ambos es el que manda, pero nunca baja de 1s.
     */
    static float timeThreshold(float fraction, int maxSeconds, float total) {
        return Math.max(1f, Math.min(total * fraction, maxSeconds));
    }

    /** 1.0 en el pico del parpadeo y 0.45 en el valle, para que nunca desaparezca. */
    static float blinkAlpha(float now, float hz) {
        return 0.725f + 0.275f * (float) Math.cos(now * hz * 2.0 * Math.PI);
    }

    static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
