package Render2D.interaction;

/**
 * Evento de teclado desacoplado de GLFW.
 */
public final class KeyEvent {

    public enum Type { PRESS, RELEASE, REPEAT }

    public final Type type;
    public final int keyCode;
    public final int scancode;
    public final int modifiers;

    public KeyEvent(Type type, int keyCode, int scancode, int modifiers) {
        this.type = type;
        this.keyCode = keyCode;
        this.scancode = scancode;
        this.modifiers = modifiers;
    }

    public boolean isLetter() { return keyCode >= 65 && keyCode <= 90; }
    public boolean isDigit() { return keyCode >= 48 && keyCode <= 57; }
    public boolean isFunctionKey() { return keyCode >= 290 && keyCode <= 301; }
    public boolean isArrowKey() { return keyCode >= 262 && keyCode <= 265; }

    @Override public String toString() {
        return "KeyEvent{" + type + ", keyCode=" + keyCode + ", mods=" + modifiers + '}';
    }
}
