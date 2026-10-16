package Render2D.components;

import Render2D.interaction.KeyEvent;
import Render2D.interaction.MouseEvent;
import Render2D.interaction.EventListener;





/**
 * [ < ] [ LABEL CENTRAL ] [ > ] - ocupa ancho completo
 */
public class TextChanger implements UIComponent, EventListener {

    private final Button btnLeft, btnRight;
    private final Label labelCenter;
    private final float btnWidth, gap;
    private float x, y, ancho, alto;

    public TextChanger(TextRenderer tr, String leftText, String rightText, float btnWidth, float gap,
                       String initialLabel, float labelR, float labelG, float labelB,
                       Runnable onLeft, Runnable onRight) {
        this.btnWidth = btnWidth; this.gap = gap;
        btnLeft = new Button(leftText, tr, 16f, 12f, btnWidth);
        btnLeft.setColorInt(218,165,32,255);
        btnLeft.setOnClickListener(()->{if(onLeft!=null)onLeft.run();});
        btnRight = new Button(rightText, tr, 16f, 12f, btnWidth);
        btnRight.setColorInt(218,165,32,255);
        btnRight.setOnClickListener(()->{if(onRight!=null)onRight.run();});
        labelCenter = new Label(initialLabel, tr);
        labelCenter.setColor(labelR, labelG, labelB);
    }

    public static class Builder {
        private final TextRenderer tr; private final String lt, rt; private final float bw;
        private float gap=8f; private String label=""; private float lr=0f,lg=1f,lb=1f;
        private Runnable onL=()->{}, onR=()->{};
        public Builder(TextRenderer tr,String lt,String rt,float bw){this.tr=tr;this.lt=lt;this.rt=rt;this.bw=bw;}
        public Builder gap(float g){gap=g;return this;}
        public Builder label(String s){label=s;return this;}
        public Builder labelColor(float r,float g,float b){lr=r;lg=g;lb=b;return this;}
        public Builder onLeft(Runnable a){onL=a;return this;}
        public Builder onRight(Runnable a){onR=a;return this;}
        public TextChanger build(){return new TextChanger(tr,lt,rt,bw,gap,label,lr,lg,lb,onL,onR);}
    }
    public static Builder builder(TextRenderer tr,String lt,String rt,float bw){return new Builder(tr,lt,rt,bw);}

    @Override public void setBounds(float x,float y,float w,float h){
        this.x=x; this.y=y; this.ancho=w; this.alto=h;
        float cx=x+btnWidth+gap, cw=w-2f*(btnWidth+gap);
        btnLeft.setBounds(x,y,btnWidth,h);
        btnRight.setBounds(x+w-btnWidth,y,btnWidth,h);
        labelCenter.setBounds(cx,y,cw,h);
    }

    @Override public void dibujar(int ww,int wh,long h){
        btnLeft.dibujar(ww,wh,h);
        labelCenter.dibujar(ww,wh,h);
        btnRight.dibujar(ww,wh,h);
    }
    @Override public float getAncho(){return ancho>0?ancho:btnWidth*2+gap*2+labelCenter.getAncho();}
    @Override public float getAlto(){return alto>0?alto:Math.max(btnWidth,labelCenter.getAlto());}
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    public void setLabelText(String t){labelCenter.setText(t);}
    public Button getLeftButton(){return btnLeft;}
    public Button getRightButton(){return btnRight;}

    // EventListener
    @Override public boolean onMouseEvent(MouseEvent e){
        return btnLeft.onMouseEvent(e) || btnRight.onMouseEvent(e);
    }
    @Override public boolean onKeyEvent(KeyEvent e){
        // Asignable: si se le asigna una key code específica
        return false;
    }
}
