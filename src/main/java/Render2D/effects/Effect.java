package Render2D.effects;

/**
 * Interfaz base para efectos visuales en la capa EFFECTS.
 */
public interface Effect {
    void init();
    boolean update(float deltaTime); // false = remover
    void draw(int windowWidth, int windowHeight);
    void cleanup();
    default boolean isActive() { return true; }
}
