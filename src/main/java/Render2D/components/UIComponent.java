package Render2D.components;

/**
 * Contrato mínimo de un elemento de interfaz.
 */
public interface UIComponent {
    void setBounds(float x, float y, float ancho, float alto);
    void dibujar(int windowWidth, int windowHeight, long windowHandle);
    float getAncho();
    float getAlto();
    float getX();
    float getY();
}
