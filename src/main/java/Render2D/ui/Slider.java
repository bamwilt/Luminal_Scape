package Render2D.ui;

import Render2D.TextRenderer;
import java.util.function.Consumer;

/**
 * Ajuste con barra y botones: {@code [-] [====barra====] [+]} y un rotulo
 * encima con el nombre y el valor.
 *
 * <p>Es el control de la pantalla de Configuracion para mover el volumen de
 * fondo, el de efectos y los modificadores de vista (brillo y contraste). El
 * valor vive en {@code 0..1}: quien lo use lo traduce a su unidad.
 *
 * <p>Reutiliza piezas que ya existen: dos {@link Button} para el menos y el mas
 * y una {@link ProgressBar} de dos paneles como barra. El rotulo se dibuja con
 * el {@link TextRenderer}.
 */
public class Slider implements UIComponent {

    /** Hueco entre el rotulo y la fila, y a los lados de la barra. */
    private static final float HUECO = 6f;
    /** Lado minimo del boton cuadrado +/-: por debajo, el glifo se recorta. */
    private static final float LADO_BOTON_MIN = 44f;

    private final TextRenderer textRenderer;
    private final String etiqueta;
    private final Button menos;
    private final Button mas;
    private final ProgressBar barra;

    private float x;
    private float y;
    private float ancho;
    private float alto;

    /** Valor actual, en {@code 0..1}. */
    private float valor;
    private float paso = 0.1f;
    /**
     * Si cada pulsacion de + / - multiplica en vez de sumar.
     *
     * <p>Los volumenes y los modificadores de vista se mueven asi: con un paso
     * fijo, el jugador tiene que pulsar veinte veces para notar un cambio en un
     * ajuste que arranca alto, y una sola para tumbarlo cuando arranca bajo. Con
     * el factor, cada pulsacion es un salto proporcional.
     */
    private boolean multiplicativo;
    /** Cuanto multiplica (o divide) cada pulsacion cuando {@link #multiplicativo}. */
    private float factor = 1f;
    private Consumer<Float> onChange;

    public Slider(String etiqueta, TextRenderer textRenderer) {
        this.etiqueta = etiqueta;
        this.textRenderer = textRenderer;
        this.menos = new Button("-", textRenderer, 4f, 4f);
        this.mas = new Button("+", textRenderer, 4f, 4f);
        this.barra = new ProgressBar();
        this.menos.setOnClickListener(() -> {
            if (multiplicativo) {
                setValor(valor / factor);
            } else {
                setValor(valor - paso);
            }
        });
        this.mas.setOnClickListener(() -> {
            if (multiplicativo) {
                setValor(valor * factor);
            } else {
                setValor(valor + paso);
            }
        });
    }

    /** Cuanto sube o baja cada pulsacion de + / - (por defecto 0.1). */
    public Slider setPaso(float paso) {
        this.paso = Math.max(0.001f, paso);
        return this;
    }

    /**
     * Paso multiplicativo: cada pulsacion de + multiplica el valor por
     * {@code factor} y la de - lo divide, acotado a 0..1.
     *
     * <p>Para los ajustes que arrancan altos (musica, efectos) o que se notan
     * poco a poco (brillo, contraste): sumando una cantidad fija, el mismo
     * numero de pulsaciones cambia mucho mas al principio que al final. Sin
     * esta llamada el slider sigue siendo aditivo.
     */
    public Slider setFactor(float factor) {
        this.factor = Math.max(1.001f, factor);
        this.multiplicativo = true;
        return this;
    }

    /** Color del relleno de la barra. */
    public Slider setFillColor(float r, float g, float b, float a) {
        barra.setFillColor(r, g, b, a);
        return this;
    }

    /** Aviso que se dispara cuando el valor cambia (lo mueven + / -). */
    public Slider setOnChange(Consumer<Float> onChange) {
        this.onChange = onChange;
        return this;
    }

    public float getValor() {
        return valor;
    }

    /**
     * Fija el valor (0..1) y avisa al listener.
     *
     * <p>Si todavia no hay listener (construccion) solo mueve la barra: asi se
     * puede fijar el valor de partida sin disparar acciones del juego.
     */
    public Slider setValor(float valor) {
        this.valor = Math.max(0f, Math.min(1f, valor));
        barra.setProgress(this.valor);
        if (onChange != null) {
            onChange.accept(this.valor);
        }
        return this;
    }

    /**
     * Mueve la barra sin avisar al listener.
     *
     * <p>Es para reponer el valor guardado cuando se abre la pantalla o se
     * restablecen los ajustes: el cambio ya esta aplicado, asi que avisar otra
     * vez solo volveria a lanzar las acciones del juego.
     */
    public Slider setValorSilencioso(float valor) {
        this.valor = Math.max(0f, Math.min(1f, valor));
        barra.setProgress(this.valor);
        return this;
    }

    public void setScale(float uiScale) {
        menos.setScale(uiScale);
        mas.setScale(uiScale);
    }

    /** Pasa el tamano de ventana a los botones (lo usan para el hit-test). */
    public void actualizarPosicion(int windowWidth, int windowHeight) {
        menos.actualizarPosicion(windowWidth, windowHeight);
        mas.actualizarPosicion(windowWidth, windowHeight);
    }

    @Override
    public void setBounds(float x, float y, float ancho, float alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;

        float altoTexto = textRenderer.measureText("Ag").alto();
        // Cuadrado y nunca por debajo del minimo tocable: con un lado menor que
        // el ancho del glifo, el Label recortaria "-"/"+" y el boton saldria
        // en blanco. La fila mide lo que el boton.
        float lado = Math.max(LADO_BOTON_MIN, alto - altoTexto - HUECO);
        float altoFila = lado;
        float filaY = y + altoTexto + HUECO;

        menos.setBounds(x, filaY, lado, altoFila);
        mas.setBounds(x + ancho - lado, filaY, lado, altoFila);

        float barraX = x + lado + HUECO;
        float barraAncho = ancho - 2f * lado - 2f * HUECO;
        barra.setBounds(barraX, filaY, Math.max(0f, barraAncho), altoFila);
    }

    @Override
    public void dibujar(int windowWidth, int windowHeight, long windowHandle) {
        textRenderer.drawTope(etiqueta + ": " + Math.round(valor * 100f) + "%", x, y, 1f, 1f, 1f);
        menos.dibujar(windowWidth, windowHeight, windowHandle);
        barra.dibujar(windowWidth, windowHeight, windowHandle);
        mas.dibujar(windowWidth, windowHeight, windowHandle);
    }

    public void handleMouseEvent(long windowHandle) {
        menos.handleMouseEvent(windowHandle);
        mas.handleMouseEvent(windowHandle);
    }

    @Override
    public float getAncho() {
        return ancho;
    }

    @Override
    public float getAlto() {
        return alto;
    }
}
