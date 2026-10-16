package Render2D.interaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Bus de eventos para distribuir KeyEvent y MouseEvent a listeners.
 * Último registrado = mayor prioridad (recibe primero).
 */
public final class EventBus {
    private final List<EventListener> listeners = new ArrayList<>();

    public void register(EventListener l) { listeners.add(l); }
    public void registerFirst(EventListener l) { listeners.add(0, l); }
    public void unregister(EventListener l) { listeners.remove(l); }

    public void dispatch(KeyEvent e) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i).onKeyEvent(e)) break;
        }
    }

    public void dispatch(MouseEvent e) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i).onMouseEvent(e)) break;
        }
    }

    public void clear() { listeners.clear(); }
}
