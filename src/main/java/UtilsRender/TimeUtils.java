package UtilsRender;

public class TimeUtils {

    private static long lastFrameTime = System.nanoTime();
    private static float deltaTime = 0f;
    private static boolean paused = false;
    private static long pauseStartTime = 0;

    // FPS target, 0 = sin límite
    private static int targetFPS = 0;
    private static long frameDurationNanos = 0;

    // FPS calculado (promedio rolling)
    private static float currentFPS = 0f;
    private static long fpsLastTime = System.nanoTime();
    private static int fpsFrames = 0;

    public static void setFPS(int fps) {
        if (fps <= 0) {
            targetFPS = 0;
            frameDurationNanos = 0;
        } else {
            targetFPS = fps;
            frameDurationNanos = 1_000_000_000L / fps;
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

        if (targetFPS > 0) {
            long elapsed = now - lastFrameTime;
            if (elapsed < frameDurationNanos) {
                try {
                    // Espera pasiva para limitar FPS (puede mejorarse con sleep más preciso)
                    Thread.sleep((frameDurationNanos - elapsed) / 1_000_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                now = System.nanoTime();
                elapsed = now - lastFrameTime;
            }
            deltaTime = elapsed / 1_000_000_000f;
            lastFrameTime = now;
        } else {
            deltaTime = (now - lastFrameTime) / 1_000_000_000f;
            lastFrameTime = now;
        }

        // Cálculo FPS promedio cada segundo
        fpsFrames++;
        long fpsNow = System.nanoTime();
        if (fpsNow - fpsLastTime >= 1_000_000_000L) {
            currentFPS = fpsFrames * 1_0f;
            fpsFrames = 0;
            fpsLastTime = fpsNow;
        }
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float getCurrentFPS() {
        return currentFPS;
    }

    // Convertir milisegundos a segundos
    public static float millisToSeconds(long millis) {
        return millis / 1000f;
    }

    // Convertir milisegundos a minutos
    public static float millisToMinutes(long millis) {
        return millis / (1000f * 60f);
    }

    // Obtener segundos actuales del sistema
    public static int getCurrentSeconds() {
        return java.time.LocalTime.now().getSecond();
    }

    // Obtener minutos actuales del sistema
    public static int getCurrentMinutes() {
        return java.time.LocalTime.now().getMinute();
    }

    // Obtener milisegundos actuales del sistema
    public static int getCurrentMilliseconds() {
        return java.time.LocalTime.now().getNano() / 1_000_000;
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

    // Contador regresivo simple
    public static class Countdown {

        private long durationMillis;
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
}
