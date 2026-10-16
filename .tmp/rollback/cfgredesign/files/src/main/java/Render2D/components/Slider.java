package Render2D.components;

import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;
import static org.lwjgl.glfw.GLFW.*;
import Render2D.interaction.KeyEvent;

public class Slider implements UIComponent, EventListener {

    private final TextRenderer tr;
    private final String label;
    private final Label labelComp;
    private final Panel track, fill;
    private float x,y,w,h;
    private float trackX, trackW;   /* origen y ancho del track (tras el label) */
    private float valor=0f, min=0f, max=1f, paso=0.05f, factor=1f;
    private boolean arrastrando=false;
    private int[] fillColor={0,255,0,255};
    private int[] trackColor={60,60,80,255};
    private java.util.function.Consumer<Float> onChange;
    private int windowWidth, windowHeight;
    private float uiScale = 1f;

    public Slider(String label, TextRenderer tr){
        this.tr=tr; this.label=label;
        labelComp=new Label(label,tr);
        track=new Panel().setBackground(trackColor[0]/255f,trackColor[1]/255f,trackColor[2]/255f,trackColor[3]/255f);
        fill=new Panel().setBackground(fillColor[0]/255f,fillColor[1]/255f,fillColor[2]/255f,fillColor[3]/255f);
    }
    public Slider setFillColor(float r,float g,float b,float a){fillColor=new int[]{(int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255)};if(fill!=null)fill.setBackground(r,g,b,a);return this;}
    public Slider setPaso(float p){paso=p;return this;}
    public Slider setFactor(float f){factor=f;return this;}
    public Slider setValor(float v){valor=Math.max(min,Math.min(max,v));return this;}
    public Slider setOnChange(java.util.function.Consumer<Float> c){onChange=c;return this;}

    @Override public void setBounds(float x,float y,float w,float h){
        this.x=x;this.y=y;this.w=w;this.h=h;
        float lw=tr.getTextWidth(label)+10f;
        labelComp.setBounds(x,y,lw,h);
        trackX = x+lw+5f;
        trackW = w-lw-5f;
        track.setBounds(trackX,y+2f,trackW,h-4f);
        fill.setBounds(trackX,y+2f,trackW*valor,h-4f);
    }
    @Override public void dibujar(int ww,int wh,long h){
        labelComp.dibujar(ww,wh,h); track.dibujar(ww,wh,h); fill.dibujar(ww,wh,h);
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
            fill.setBounds(trackX,y+2f,trackW*valor,h-4f);
            if(onChange!=null)onChange.accept(getValor());
        }
    }
    public void setScale(float s){this.uiScale=s;}

    private int mouseX(long h){double[] a=new double[1];glfwGetCursorPos(h,a,null);return(int)(a[0]/uiScale);}
    private int mouseY(long h){double[] a=new double[1];glfwGetCursorPos(h,null,a);return(int)(a[0]/uiScale);}

    @Override public boolean onMouseEvent(MouseEvent e){return false;}
    @Override public boolean onKeyEvent(KeyEvent e){return false;}
}
