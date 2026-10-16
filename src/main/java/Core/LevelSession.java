package Core;

import Render3D.map.LevelData;
import Render3D.map.LevelLoader;

/**
 * Que nivel y que dificultad estan seleccionados ahora mismo.
 *
 * <p>Antes esto vivia dentro de {@link GameLoop} (un array de niveles, un indice,
 * un enum de dificultad y el nivel cargado), mezclado con el bucle de render y
 * con la carga de texturas. Aqui queda junto y con una unica responsabilidad:
 * saber que nivel y que dificultad estan seleccionados. La carga de texturas del
 * preset se queda en {@code GameLoop}, que es quien tiene el cache de OpenGL.
 *
 * <p>La clase no dibuja ni refresca rotulos: quien cambia de nivel llama despues
 * a {@code GameLoop.loadCurrentLevel()} y al refresco de la interfaz. Asi el
 * cambio de seleccion y sus efectos quedan separados.
 */
final class LevelSession {

    private LevelLoader.LevelInfo[] files;
    private int index = 0;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean testLevelUnlocked = false;
    private LevelData data;

    LevelSession() {
        files = LevelLoader.listLevels();
    }

    /** El nivel cargado ahora mismo (directiva {@code map:} y su preset). */
    LevelData data() {
        return data;
    }

    /** Relee del disco el nivel seleccionado. */
    void reload() {
        data = LevelLoader.loadLevel(file().path);
    }

    /** Titulo (directiva {@code name:}) del nivel seleccionado. */
    String name() {
        return file().name;
    }

    /** Titulo del siguiente nivel (indice +1, ciclico). */
    String nextName() {
        return files[(index + 1) % files.length].name;
    }

    /** Tiempo (directiva {@code time: mm:ss}) del nivel seleccionado, en segundos. */
    int timeSeconds() {
        return data.getTimeSeconds();
    }

    Difficulty difficulty() {
        return difficulty;
    }

    boolean isFacil() {
        return difficulty == Difficulty.FACIL;
    }

    boolean isDificil() {
        return difficulty == Difficulty.DIFICIL;
    }

    /** Nombre mostrado de la dificultad seleccionada. */
    String difficultyLabel() {
        switch (difficulty) {
            case FACIL:
                return "Facil";
            case DIFICIL:
                return "Dificil";
            default:
                return "Normal";
        }
    }

    /** Indice del nivel seleccionado, 0-based. */
    int index() {
        return index;
    }

    /** Numero de niveles de la partida. */
    int count() {
        return files.length;
    }

    /** El nivel seleccionado es el ultimo de la lista? */
    boolean isLastLevel() {
        return index >= files.length - 1;
    }

    /** Cambia el nivel seleccionado (adelante/atras). No recarga datos. */
    void stepLevel(int delta) {
        index = (index + delta % files.length + files.length) % files.length;
    }

    /** Cambia la dificultad seleccionada (circular). */
    void stepDifficulty(int delta) {
        Difficulty[] values = Difficulty.values();
        difficulty = values[(difficulty.ordinal() + delta % values.length + values.length) % values.length];
    }

    /** Vuelve al primer nivel de la partida (sin recargar). */
    void goToFirst() {
        index = 0;
    }

    /**
     * Desbloquea o vuelve a esconder el nivel de pruebas.
     *
     * <p>Relee la lista de niveles porque el nivel de pruebas no viene en
     * {@link LevelLoader#listLevels()}: vive en el mismo directorio pero se
     * filtra a proposito. Al ocultarlo de nuevo el selector se queda con los
     * niveles de la partida, y el indice se recorta para que no se quede fuera
     * de rango un nivel que ya no esta. No recarga datos: eso lo hace el
     * llamante.
     */
    void toggleTestLevel() {
        testLevelUnlocked = !testLevelUnlocked;
        int estaba = index;
        files = LevelLoader.listLevels(testLevelUnlocked);
        if (index >= files.length) {
            index = files.length - 1;
        }
        // Al entrar en el nivel de pruebas el selector se queda ahi, que es lo
        // que se quiere: se acaba de pedir expresamente. Al salir, se vuelve al
        // ultimo nivel de la partida en vez de saltar a un sitio raro.
        if (!testLevelUnlocked && estaba >= files.length) {
            index = files.length - 1;
        }
    }

    private LevelLoader.LevelInfo file() {
        return files[index % files.length];
    }
}
