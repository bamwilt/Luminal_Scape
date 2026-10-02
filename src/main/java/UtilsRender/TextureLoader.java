package UtilsRender;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class TextureLoader {

    /**
     * Textura cargada con su tamaño original en píxeles. Lo necesitan los quads
     * de interfaz, que calculan la altura a partir de la anchura para no
     * deformar la imagen.
     */
    public record LoadedTexture(int id, int width, int height) {
    }

    public static int loadTexture(String resourcePath) {
        return loadTextureInfo(resourcePath).id();
    }

    public static LoadedTexture loadTextureInfo(String resourcePath) {
        // Leer el recurso desde classpath
        ByteBuffer imageBuffer;
        try {
            imageBuffer = ioResourceToByteBuffer(resourcePath, 8 * 1024);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el recurso: " + resourcePath, e);
        }

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Cargar imagen desde memoria
        ByteBuffer decodedImage = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
        if (decodedImage == null) {
            throw new RuntimeException("No se pudo cargar la textura desde memoria: " + resourcePath + "\n" + STBImage.stbi_failure_reason());
        }

        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Parámetros de textura
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Enviar a GPU
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, decodedImage);
        glGenerateMipmap(GL_TEXTURE_2D);

        STBImage.stbi_image_free(decodedImage);
        return new LoadedTexture(textureID, width.get(0), height.get(0));
    }

    // Método para cargar un archivo del classpath como ByteBuffer
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        try (InputStream source = TextureLoader.class.getClassLoader().getResourceAsStream(resource);
             ReadableByteChannel rbc = Channels.newChannel(source)) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(bufferSize);
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
            buffer.flip();
            return buffer;
        }
    }
}