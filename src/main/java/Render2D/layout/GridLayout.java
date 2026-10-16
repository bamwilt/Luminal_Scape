package Render2D.layout;

import Render2D.components.UIComponent;
import java.util.ArrayList;
import java.util.List;

public class GridLayout implements Layout {

    private final int columns;
    private final float gapX, gapY;
    private final boolean fillX, fillY;
    private final List<UIComponent> componentes = new ArrayList<>();
    private float[] colWidths = new float[0];
    private float[] rowHeights = new float[0];

    public GridLayout(int columns, float gapX, float gapY, boolean fillX, boolean fillY) {
        this.columns = Math.max(1, columns);
        this.gapX = Math.max(0f, gapX);
        this.gapY = Math.max(0f, gapY);
        this.fillX = fillX;
        this.fillY = fillY;
    }
    public GridLayout add(UIComponent c){if(c!=null)componentes.add(c);return this;}

    @Override public List<UIComponent> components(){return componentes;}
    public int getFilas(){return componentes.isEmpty()?0:(componentes.size()+columns-1)/columns;}

    private void medir(){
        int cols = componentes.isEmpty()?0:columns;
        colWidths = new float[cols];
        rowHeights = new float[getFilas()];
        for(int i=0;i<componentes.size();i++){
            int col=i%columns, row=i/columns;
            UIComponent c=componentes.get(i);
            colWidths[col]=Math.max(colWidths[col],c.getAncho());
            rowHeights[row]=Math.max(rowHeights[row],c.getAlto());
        }
    }
    @Override public float getAnchoTotal(){medir();float t=0;for(float w:colWidths)t+=w;return t+gapX*Math.max(0,colWidths.length-1);}
    @Override public float getAltoTotal(){medir();float t=0;for(float h:rowHeights)t+=h;return t+gapY*Math.max(0,rowHeights.length-1);}

    @Override public void arrange(float x,float y,float w,float h){
        int n=componentes.size(); if(n==0)return;
        medir();
        int filas=rowHeights.length;
        float[] col=colWidths.clone(), row=rowHeights.clone();
        if(fillX){float c=Math.max(0f,(w-gapX*(columns-1))/columns);for(int i=0;i<col.length;i++)col[i]=c;}
        if(fillY){float c=Math.max(0f,(h-gapY*(filas-1))/filas);for(int i=0;i<row.length;i++)row[i]=c;}
        float bw=0;for(float cw:col)bw+=cw;bw+=gapX*Math.max(0,col.length-1);
        float bh=0;for(float rh:row)bh+=rh;bh+=gapY*Math.max(0,row.length-1);
        float sx=x+(w-bw)/2f, sy=y+(h-bh)/2f;
        for(int i=0;i<n;i++){
            int c=i%columns, r=i/columns;
            float cx=sx, cy=sy;
            for(int k=0;k<c;k++)cx+=col[k]+gapX;
            for(int k=0;k<r;k++)cy+=row[k]+gapY;
            componentes.get(i).setBounds(cx,cy,col[c],row[r]);
        }
    }
}
