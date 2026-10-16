package Render2D.interaction;

/**
 * Interfaz para escuchar eventos de entrada.
 */
public interface EventListener {
    default boolean onKeyEvent(KeyEvent event) { return false; }
    default boolean onMouseEvent(MouseEvent event) { return false; }
}
