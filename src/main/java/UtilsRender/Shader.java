package UtilsRender;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private int programId;
    private boolean compiled = false;

    public Shader(String vertexResource, String fragmentResource) {
        try {
            String vertexSource = readShaderFromResource(vertexResource);
            String fragmentSource = readShaderFromResource(fragmentResource);

            int vertexId = compileShader(GL33.GL_VERTEX_SHADER, vertexSource);
            int fragmentId = compileShader(GL33.GL_FRAGMENT_SHADER, fragmentSource);

            programId = GL33.glCreateProgram();
            GL33.glAttachShader(programId, vertexId);
            GL33.glAttachShader(programId, fragmentId);
            GL33.glLinkProgram(programId);

            if (GL33.glGetProgrami(programId, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
                String log = GL33.glGetProgramInfoLog(programId);
                throw new RuntimeException("Shader linking failed:\n" + log);
            }

            GL33.glDeleteShader(vertexId);
            GL33.glDeleteShader(fragmentId);

            compiled = true;

        } catch (IOException | RuntimeException e) {
            cleanup();
            throw new RuntimeException("Error al compilar/enlazar shaders: " + e.getMessage(), e);
        }
    }

    private String readShaderFromResource(String resourcePath) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("Recurso no encontrado: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    private int compileShader(int type, String source) {
        int shaderId = GL33.glCreateShader(type);
        GL33.glShaderSource(shaderId, source);
        GL33.glCompileShader(shaderId);

        if (GL33.glGetShaderi(shaderId, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            String log = GL33.glGetShaderInfoLog(shaderId);
            GL33.glDeleteShader(shaderId);
            throw new RuntimeException("Error compilando shader:\n" + log);
        }

        return shaderId;
    }

    public void use() {
        if (compiled) {
            GL33.glUseProgram(programId);
        }
    }

    public void cleanup() {
        if (compiled) {
            GL33.glDeleteProgram(programId);
            compiled = false;
        }
    }

    public void setFloat(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public void setInt(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }

    public void setBool(String name, boolean value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1i(location, value ? 1 : 0);
        }
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform4f(location, x, y, z, w);
        }
    }

    public void setMat4(String name, FloatBuffer matrixBuffer) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniformMatrix4fv(location, false, matrixBuffer);
        } else {
            System.err.println("Uniform '" + name + "' no encontrado en el shader.");
        }
    }

    public void setMat4(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            setMat4(name, buffer);
        }
    }

    public void setVec3(String name, Vector3f vector) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform3f(location, vector.x, vector.y, vector.z);
        } else {
            System.err.println("Uniform '" + name + "' no encontrado en el shader.");
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        setVec3(name, new Vector3f(x, y, z));
    }

    public void setVec2(String name, float x, float y) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform2f(location, x, y);
        }
    }
}