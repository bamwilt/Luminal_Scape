package Core;

/**
 * Estado de alto nivel de la partida.
 *
 * <p>Vive fuera de {@link GameLoop} para que las clases que lo consultan
 * ({@link InputHandler}, {@code GameLoop}) no tengan que depender de la clase
 * entera solo por un enum.
 */
enum GameState {
    TITLE,
    PLAYING,
    PAUSED,
    CONFIG,
    VICTORY,
    END
}
