package MediaUtil;

import java.util.HashMap;
import java.util.Map;

public class SoundLoader {
    private static final Map<String, String> SOUND_PATHS = new HashMap<>();
    
    static {
        SOUND_PATHS.put("step", "sound/step.ogg");
        SOUND_PATHS.put("ambient", "sound/ambientMusic.ogg");
    }

    // Corregido: usa getResourceAsStream + loadSoundFromStream en vez de
    // convertir la URL a File y pasar la ruta a loadSound. El metodo anterior
    // fallaba porque stb_vorbis_decode_filename no soporta rutas UTF-8 con
    // caracteres acentuados (ej. "á" en "Imágenes"). Ahora el audio se lee
    // directamente desde el classpath como un InputStream, evitando problemas
    // con la codificacion de la ruta del archivo.
    public static void loadAllSounds(SoundManager soundManager) {
        for (Map.Entry<String, String> entry : SOUND_PATHS.entrySet()) {
            try (java.io.InputStream is = SoundLoader.class.getClassLoader().getResourceAsStream(entry.getValue())) {
                if (is == null) {
                    System.err.println("Sound file not found: " + entry.getValue());
                    continue;
                }
                soundManager.loadSoundFromStream(entry.getKey(), is);
            } catch (Exception e) {
                System.err.println("Error loading sound: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }
}