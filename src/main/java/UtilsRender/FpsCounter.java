package UtilsRender;

/**
 * Contador de FPS basado en frames acumulados por segundo.
 * Extraído de Main para evitar lógica de UI dispersa en el orquestador.
 */
public class FpsCounter {

    private int currentFPS = 0;
    private int frames = 0;
    private float timer = 0f;

    public void update(float deltaTime) {
        frames++;
        timer += deltaTime;
        if (timer >= 1.0f) {
            currentFPS = frames;
            frames = 0;
            timer = 0f;
        }
    }

    public int getCurrentFPS() {
        return currentFPS;
    }
}