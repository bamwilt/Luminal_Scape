package MediaUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundLoader {
    private static final Map<String, String> SOUND_PATHS = new HashMap<>();
    
    static {
        SOUND_PATHS.put("step", "sound/step.ogg");
        SOUND_PATHS.put("ambient", "sound/ambientMusic.ogg");
    }

    public static void loadAllSounds(SoundManager soundManager) {
        for (Map.Entry<String, String> entry : SOUND_PATHS.entrySet()) {
            try {
                URL resource = SoundLoader.class.getClassLoader().getResource(entry.getValue());
                if (resource == null) {
                    System.err.println("Sound file not found: " + entry.getValue());
                    continue;
                }
                
                File soundFile = new File(resource.toURI());
                soundManager.loadSound(entry.getKey(), soundFile.getAbsolutePath());
            } catch (URISyntaxException e) {
                System.err.println("Error loading sound: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }
}