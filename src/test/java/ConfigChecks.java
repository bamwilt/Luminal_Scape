import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Comprobaciones headless del menu de configuracion.
 *
 * <p>Se leen los fuentes desde la raiz del proyecto y se comprueba que las
 * piezas del menu (volumen, brillo y contraste) siguen existiendo donde deben:
 * los ajustes en {@link Core.VideoSettings}, los volumenes en SoundManager, los
 * uniformes en los shaders, el control deslizante reutilizable, la pantalla en
 * MenuScreens, y el cableado en GameLoop/InputHandler.
 *
 * <p>Sin framework: cada comprobacion es un archivo leido como texto. Si algo
 * falta, se imprime la lista de fallos y se sale con exit 1.
 */
public final class ConfigChecks {

    private static final String GAME_STATE = "src/main/java/Core/GameState.java";
    private static final String SOUND_MANAGER = "src/main/java/Audio/SoundManager.java";
    private static final String VIDEO_SETTINGS = "src/main/java/Core/VideoSettings.java";
    private static final String WALL_SHADER = "src/main/resources/shaders/wall_fragment.glsl";
    private static final String PROP_SHADER = "src/main/resources/shaders/prop_fragment.glsl";
    private static final String SLIDER = "src/main/java/Render2D/ui/Slider.java";
    private static final String MENU_SCREENS = "src/main/java/Core/MenuScreens.java";
    private static final String GAME_LOOP = "src/main/java/Core/GameLoop.java";
    private static final String INPUT_HANDLER = "src/main/java/Core/InputHandler.java";

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        elEstadoTieneConfig();
        losVolumenesSeAjustan();
        brilloYContrasteExisten();
        losShadersLlevanLosUniformes();
        elSliderEsReutilizable();
        laPantallaDeConfiguracionExiste();
        elGameLoopCableaTodo();
        elTecladoLlegaAConfig();

        if (fallos == 0) {
            System.out.println("ConfigChecks: OK");
        } else {
            System.out.println("FALLOS: " + fallos);
            System.exit(1);
        }
    }

    private static void elEstadoTieneConfig() throws Exception {
        System.out.println("== GameState ==");
        String estado = leer(GAME_STATE);
        ok(estado.contains("CONFIG"), "GameState declara CONFIG");
        ok(estado.contains("CONFIG"), "el estado del menu de configuracion existe");
    }

    private static void losVolumenesSeAjustan() throws Exception {
        System.out.println("== SoundManager ==");
        String sonido = leer(SOUND_MANAGER);
        ok(sonido.contains("setMusicVolume"), "SoundManager tiene setMusicVolume");
        ok(sonido.contains("setSfxVolume"), "SoundManager tiene setSfxVolume");
        ok(sonido.contains("musicVolume"), "SoundManager guarda el volumen de musica");
        ok(sonido.contains("sfxVolume"), "SoundManager guarda el volumen de efectos");
    }

    private static void brilloYContrasteExisten() throws Exception {
        System.out.println("== VideoSettings ==");
        ok(existe(VIDEO_SETTINGS), "VideoSettings.java existe");
        String video = leer(VIDEO_SETTINGS);
        ok(video.contains("brillo"), "VideoSettings tiene brillo");
        ok(video.contains("contraste"), "VideoSettings tiene contraste");
    }

    private static void losShadersLlevanLosUniformes() throws Exception {
        System.out.println("== shaders ==");
        for (String shader : new String[] {WALL_SHADER, PROP_SHADER}) {
            String fuente = leer(shader);
            ok(fuente.contains("u_brillo"), shader + " declara u_brillo");
            ok(fuente.contains("u_contraste"), shader + " declara u_contraste");
        }
    }

    private static void elSliderEsReutilizable() throws Exception {
        System.out.println("== Slider ==");
        ok(existe(SLIDER), "Slider.java existe");
        String slider = leer(SLIDER);
        ok(slider.contains("setOnChange"), "Slider ofrece setOnChange");
        ok(slider.contains("handleMouseEvent"), "Slider atiende el raton");
    }

    private static void laPantallaDeConfiguracionExiste() throws Exception {
        System.out.println("== MenuScreens ==");
        String menus = leer(MENU_SCREENS);
        for (String etiqueta : new String[] {"Configuracion", "Musica", "Efectos",
                "Brillo", "Contraste", "Slider"}) {
            ok(menus.contains(etiqueta), "MenuScreens menciona " + etiqueta);
        }
    }

    private static void elGameLoopCableaTodo() throws Exception {
        System.out.println("== GameLoop ==");
        String loop = leer(GAME_LOOP);
        ok(loop.contains("openConfig"), "GameLoop sabe abrir configuracion");
        ok(loop.contains("closeConfig"), "GameLoop sabe cerrar configuracion");
        ok(loop.contains("setMusicVolume"), "GameLoop aplica el volumen de musica");
        ok(loop.contains("setBrightness"), "GameLoop aplica el brillo");
    }

    private static void elTecladoLlegaAConfig() throws Exception {
        System.out.println("== InputHandler ==");
        String input = leer(INPUT_HANDLER);
        ok(input.contains("case CONFIG"), "InputHandler tiene la rama CONFIG");
    }

    // ---- Utilidades -------------------------------------------------------

    private static boolean existe(String archivo) {
        return Files.exists(Paths.get(archivo));
    }

    private static String leer(String archivo) throws Exception {
        return Files.readString(Paths.get(archivo));
    }

    private static void ok(boolean condicion, String que) {
        System.out.println((condicion ? "  OK    " : "  FALLA ") + que);
        if (!condicion) {
            fallos++;
        }
    }

    private ConfigChecks() {
    }
}
