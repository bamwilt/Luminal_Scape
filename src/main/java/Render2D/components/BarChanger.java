package Render2D.components;

import Render2D.interaction.KeyEvent;
import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;




/**
 * [ - ] [ BARRA PROGRESO ] [ + ]
 */
public class BarChanger implements UIComponent, EventListener {

    private final Button btnLeft, btnRight;
    private final Panel bgPanel, fillPanel;
    private final float btnWidth, gap, barHeight;
    private float x,y,ancho,alto, value=0f;

    public BarChanger(TextRenderer tr, String lt, String rt, float btnWidth, float gap, float barHeight,
                      int bgColor, int fillColor, Runnable onL, Runnable onR) {
        this.btnWidth=btnWidth; this.gap=gap; this.barHeight=barHeight;
        btnLeft=new Button(lt,tr,16f,12f,btnWidth); btnLeft.setColorInt(218,165,32,255); btnLeft.setOnClickListener(()->{if(onL!=null)onL.run();});
        btnRight=new Button(rt,tr,16f,12f,btnWidth); btnRight.setColorInt(218,165,32,255); btnRight.setOnClickListener(()->{if(onR!=null)onR.run();});
        bgPanel=new Panel().setBackground((bgColor>>16&0xFF)/255f,(bgColor>>8&0xFF)/255f,(bgColor&0xFF)/255f,(bgColor>>24&0xFF)/255f);
        fillPanel=new Panel().setBackground((fillColor>>16&0xFF)/255f,(fillColor>>8&0xFF)/255f,(fillColor&0xFF)/255f,(fillColor>>24&0xFF)/255f);
    }
    public static class Builder{
        private final TextRenderer tr; private final String lt,rt; private final float bw;
        private float gap=8f, bh=14f; private int bg=0x1A2638FF, fill=0x4E944AFF;
        private Runnable onL=()->{}, onR=()->{};
        public Builder(TextRenderer tr,String lt,String rt,float bw){this.tr=tr;this.lt=lt;this.rt=rt;this.bw=bw;}
        public Builder gap(float g){gap=g;return this;}
        public Builder barHeight(float h){bh=h;return this;}
        public Builder bgColor(int c){bg=c;return this;}
        public Builder fillColor(int c){fill=c;return this;}
        public Builder onLeft(Runnable a){onL=a;return this;}
        public Builder onRight(Runnable a){onR=a;return this;}
        public BarChanger build(){return new BarChanger(tr,lt,rt,bw,gap,bh,bg,fill,onL,onR);}
    }
    public static Builder builder(TextRenderer tr,String lt,String rt,float bw){return new Builder(tr,lt,rt,bw);}

    @Override public void setBounds(float x,float y,float w,float h){
        this.x=x; this.y=y; this.ancho=w; this.alto=h;
        float cx=x+btnWidth+gap, cw=w-2f*(btnWidth+gap), by=y+(h-barHeight)/2f;
        btnLeft.setBounds(x,y,btnWidth,h);
        btnRight.setBounds(x+w-btnWidth,y,btnWidth,h);
        bgPanel.setBounds(cx,by,cw,barHeight);
        fillPanel.setBounds(cx,by,cw*value,barHeight);
    }
    @Override public void dibujar(int ww,int wh,long h){
        btnLeft.dibujar(ww,wh,h); bgPanel.dibujar(ww,wh,h);
        if(value>0f) fillPanel.dibujar(ww,wh,h);
        btnRight.dibujar(ww,wh,h);
    }
    @Override public float getAncho(){return ancho>0?ancho:btnWidth*2+gap*2+200f;}
    @Override public float getAlto(){return alto>0?alto:Math.max(btnWidth,barHeight);}
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    public void setValue(float v){value=Math.max(0f,Math.min(1f,v));}
    public float getValue(){return value;}
    public Button getLeftButton(){return btnLeft;}
    public Button getRightButton(){return btnRight;}

    @Override public boolean onMouseEvent(MouseEvent e){
        return btnLeft.onMouseEvent(e) || btnRight.onMouseEvent(e);
    }
    @Override public boolean onKeyEvent(KeyEvent e){return false;}
}
