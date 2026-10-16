package Render2D.effects;

import java.util.ArrayList;
import java.util.List;

public final class EffectManager {
    private final List<Effect> effects = new ArrayList<>();

    public void add(Effect e) { effects.add(e); e.init(); }

    public void update(float dt) {
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            if (!e.update(dt)) { e.cleanup(); effects.remove(i); }
        }
    }

    public void draw(int w, int h) {
        for (Effect e : effects) if (e.isActive()) e.draw(w, h);
    }

    public void clear() { for (Effect e : effects) e.cleanup(); effects.clear(); }
    public boolean hasActiveEffects() { return !effects.isEmpty(); }
}
