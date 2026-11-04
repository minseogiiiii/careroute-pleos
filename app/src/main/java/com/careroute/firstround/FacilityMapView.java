package com.careroute.firstround;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import static com.careroute.firstround.JourneyEngine.*;

/** Diagram, never a road map. Coordinates intentionally have no latitude/longitude. */
public final class FacilityMapView extends View {
    private final Paint p=new Paint(3);private final JourneyEngine engine;
    private final Path line=new Path();private final DashPathEffect dash=new DashPathEffect(new float[]{12,9},0);
    public FacilityMapView(Context c){this(c,new JourneyEngine());}
    public FacilityMapView(Context c,JourneyEngine e){super(c);engine=e;setContentDescription("가상 한빛병원 배치도. 실제 도로 지도 아님");}
    void text(Canvas c,String t,float x,float y,int size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float sx=getWidth()/780f,sy=getHeight()/450f;c.save();c.scale(sx,sy);
        c.drawColor(Color.rgb(20,48,54));p.setStrokeWidth(1);p.setColor(Color.rgb(34,65,71));for(int x=0;x<780;x+=30)c.drawLine(x,0,x,450,p);for(int y=0;y<450;y+=30)c.drawLine(0,y,780,y,p);
        Plan plan=engine.displayPlan();
        if(plan!=null){
            String[] path={"origin",plan.drop,plan.park,plan.pickup,"home"};
            for(int i=0;i<path.length-1;i++){
                Node a=NODES.get(path[i]),b=NODES.get(path[i+1]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(i==1?7:4);p.setColor(i==1?Color.rgb(184,230,176):Color.rgb(104,155,147));
                if(i>=2)p.setPathEffect(dash);else p.setPathEffect(null);
                line.reset();line.moveTo(a.x,a.y);
                if((a.id.equals("front")&&b.id.equals("north"))||(a.id.equals("north")&&b.id.equals("front"))){
                    float outside=i>=2?520:255;line.lineTo(outside,a.y);line.lineTo(outside,b.y);line.lineTo(b.x,b.y);
                }else{float bend=(a.x+b.x)/2f;line.cubicTo(bend,a.y,bend,b.y,b.x,b.y);}
                c.drawPath(line,p);
            }
            p.setPathEffect(null);p.setStyle(Paint.Style.FILL);
        }
        p.setColor(Color.rgb(44,77,82));c.drawRoundRect(287,137,482,251,16,16,p);
        text(c,"한빛병원 본관",384,183,24,Color.rgb(225,239,224));text(c,"가상 시설 · 실제 도로 아님",384,213,15,Color.rgb(163,191,185));
        for(Node n:NODES.values()){
            boolean chosen=plan!=null&&(n.id.equals(plan.drop)||n.id.equals(plan.park)||n.id.equals(plan.pickup));
            p.setColor(engine.options.closed.contains(n.id)?Color.rgb(206,132,108):chosen?Color.rgb(184,230,176):Color.rgb(103,139,140));
            c.drawCircle(n.x,n.y,chosen?10:6,p);
            String label=n.shortName;if(chosen)label=(n.id.equals(plan.drop)&&n.id.equals(plan.pickup)?"하차·픽업 · ":n.id.equals(plan.drop)?"하차 · ":n.id.equals(plan.park)?"주차 · ":"픽업 · ")+label;
            int labelOffset=n.id.equals("north")?-18:n.id.equals("taxi")?59:29;
            text(c,label,n.x,n.y+labelOffset,chosen?20:15,chosen?Color.rgb(224,244,214):Color.rgb(159,185,184));
            if(engine.options.closed.contains(n.id))text(c,"통제",n.x,n.y-14,14,Color.rgb(245,178,151));
        }
        text(c,"실선: 출발·하차·주차    점선: 나중의 픽업·귀가",390,444,14,Color.rgb(161,189,184));c.restore();
    }
}
