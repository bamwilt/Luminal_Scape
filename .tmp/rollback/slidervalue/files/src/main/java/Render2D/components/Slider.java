package Render2D.components;

import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;
import static org.lwjgl.glfw.GLFW.*;
import Render2D.interaction.KeyEvent;

/**
 * Barra deslizante moderna.
 *
 * <p>Dibuja una barra fina y redondeada (tipo pill) y, debajo, el nombre
 * alineado a la izquierda y el porcentaje a la derecha. El {@link Panel} del
 * track y el del relleno usan esquinas redondeadas, asi que la barra es suave.
 *
 * <p>El {@code valor} interno es siempre el recorrido 0..1; {@link #getValor()}
 * lo traduce con {@code min/max/factor}.
 */
public class Slider implements UIComponent, EventListener {

    /** Grosor de la barra. */
    private static final float BAR_H = 8f;
    /** Hueco entre la barra y el nombre. */
    private static final float GAP_NOMBRE = 8f;

    private final TextRenderer tr;
    private final String label;
    private final Label labelComp;
    private final Panel track, fill;
    private float x,y,w,h;
    private float trackX, trackW;
    private float barY, textY, textH;
    private float valor=0f, min=0f, max=1f, paso=0.05f, factor=1f;
    private boolean arrastrando=false;
    private int[] fillColor={0,255,0,255};
    private int[] trackColor={46,54,72,255};
    private java.util.function.Consumer<Float> onChange;
    private int windowWidth, windowHeight;
    private float uiScale = 1f;

    public Slider(String label, TextRenderer tr){
        this.tr=tr; this.label=label;
        labelComp=new Label(label,tr).setAlign(Label.Align.LEFT);
        labelComp.setColor(0.92f,0.94f,1f);
        track=new Panel()
                .setBackground(trackColor[0]/255f,trackColor[1]/255f,trackColor[2]/255f,trackColor[3]/255f)
                .setCornerRadius(BAR_H/2f);
        fill=new Panel()
                .setBackground(fillColor[0]/255f,fillColor[1]/255f,fillColor[2]/255f,fillColor[3]/255f)
                .setCornerRadius(BAR_H/2f);
    }
    public Slider setFillColor(float r,float g,float b,float a){fillColor=new int[]{(int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255)};if(fill!=null)fill.setBackground(r,g,b,a);return this;}
    public Slider setPaso(float p){paso=p;return this;}
    public Slider setFactor(float f){factor=f;return this;}
    public Slider setValor(float v){valor=Math.max(min,Math.min(max,v));return this;}
    public Slider setOnChange(java.util.function.Consumer<Float> c){onChange=c;return this;}

    @Override public void setBounds(float x,float y,float w,float h){
        this.x=x;this.y=y;this.w=w;this.h=h;
        textH = tr.measureText(label).alto();
        float contenido = BAR_H + GAP_NOMBRE + textH;
        float top = y + Math.max(0f, (h - contenido) / 2f);
        barY = top;
        trackX = x;
        trackW = w;
        track.setBounds(trackX, barY, trackW, BAR_H);
        fill.setBounds(trackX, barY, trackW*valor, BAR_H);
        textY = barY + BAR_H + GAP_NOMBRE;
        labelComp.setBounds(x, textY, w, textH);
    }

    @Override public void dibujar(int ww,int wh,long hh){
        track.dibujar(ww,wh,hh);
        fill.dibujar(ww,wh,hh);
        labelComp.dibujar(ww,wh,hh);
        String pct = Math.round(valor * 100f) + "%";
        tr.drawRightAligned(pct, x + w, textY + textH / 2f, 0.85f, 0.88f, 0.96f);
    }

    @Override public float getAncho(){return w;}
    @Override public float getAlto(){return h;}
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    public void actualizarPosicion(int ww,int wh){this.windowWidth=ww;this.windowHeight=wh;}

    public float getValor(){return min+valor*(max-min)*factor;}
    public void setValorSilencioso(float v){valor=Math.max(0f,Math.min(1f,(v-min)/(max-min)/factor));}

    public void handleMouseEvent(long handle){
        int mx=mouseX(handle), my=mouseY(handle);
        int state=glfwGetMouseButton(handle,GLFW_MOUSE_BUTTON_LEFT);
        boolean over=mx>=x && mx<=x+w && my>=y && my<=y+h;
        if(state==GLFW_PRESS && over){arrastrando=true;}
        else if(state==GLFW_RELEASE){arrastrando=false;}
        if(arrastrando){
            float rel=Math.max(0f,Math.min(1f,(mx-trackX)/trackW));
            valor=rel;
            fill.setBounds(trackX,barY,trackW*valor,BAR_H);
            if(onChange!=null)onChange.accept(getValor());
        }
    }
    public void setScale(float s){this.uiScale=s;}

    private int mouseX(long h){double[] a=new double[1];glfwGetCursorPos(h,a,null);return(int)(a[0]/uiScale);}
    private int mouseY(long h){double[] a=new double[1];glfwGetCursorPos(h,null,a);return(int)(a[0]/uiScale);}

    @Override public boolean onMouseEvent(MouseEvent e){return false;}
    @Override public boolean onKeyEvent(KeyEvent e){return false;}
}
