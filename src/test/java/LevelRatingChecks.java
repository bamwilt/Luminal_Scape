import Core.LevelRating;

/**
 * Comprobaciones headless de la puntuacion de nivel ({@link LevelRating}).
 *
 * <p>Sin ventana ni OpenGL: la regla es una funcion pura. Se comprueban los
 * umbrales (70% -> 3 estrellas, 40% -> 2, resto -> 1) y el recorte a 0..1.
 */
public final class LevelRatingChecks {

    private static int fallos = 0;

    private static void ok(boolean cond, String msg) {
        System.out.println((cond ? "  OK    " : "  FALLO ") + msg);
        if (!cond) {
            fallos++;
        }
    }

    public static void main(String[] args) {
        ok(LevelRating.estrellas(1.00f) == 3, "tiempo intacto -> 3 estrellas");
        ok(LevelRating.estrellas(0.70f) == 3, "70% restante -> 3 estrellas (umbral)");
        ok(LevelRating.estrellas(0.69f) == 2, "69% restante -> 2 estrellas");
        ok(LevelRating.estrellas(0.40f) == 2, "40% restante -> 2 estrellas (umbral)");
        ok(LevelRating.estrellas(0.39f) == 1, "39% restante -> 1 estrella");
        ok(LevelRating.estrellas(0.00f) == 1, "0% restante -> 1 estrella");
        ok(LevelRating.estrellas(-5f) == 1, "negativo se recorta a 1 estrella");
        ok(LevelRating.estrellas(2f) == 3, "mas de 1 se recorta a 3 estrellas");
        ok(LevelRating.MAX_ESTRELLAS == 3, "el maximo es 3");

        if (fallos == 0) {
            System.out.println("LevelRatingChecks: OK");
        } else {
            System.out.println("LevelRatingChecks: " + fallos + " fallo(s)");
            System.exit(1);
        }
    }
}
