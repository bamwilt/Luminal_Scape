package UtilsRender;

/**
 * Contador regresivo simple. Separado de TimeUtils para respetar
 * Single Responsibility: TimeUtils maneja tiempo global, Countdown
 * maneja un temporizador de juego.
 */
public class Countdown {

    private final long durationMillis;
    private long startTimeMillis;
    private boolean running;

    public Countdown(float durationSeconds) {
        this.durationMillis = (long) (durationSeconds * 1000);
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        startTimeMillis = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        if (!running) {
            return true;
        }
        return System.currentTimeMillis() - startTimeMillis >= durationMillis;
    }

    public float getRemainingSeconds() {
        if (!running) {
            return 0f;
        }
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        long remaining = durationMillis - elapsed;
        return remaining > 0 ? remaining / 1000f : 0f;
    }

    public float getElapsedSeconds() {
        if (!running) {
            return 0f;
        }
        return (System.currentTimeMillis() - startTimeMillis) / 1000f;
    }
}