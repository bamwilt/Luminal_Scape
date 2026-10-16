package Render2D.interaction;

/**
 * Evento de ratón desacoplado de GLFW.
 */
public final class MouseEvent {

    public enum Type { MOVE, PRESS, RELEASE, SCROLL, ENTER, EXIT }

    public enum Button {
        LEFT(0), RIGHT(1), MIDDLE(2), BUTTON_3(3), BUTTON_4(4), BUTTON_5(5), BUTTON_6(6), BUTTON_7(7);
        public final int glfwCode;
        Button(int glfwCode) { this.glfwCode = glfwCode; }
        public static Button fromGlfw(int glfwCode) {
            for (Button b : values()) if (b.glfwCode == glfwCode) return b;
            return LEFT;
        }
    }

    public final Type type;
    public final Button button;
    public final float x, y;
    public final float scrollX, scrollY;
    public final int modifiers;
    public boolean consumed = false;

    private MouseEvent(Builder b) {
        this.type = b.type;
        this.button = b.button;
        this.x = b.x; this.y = b.y;
        this.scrollX = b.scrollX; this.scrollY = b.scrollY;
        this.modifiers = b.modifiers;
    }

    public static Builder builder(Type type) { return new Builder(type); }

    public static class Builder {
        private final Type type;
        private Button button = Button.LEFT;
        private float x, y, scrollX, scrollY;
        private int modifiers;
        Builder(Type type) { this.type = type; }
        public Builder button(Button b) { this.button = b; return this; }
        public Builder position(float x, float y) { this.x = x; this.y = y; return this; }
        public Builder scroll(float x, float y) { this.scrollX = x; this.scrollY = y; return this; }
        public Builder modifiers(int m) { this.modifiers = m; return this; }
        public MouseEvent build() { return new MouseEvent(this); }
    }

    public MouseEvent consumed() { this.consumed = true; return this; }

    @Override public String toString() {
        return "MouseEvent{" + type + ", btn=" + button + ", pos=(" + x + "," + y + "), consumed=" + consumed + '}';
    }
}
