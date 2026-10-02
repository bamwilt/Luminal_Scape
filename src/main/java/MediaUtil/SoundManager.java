package MediaUtil;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class SoundManager {

    private static SoundManager instance;
    private long device;
    private long context;
    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final Map<String, Integer> musicSources = new HashMap<>();
    private final List<Integer> activeSources = new LinkedList<>();
    private static final int MAX_ACTIVE_SOURCES = 32;
    private float masterVolume = 1.0f;

    private SoundManager() {
        initOpenAL();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void initOpenAL() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDeviceName);

        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL10) {
            throw new IllegalStateException("OpenAL 1.0 not supported");
        }
    }

    // Corregido: loadSoundFromStream reemplaza loadSound con rutas de archivo.
    // stb_vorbis_decode_filename falla con rutas UTF-8 que contienen caracteres
    // especiales (ej. "á" en "Imágenes"). Este metodo lee el InputStream a un
    // ByteBuffer en memoria y usa stb_vorbis_decode_memory, evitando el problema.
    public void loadSoundFromStream(String name, java.io.InputStream inputStream) {
        try {
            byte[] data = inputStream.readAllBytes();
            ByteBuffer bufferData = ByteBuffer.allocateDirect(data.length).put(data);
            bufferData.flip();

            try (MemoryStack stack = stackPush()) {
                IntBuffer channels = stack.mallocInt(1);
                IntBuffer sampleRate = stack.mallocInt(1);
                ShortBuffer rawAudio = STBVorbis.stb_vorbis_decode_memory(bufferData, channels, sampleRate);

                if (rawAudio == null) {
                    throw new RuntimeException("Failed to decode sound from stream: " + name);
                }

                int format = channels.get(0) == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
                int buffer = alGenBuffers();
                alBufferData(buffer, format, rawAudio, sampleRate.get(0));

                soundBuffers.put(name, buffer);
                LibCStdlib.free(rawAudio);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sound from stream: " + name, e);
        }
    }

    public void playSound(String name) {
        playSound(name, 1.0f, 1.0f, false);
    }

    public void playSound(String name, float volume, float pitch, boolean loop) {
        if (!soundBuffers.containsKey(name)) {
            System.err.println("Sound not loaded: " + name);
            return;
        }

        // Evitar acumulación ilimitada de fuentes: reusar la más antigua terminada
        if (!loop && activeSources.size() >= MAX_ACTIVE_SOURCES && activeSources.size() > 0) {
            int oldest = activeSources.remove(0);
            alDeleteSources(oldest);
        }

        int buffer = soundBuffers.get(name);

        int source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
        alSourcef(source, AL_GAIN, volume * masterVolume);
        alSourcef(source, AL_PITCH, pitch);
        alSourcei(source, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);

        alSourcePlay(source);

        if (!loop) {
            activeSources.add(source);
        }
    }

    public void update() {
        Iterator<Integer> it = activeSources.iterator();
        while (it.hasNext()) {
            int src = it.next();
            int state = alGetSourcei(src, AL_SOURCE_STATE);
            if (state != AL_PLAYING) {
                alDeleteSources(src);
                it.remove();
            }
        }
    }

    public void playMusic(String name) {
        playMusic(name, 1.0f);
    }

    public void playMusic(String name, float volume) {
        if (musicSources.containsKey(name)) {
            stopMusic(name);
        }

        if (!soundBuffers.containsKey(name)) {
            System.err.println("Music not loaded: " + name);
            return;
        }

        int buffer = soundBuffers.get(name);

        // Crear fuente dedicada para música
        int source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
        alSourcef(source, AL_GAIN, volume * masterVolume);
        alSourcei(source, AL_LOOPING, AL_TRUE);

        // Guardar fuente para control posterior
        musicSources.put(name, source);

        // Reproducir
        alSourcePlay(source);
    }

    public void stopMusic(String name) {
        if (musicSources.containsKey(name)) {
            int source = musicSources.get(name);
            alSourceStop(source);
            alDeleteSources(source);
            musicSources.remove(name);
        }
    }

    public void pauseMusic(String name) {
        if (musicSources.containsKey(name)) {
            int source = musicSources.get(name);
            alSourcePause(source);
        }
    }

    public void resumeMusic(String name) {
        if (musicSources.containsKey(name)) {
            int source = musicSources.get(name);
            alSourcePlay(source);
        }
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));

        for (int source : musicSources.values()) {
            alSourcef(source, AL_GAIN, getCurrentVolume(source) * masterVolume);
        }
    }

    private float getCurrentVolume(int source) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer volume = stack.mallocFloat(1);
            alGetSourcef(source, AL_GAIN, volume);
            return volume.get(0) / masterVolume;
        }
    }

    public void cleanup() {
        for (int buffer : soundBuffers.values()) {
            alDeleteBuffers(buffer);
        }
        soundBuffers.clear();

        for (int source : musicSources.values()) {
            alSourceStop(source);
            alDeleteSources(source);
        }
        musicSources.clear();

        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
