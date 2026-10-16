package Render2D.components;

import Render2D.layout.Layout;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

public class Panel implements UIComponent {

    private static SharedQuadMesh sharedMesh;

    private float x, y, ancho, alto;
    private float bgR, bgG, bgB, bgA;
    private float bordeR=1f, bordeG=1f, bordeB=1f, bordeA, borde;
    private float padIzq, padDer, padArriba, padAbajo;
    private Layout layout;
    private final List<UIComponent> hijos = new ArrayList<>();

    public Panel setBackground(float r,float g,float b,float a) { bgR=r; bgG=g; bgB=b; bgA=a; return this; }
    public Panel setBorder(float r,float g,float b,float a,float grosor) { bordeR=r; bordeG=g; bordeB=b; bordeA=a; borde=Math.max(0f,grosor); return this; }
    public Panel setPadding(float t) { return setPadding(t,t,t,t); }
    public Panel setPadding(float i,float d,float a,float ab) { padIzq=Math.max(0f,i); padDer=Math.max(0f,d); padArriba=Math.max(0f,a); padAbajo=Math.max(0f,ab); return this; }
    public Panel setLayout(Layout l) { layout=l; return this; }
    public Panel add(UIComponent h) { if(h!=null) hijos.add(h); return this; }
    public Layout getLayout() { return layout; }

    @Override public void setBounds(float x,float y,float w,float h) {
        this.x=x; this.y=y; this.ancho=w; this.alto=h;
        if(layout!=null) layout.arrange(x+padIzq, y+padArriba, Math.max(0f,w-padIzq-padDer), Math.max(0f,h-padArriba-padAbajo));
    }

    @Override public float getAncho() { return ancho>0?ancho:(layout==null?0f:layout.getAnchoTotal()+padIzq+padDer); }
    @Override public float getAlto() { return alto>0?alto:(layout==null?0f:layout.getAltoTotal()+padArriba+padAbajo); }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    @Override public void dibujar(int ww,int wh,long handle) {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        ensureMesh();
        if(bgA>0f) sharedMesh.draw(x,y,ancho,alto,ww,wh,color(bgR,bgG,bgB,bgA),0f);
        if(borde>0f&&bordeA>0f) {
            int[] bc=color(bordeR,bordeG,bordeB,bordeA);
            sharedMesh.draw(x,y,ancho,borde,ww,wh,bc,0f);
            sharedMesh.draw(x,y+alto-borde,ancho,borde,ww,wh,bc,0f);
            sharedMesh.draw(x,y,borde,alto,ww,wh,bc,0f);
            sharedMesh.draw(x+ancho-borde,y,borde,alto,ww,wh,bc,0f);
        }
        for(UIComponent h:hijos) h.dibujar(ww,wh,handle);
        if(layout!=null) for(UIComponent h:layout.components()) h.dibujar(ww,wh,handle);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    private static int[] color(float r,float g,float b,float a) {
        return new int[]{(int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255)};
    }
    private static void ensureMesh() { if(sharedMesh==null) sharedMesh=new SharedQuadMesh(); }
    public static void cleanupShared() { if(sharedMesh!=null){sharedMesh.cleanup();sharedMesh=null;} }
}
