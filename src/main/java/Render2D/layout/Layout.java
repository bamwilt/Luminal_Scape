package Render2D.layout;

import Render2D.components.UIComponent;
import java.util.List;

public interface Layout {
    void arrange(float x, float y, float ancho, float alto);
    float getAnchoTotal();
    float getAltoTotal();
    List<UIComponent> components();
}
