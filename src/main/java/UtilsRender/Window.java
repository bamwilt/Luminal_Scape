package UtilsRender;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {

    private long windowHandle;
    private int width;
    private int height;
    private String title;

    private boolean fullscreen = false;
    private int windowedPosX, windowedPosY, windowedWidth, windowedHeight;

    private java.util.function.BiConsumer<Integer, Integer> onResizeCallback;
    private GLFWErrorCallback errorCallback;
    private GLFWFramebufferSizeCallback fbSizeCallback;
    private GLFWKeyCallback keyCallback;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        if (initialized.get()) {
            throw new IllegalStateException("Window already initialized");
        }
        
        // Configurar callback de error de GLFW
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);

        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4); // MSAA 4x
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE); // Soporte para transparencia

        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        // Guardamos posición y tamaño ventana normal
        int[] wx = new int[1], wy = new int[1];
        glfwGetWindowPos(windowHandle, wx, wy);
        windowedPosX = wx[0];
        windowedPosY = wy[0];
        windowedWidth = width;
        windowedHeight = height;

        // Configurar callbacks
        fbSizeCallback = glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
            if (onResizeCallback != null) {
                onResizeCallback.accept(w, h);
            }
        });

        keyCallback = glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Callback para errores de OpenGL
        glfwSetWindowFocusCallback(windowHandle, (window, focused) -> {
            if (focused) {
                System.out.println("Window focused");
            }
        });

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // V-Sync activado por defecto

        // Mostrar ventana
        glfwShowWindow(windowHandle);

        // Inicializar bindings de OpenGL
        GL.createCapabilities();

        // Configurar estado de OpenGL
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE); // Habilitar antialiasing
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0f, 0f, 0f, 0f); // Fondo transparente
        glViewport(0, 0, width, height);
        
        // Configurar callback de debug de OpenGL si está disponible
        if (GL.getCapabilities().GL_ARB_debug_output) {
            glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
                String msg = MemoryUtil.memUTF8(message, length);
                System.err.println("OpenGL Debug: " + msg);
            }, NULL);
            glEnable(GL_DEBUG_OUTPUT);
            glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        }

        initialized.set(true);
        System.out.println("Window initialized successfully");
    }

    public void toggleFullscreen() {
        fullscreen = !fullscreen;

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = Objects.requireNonNull(glfwGetVideoMode(monitor));

        if (fullscreen) {
            // Guardar tamaño y posición ventana actual
            int[] wx = new int[1], wy = new int[1];
            glfwGetWindowPos(windowHandle, wx, wy);
            windowedPosX = wx[0];
            windowedPosY = wy[0];

            int[] ww = new int[1], wh = new int[1];
            glfwGetWindowSize(windowHandle, ww, wh);
            windowedWidth = ww[0];
            windowedHeight = wh[0];

            // Cambiar a modo fullscreen
            glfwSetWindowMonitor(windowHandle, monitor, 0, 0, vidmode.width(), vidmode.height(), vidmode.refreshRate());

            // Actualizar viewport y llamar callback
            glViewport(0, 0, vidmode.width(), vidmode.height());
            if (onResizeCallback != null) {
                onResizeCallback.accept(vidmode.width(), vidmode.height());
            }
        } else {
            // Volver a modo ventana con tamaño guardado
            glfwSetWindowMonitor(windowHandle, NULL, windowedPosX, windowedPosY, windowedWidth, windowedHeight, 0);

            // Actualizar viewport y llamar callback
            glViewport(0, 0, windowedWidth, windowedHeight);
            if (onResizeCallback != null) {
                onResizeCallback.accept(windowedWidth, windowedHeight);
            }
        }
    }

    public void loop(Runnable renderCallback) {
        if (!initialized.get()) {
            throw new IllegalStateException("Window not initialized");
        }

        while (!glfwWindowShouldClose(windowHandle)) {
            try {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                renderCallback.run();

                glfwSwapBuffers(windowHandle);
                glfwPollEvents();
            } catch (Exception e) {
                System.err.println("Error during rendering: " + e.getMessage());
                e.printStackTrace();
                // Continuar el loop a pesar de errores en el render
            }
        }
    }

    public void cleanup() {
        if (!initialized.get()) {
            return;
        }

        // Liberar callbacks
        if (fbSizeCallback != null) {
            fbSizeCallback.free();
        }
        if (keyCallback != null) {
            keyCallback.free();
        }

        // Liberar la ventana
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // Terminar GLFW
        glfwTerminate();
        if (errorCallback != null) {
            errorCallback.free();
        }

        initialized.set(false);
        System.out.println("Window resources cleaned up");
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(windowHandle, title);
    }

    public String getTitle() {
        return title;
    }

    public void setOnResizeCallback(java.util.function.BiConsumer<Integer, Integer> callback) {
        this.onResizeCallback = callback;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setVSync(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    public double getTime() {
        return glfwGetTime();
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void centerWindow() {
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - width) / 2,
                (vidmode.height() - height) / 2
            );
        }
    }

    public void setClearColor(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
    }
}