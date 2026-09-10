package UtilsRender;

/**
 * Utilidad estática para el manejo del tiempo por frame:
 * limitación de FPS y cálculo de deltaTime.
 */
public class TimeUtils {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private static long lastFrameTime = System.nanoTime();
    private static float deltaTime = 0f;
    private static boolean paused = false;
    private static long pauseStartTime = 0;

    private static int targetFPS = 0; // 0 = sin límite
    private static long frameDurationNanos = 0;

    private TimeUtils() {
    }

    public static void setFPS(int fps) {
        if (fps <= 0) {
            targetFPS = 0;
            frameDurationNanos = 0;
        } else {
            targetFPS = fps;
            frameDurationNanos = NANOS_PER_SECOND / fps;
        }
    }

    public static int getFPS() {
        return targetFPS;
    }

    // Actualizar deltaTime (llamar cada frame)
    public static void update() {
        if (paused) {
            deltaTime = 0f;
            pauseStartTime = System.nanoTime();
            return;
        }

        long now = System.nanoTime();
        long elapsed = now - lastFrameTime;

        if (targetFPS > 0 && elapsed < frameDurationNanos) {
            try {
                Thread.sleep((frameDurationNanos - elapsed) / 1_000_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            now = System.nanoTime();
            elapsed = now - lastFrameTime;
        }

        deltaTime = elapsed / (float) NANOS_PER_SECOND;
        lastFrameTime = now;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    // Pausar el tiempo (afecta delta)
    public static void pause() {
        if (!paused) {
            paused = true;
            pauseStartTime = System.nanoTime();
        }
    }

    // Reanudar el tiempo
    public static void resume() {
        if (paused) {
            paused = false;
            long now = System.nanoTime();
            lastFrameTime += (now - pauseStartTime);
        }
    }

    // Indica si está pausado
    public static boolean isPaused() {
        return paused;
    }
}