package Core;

/**
 * Puntuacion de un nivel segun el tiempo que sobro.
 *
 * <p>Se calcula la fraccion de tiempo RESTANTE respecto al limite del nivel y
 * se traduce a estrellas: mas tiempo sobrante, mejor. Los umbrales son
 * constantes para poder ajustarlos sin tocar la logica.
 *
 * <p>Sin estado y sin dependencias: se puede comprobar sin abrir OpenGL.
 */
public final class LevelRating {

    /** Fraccion de tiempo restante a partir de la cual son 3 estrellas. */
    public static final float UMBRAL_TRES = 0.70f;

    /** Fraccion de tiempo restante a partir de la cual son 2 estrellas. */
    public static final float UMBRAL_DOS = 0.40f;

    /** Maximo de estrellas de un nivel. */
    public static final int MAX_ESTRELLAS = 3;

    private LevelRating() {
    }

    /**
     * @param fraccionRestante tiempo que sobro dividido entre el limite del
     *                         nivel (0 = agotado, 1 = intacto). Se recorta a 0..1.
     * @return 1, 2 o 3 estrellas.
     */
    public static int estrellas(float fraccionRestante) {
        float f = Math.max(0f, Math.min(1f, fraccionRestante));
        if (f >= UMBRAL_TRES) {
            return 3;
        }
        if (f >= UMBRAL_DOS) {
            return 2;
        }
        return 1;
    }
}
