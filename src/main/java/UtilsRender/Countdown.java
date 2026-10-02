package UtilsRender;

/**
 * Contador regresivo simple. Separado de TimeUtils para respetar
 * Single Responsibility: TimeUtils maneja tiempo global, Countdown
 * maneja un temporizador de juego.
 *
 * Soportea pausa/reanudación acumulada: {@link #pause()} detiene la cuenta y
 * {@link #resume()} la continúa donde quedó (no se pierde el tiempo pausado).
 */
public class Countdown {

    private long durationMillis;
    private long accumulatedMillis = 0;
    private long lastStartMillis = 0;
    private boolean running = false;

    public Countdown(float durationSeconds) {
        this.durationMillis = (long) (durationSeconds * 1000);
    }

    /** Inicia desde cero (o reinicia si ya estaba en marcha). */
    public void start() {
        accumulatedMillis = 0;
        lastStartMillis = System.currentTimeMillis();
        running = true;
    }

    public void pause() {
        if (running) {
            accumulatedMillis += System.currentTimeMillis() - lastStartMillis;
            running = false;
        }
    }

    public void resume() {
        if (!running) {
            lastStartMillis = System.currentTimeMillis();
            running = true;
        }
    }

    public void stop() {
        pause();
    }

    /** Añade tiempo extra al contador (p. ej. bonus por recoger items). */
    public void addSeconds(float seconds) {
        durationMillis += (long) (seconds * 1000);
    }

    public boolean isRunning() {
        return running;
    }

    private long elapsedMillis() {
        long elapsed = accumulatedMillis;
        if (running) {
            elapsed += System.currentTimeMillis() - lastStartMillis;
        }
        return elapsed;
    }

    public boolean isFinished() {
        return elapsedMillis() >= durationMillis;
    }

    public float getRemainingSeconds() {
        long remaining = durationMillis - elapsedMillis();
        return remaining > 0 ? remaining / 1000f : 0f;
    }

    public float getElapsedSeconds() {
        return elapsedMillis() / 1000f;
    }
}