package MediaUtil;

import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;

/**
 * Encapsula la inicialización y el ciclo de vida del audio (SFX + música).
 * Extraído de Main para respetar Single Responsibility: Main orquesta,
 * AudioSystem se encarga de todo lo relacionado con sonido.
 */
public class AudioSystem {

    private static final float STEP_COOLDOWN = 0.4f;
    private static final float STEP_TRIGGER_SPEED = 0.1f;
    private static final float STEP_VOLUME = 1.0f;
    private static final float STEP_MIN_PITCH = 0.9f;
    private static final float STEP_PITCH_RANGE = 0.2f;
    private static final float AMBIENT_VOLUME = 0.2f;
    private static final float PICKED_VOLUME = 1.8f;

    private final SoundManager soundManager;
    private float timeSinceLastStep = 0f;

    public AudioSystem() {
        soundManager = SoundManager.getInstance();
        SoundLoader.loadAllSounds(soundManager);
        soundManager.playMusic("ambient", AMBIENT_VOLUME);
    }

    public void update(float deltaTime, float movementSpeed) {
        soundManager.update();
        handleFootstepSound(deltaTime, movementSpeed);
    }

    /** Suena al recoger un item. */
    public void playPicked() {
        soundManager.playSound("picked", PICKED_VOLUME, 1.0f, false);

        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.err.println("OpenAL error al reproducir pickup: " + error);
        }
    }

    private void handleFootstepSound(float deltaTime, float speed) {
        timeSinceLastStep += deltaTime;
        if (speed > STEP_TRIGGER_SPEED && timeSinceLastStep >= STEP_COOLDOWN) {
            float pitch = STEP_MIN_PITCH + (float) Math.random() * STEP_PITCH_RANGE;
            soundManager.playSound("step", STEP_VOLUME, pitch, false);

            int error = alGetError();
            if (error != AL_NO_ERROR) {
                System.err.println("OpenAL error al reproducir paso: " + error);
            }

            timeSinceLastStep = 0f;
        }
    }

    public void cleanup() {
        soundManager.cleanup();
    }
}