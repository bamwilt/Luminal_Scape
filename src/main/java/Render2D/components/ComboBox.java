package Render2D.components;

import Render2D.interaction.KeyEvent;
import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;

import java.util.ArrayList;
import java.util.List;



public class ComboBox implements UIComponent, EventListener {

    private static final int[][] PALETA = {
        {26,34,52,245},{34,44,68,247},{42,54,84,248},{50,66,100,248},{58,78,122,250}
    };

    private final TextRenderer tr;
    private final String titulo;
    private final Button principal;
    private final List<Button> opciones = new ArrayList<>();
    private final List<Runnable> acciones = new ArrayList<>();
    private boolean abierto;
    private float x,y,ancho,alto, altoOpcion=34f, hueco=2f;

    public ComboBox(String titulo, TextRenderer tr){
        this.titulo=titulo; this.tr=tr;
        principal=new Button(titulo,tr,16f,14f,180f);
        principal.setOnClickListener(()->abierto=!abierto);
    }
    public ComboBox setAltoOpcion(float h){altoOpcion=Math.max(1f,h);return this;}
    public ComboBox addOption(String label, Runnable a){
        Button b=new Button(label,tr,14f,8f,120f);
        int[] c=PALETA[opciones.size()%PALETA.length];
        b.setColorInt(c[0],c[1],c[2],c[3]);
        b.setOnClickListener(()->{abierto=false;principal.setText(titulo+": "+label);if(a!=null)a.run();});
        opciones.add(b); acciones.add(a); return this;
    }
    public void cerrar(){abierto=false;}
    public boolean estaAbierto(){return abierto;}
    public void setScale(float s){principal.setScale(s);for(Button b:opciones)b.setScale(s);}

    @Override public void setBounds(float x,float y,float w,float h){
        this.x=x; this.y=y; this.ancho=Math.max(0f,w); this.alto=Math.max(0f,h);
        principal.setBounds(x,y,this.ancho,this.alto);
        float fila=y+this.alto+hueco;
        for(Button b:opciones){b.setBounds(x,fila,this.ancho,altoOpcion);fila+=altoOpcion+hueco;}
    }

    @Override public void dibujar(int ww,int wh,long h){principal.dibujar(ww,wh,h);}
    public void dibujarOpciones(int ww,int wh,long h){
        if(!abierto)return;
        for(Button b:opciones)b.dibujar(ww,wh,h);
    }
    @Override public float getAncho(){return ancho;}
    @Override public float getAlto(){return alto;}
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    public void handleMouseEvent(long h){principal.handleMouseEvent(h);if(abierto)for(Button b:opciones)b.handleMouseEvent(h);}

    @Override public boolean onMouseEvent(MouseEvent e){
        if(principal.onMouseEvent(e)) return true;
        if(abierto){for(Button b:opciones)if(b.onMouseEvent(e))return true;}
        return false;
    }
    @Override public boolean onKeyEvent(KeyEvent e){return false;}
}
