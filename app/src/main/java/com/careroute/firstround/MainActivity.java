package com.careroute.firstround;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import static com.careroute.firstround.JourneyEngine.*;

/** Native offline judge prototype. No WebView, Internet, location or vehicle permission. */
public final class MainActivity extends Activity {
    private JourneyEngine e;
    private LinearLayout page;private ScrollView scroll;private int tab=0;
    private static final int INK=Color.rgb(16,45,50),GREEN=Color.rgb(33,102,80),MUTED=Color.rgb(88,108,110),BG=Color.rgb(244,246,240),LINE=Color.rgb(217,226,217),ORANGE=Color.rgb(158,73,38);
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        e=SessionCodec.decode(getSharedPreferences("judge-v1",MODE_PRIVATE).getString("session",null));
        if(e==null)e=new JourneyEngine();
        if(state!=null)tab=state.getInt("tab",0);
        getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);
        render(false);
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);render(false);}
    @Override public void onSaveInstanceState(Bundle out){out.putInt("tab",tab);super.onSaveInstanceState(out);}
    @Override protected void onStop(){save();super.onStop();}
    private void save(){if(e!=null)getSharedPreferences("judge-v1",MODE_PRIVATE).edit().putString("session",SessionCodec.encode(e)).apply();}
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    private void add(LinearLayout l,String s,int size,boolean bold,int color){TextView t=text(s,size,bold,color);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);l.addView(t,lp);}
    private LinearLayout card(LinearLayout parent){LinearLayout l=col();l.setPadding(dp(18),dp(18),dp(18),dp(14));l.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(l,lp);return l;}
    private Button button(LinearLayout parent,String label,boolean primary,boolean enabled,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));b.setMinHeight(dp(52));b.setPadding(dp(13),dp(10),dp(13),dp(10));
        b.setBackground(bg(enabled?(primary?GREEN:Color.rgb(234,240,232)):Color.rgb(228,232,227),12));b.setTextColor(enabled?(primary?Color.WHITE:INK):MUTED);b.setEnabled(enabled);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);lp.bottomMargin=dp(4);parent.addView(b,lp);
        b.setOnClickListener(v->{action.run();save();render(true);});return b;
    }
    private CheckBox check(LinearLayout parent,String label,boolean value,java.util.function.Consumer<Boolean> action){
        CheckBox b=new CheckBox(this);b.setText(label);b.setTextColor(INK);b.setTextSize(15);b.setMinHeight(dp(48));b.setChecked(value);parent.addView(b,new LinearLayout.LayoutParams(-1,-2));
        b.setOnCheckedChangeListener((v,checked)->{action.accept(checked);save();render(true);});return b;
    }
    private void chip(LinearLayout l,String label){TextView t=text(label,12,true,GREEN);t.setPadding(dp(10),dp(5),dp(10),dp(5));t.setBackground(bg(Color.rgb(231,239,220),20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.bottomMargin=dp(10);l.addView(t,p);}
    private boolean canTap(){return e.motion==Motion.STOPPED;}
    private String stage(){switch(e.stage){case PLAN:return "방문 계획";case TO_DROP:return "하차 위치로 안내";case TO_PARK:return "주차장으로 안내";case WAITING:return "주차 후 대기";case TO_PICKUP:return "픽업 위치로 안내";case AT_PICKUP:return "픽업 위치 도착";case HOME:return "귀가 안내";case DONE:return "방문 완료";default:return "안내 상태 확인 필요";}}
    private void render(boolean keepScroll){
        int oldY=scroll==null||!keepScroll?0:scroll.getScrollY();
        LinearLayout root=col();root.setBackgroundColor(BG);
        LinearLayout header=col();header.setPadding(dp(20),dp(16),dp(20),dp(12));header.setBackgroundColor(INK);
        add(header,"CareRoute",25,true,Color.WHITE);
        add(header,"가족은 입구에, 차는 주차장에. 다시 만날 곳까지.",13,false,Color.rgb(203,223,211));
        add(header,"가정 배치 모의 시연 · 실제 승하차 허용 미확인 · SDK 미연동",12,true,Color.rgb(200,231,169));root.addView(header);
        HorizontalScrollView tabs=new HorizontalScrollView(this);tabs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(dp(12),dp(8),dp(12),dp(8));
        String[] labels={"방문 시연","추천 근거","시연 도구","검증 범위"};
        for(int i=0;i<labels.length;i++){final int index=i;TextView t=text(labels[i],14,true,tab==i?Color.WHITE:INK);t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setBackground(bg(tab==i?INK:BG,20));row.addView(t);t.setOnClickListener(v->{tab=index;render(false);});}tabs.addView(row);root.addView(tabs);
        scroll=new ScrollView(this);scroll.setFillViewport(true);page=col();page.setPadding(dp(16),dp(6),dp(16),dp(24));scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!e.notice.isEmpty()){LinearLayout n=card(page);add(n,e.notice,15,true,ORANGE);}
        if(tab==0)journey();else if(tab==1)evidence();else if(tab==2)lab();else scope();
        if(tab==0&&e.stage==Stage.PLAN&&e.displayPlan()!=null){
            LinearLayout action=col();action.setPadding(dp(16),dp(4),dp(16),dp(8));action.setBackgroundColor(BG);
            button(action,"이 계획으로 하차·주차 안내 시작",true,canTap(),()->e.start());root.addView(action);
        }
        setContentView(root);if(keepScroll)scroll.post(()->scroll.scrollTo(0,oldY));
    }
    private void journey(){
        add(page,FACILITY+"  /  "+stage(),14,true,MUTED);
        boolean wide=getResources().getConfiguration().screenWidthDp>=760;
        LinearLayout left=page,right=page;
        if(wide){LinearLayout r=new LinearLayout(this);r.setBaselineAligned(false);page.addView(r);left=col();right=col();LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,-2,1.05f);a.rightMargin=dp(16);r.addView(left,a);r.addView(right,new LinearLayout.LayoutParams(0,-2,1));}
        LinearLayout map=card(left);chip(map,"승하차 가정 + 주차장 연결 가정 + 나중의 픽업");
        add(map,ASSUMPTION_NOTICE,12,true,ORANGE);
        FacilityMapView diagram=new FacilityMapView(this,e);map.addView(diagram,new LinearLayout.LayoutParams(-1,dp(wide?245:210)));
        Plan p=e.displayPlan();
        if(p!=null){
            LinearLayout metrics=new LinearLayout(this);metrics.setPadding(0,dp(14),0,dp(6));
            TextView walk=text("동승자 보행 합계\n"+p.walk+"m",20,true,INK),drive=text("방문 차량 합계\n"+p.drive+"분",20,true,INK);
            metrics.addView(walk,new LinearLayout.LayoutParams(0,-2,1));metrics.addView(drive,new LinearLayout.LayoutParams(0,-2,1));map.addView(metrics);
            add(map,"계획용 합성 입력 · 운전자 보행·진료 대기·요금 제외",11,false,MUTED);
        }
        if(e.stage==Stage.PLAN)planning(right);else execution(right);
        LinearLayout proof=card(left);add(proof,"모의 경로 요청  "+e.requestCount+"회",17,true,INK);
        add(proof,"실제 NaviHelper 호출 0회 · 정상 흐름의 시작 선택은 최초·픽업·귀가. 조건 입력과 시연 조작은 별도입니다.",12,false,MUTED);
        if(e.currentRequest!=0)add(proof,"요청 #"+e.currentRequest+"  ·  "+navLabel(),13,true,GREEN);
        if(e.stage!=Stage.PLAN&&e.stage!=Stage.DONE&&e.stage!=Stage.RECOVERY)eventPanel(right);
        if(e.stage==Stage.DONE){LinearLayout c=card(right);button(c,"처음부터 다시 시연",true,true,()->{e=new JourneyEngine();tab=0;});}
    }
    private void planning(LinearLayout parent){
        LinearLayout c=card(parent);chip(c,"01  동승자 조건에 맞춰 장소를 함께 선정");
        add(c,"같은 병원도, 내리는 곳이 달라집니다.",22,true,INK);
        add(c,"입구까지 스스로 이동할 수 있지만 걷기가 부담스러운 가족을 태운 상황입니다. 상시 동행이 필요한 경우는 이 시연의 대상이 아닙니다.",13,false,MUTED);
        check(c,"계단 없는 이동이 필요해요",e.options.stepFree,v->e.configure(v,e.options.maxWalk,e.options.walkFirst)).setEnabled(canTap());
        check(c,"동승자 보행 합계 100m 이하",e.options.maxWalk!=null,v->e.configure(e.options.stepFree,v?100:null,e.options.walkFirst)).setEnabled(canTap());
        check(c,"조건 충족 후 걷는 거리 우선",e.options.walkFirst,v->e.configure(e.options.stepFree,e.options.maxWalk,v)).setEnabled(canTap());
        add(c,"100m는 시연 설정값입니다. 거리·시간·계단 여부도 가정값입니다. 조건 충족 후 차량 합계 → 보행 거리 순입니다.",11,false,MUTED);
        button(c,"대표 조건 적용 · 계단 회피 + 100m",false,canTap(),()->e.configure(true,100,false));
        Plan p=e.displayPlan();
        if(p==null){add(c,"현재 조건에 맞는 조합이 없어요.",20,true,ORANGE);add(c,"필수 정보·통제·보행 조건을 확인해 주세요. 미확인 후보를 대안으로 추천하지 않습니다.",13,false,MUTED);}
        else {
            places(c,p);add(c,why(p),13,false,MUTED);
        }
        motionHint(c);
    }
    private void places(LinearLayout c,Plan p){
        add(c,"하차  "+name(p.drop)+"\n주차  "+name(p.park)+"\n픽업  "+name(p.pickup),15,true,INK);
    }
    private String why(Plan p){
        String a=p.stairs?"가정 A의 보행 경로를 계단 포함으로 설정했습니다.":"가정된 하차·픽업 보행 경로는 계단이 없습니다.";
        if(e.options.stepFree&&p.drop.equals("side"))a+=" 가정 A는 계단 조건으로 제외하고 B 하차와 2주차장 연결 가정을 함께 선택했습니다. A 조합보다 차량 합계는 늘어납니다.";
        else a+=" 등록된 후보 중 현재 조건과 우선순위를 적용한 결과입니다.";
        return a;
    }
    private void execution(LinearLayout parent){
        LinearLayout c=card(parent);Plan p=e.selected;
        if(e.stage==Stage.RECOVERY){
            chip(c,"저장된 계획 · 안내 활성 여부 미확인");add(c,"현재 상태를 확인하고 이어가세요.",23,true,INK);
            add(c,"앱 재시작·오류 뒤에는 안내나 정차 상태를 추정하지 않습니다. 자동으로 다시 요청하지 않습니다.",14,false,MUTED);
            if(p!=null)places(c,p);
            button(c,"모의 정차 상태 확인",false,true,()->{e.motion=Motion.STOPPED;e.log("시연자: 정차 상태 확인");});
            button(c,"저장한 단계 확인 후 재개",true,canTap(),()->e.resume());motionHint(c);return;
        }
        if(p==null){add(c,"저장된 계획이 없어요.",20,true,INK);return;}
        switch(e.stage){
            case TO_DROP:
                chip(c,"02  하차를 경유하고, 주차장까지");add(c,shortName(p.drop)+"에서 가족을 내려주세요.",24,true,INK);
                add(c,"현재 경유지  "+name(p.drop),16,true,INK);add(c,"이후 목적지  "+name(p.park),16,true,INK);
                add(c,"하차 경유지와 주차장 목적지를 한 요청에 넣었습니다. 내비 도착은 사람의 하차 완료가 아닙니다.",14,false,MUTED);break;
            case TO_PARK:
                chip(c,"03  기존 요청 유지 · 추가 요청 없음");add(c,shortName(p.park)+"으로 이어서 이동해요.",24,true,INK);
                add(c,"현재 목적지  "+name(p.park),16,true,INK);add(c,"가족을 내려준 뒤 운전자가 상황을 확인하고 출발합니다. 하차 완료 버튼이나 주차장 재검색 없이 승인한 안내를 유지합니다.",14,false,MUTED);break;
            case WAITING:
                chip(c,"04  주차 후 조용히 대기");add(c,"다시 만날 곳을 보관해요.",24,true,INK);
                add(c,"주차  "+name(p.park)+"\n약속한 픽업  "+name(p.pickup),16,true,INK);
                add(c,"건물 연결  "+(p.pickup.equals("east")?"가정 U에서 계단 없는 35m 연결 가정":"가정 A에서 계단 포함 90m 연결 가정"),14,false,MUTED);
                add(c,"연락은 기존 전화·메시지로 주고받습니다. 연락·대기만으로 안내를 시작하지 않습니다. 아래 선택 시 이용 조건을 다시 확인합니다.",14,false,MUTED);
                if(e.contact)chip(c,"시연 입력: 가족 연락 수신 상황 · 요청 증가 없음");
                if(!e.usable())add(c,"현재 보존한 픽업 계획은 이용할 수 없어요. 확인된 출입구로 변경하거나 시설 정보를 다시 확인해 주세요.",14,true,ORANGE);
                button(c,"만날 출입구 다시 확인·변경",false,canTap(),this::pickupDialog);
                button(c,"픽업 안내 시작",true,canTap()&&e.usable(),()->e.pickup());motionHint(c);break;
            case TO_PICKUP:
                chip(c,"05  운전자가 요청한 픽업 안내");add(c,shortName(p.pickup)+"에서 다시 만나요.",24,true,INK);add(c,name(p.pickup),16,true,INK);
                add(c,"하차와 픽업 지점은 다를 수 있습니다. 출발 때의 계획 수치로 미래 교통을 보장하지 않습니다.",14,false,MUTED);break;
            case AT_PICKUP:
                chip(c,"06  픽업 도착 ≠ 실제 탑승");add(c,"가족이 탔는지 직접 확인해 주세요.",24,true,INK);add(c,name(p.pickup),16,true,INK);
                add(c,"내비 도착·좌석·문 신호만으로 사람의 탑승이나 신원을 확정하지 않습니다.",14,false,MUTED);
                check(c,"운전자가 실제 탑승을 확인했어요",e.boarded,v->{if(canTap())e.boarded=v;}).setEnabled(canTap());
                button(c,"탑승 확인 후 귀가 안내 시작",true,canTap()&&e.boarded,()->e.home());motionHint(c);break;
            case HOME:chip(c,"07  운전자가 요청한 귀가 안내");add(c,"함께 돌아가요.",25,true,INK);add(c,"목적지  고정 귀가지",16,true,INK);add(c,"이번 APK는 고정된 합성 출발지·귀가지를 사용합니다. 실제 주소나 위치 정보를 수집하지 않습니다.",14,false,MUTED);break;
            case DONE:chip(c,"방문 시연 완료");add(c,"장소 선정부터, 다시 만날 때까지.",25,true,INK);add(c,"조건에 맞는 세 장소를 함께 정하고, 하차→주차를 이어 안내했습니다. 픽업·귀가는 운전자가 선택했습니다.",14,false,MUTED);break;
            default:break;
        }
        if(e.stage!=Stage.DONE&&!e.usable()&&e.stage!=Stage.WAITING)add(c,"시설 정보 변경: 남은 계획을 재확인해야 합니다. 현재 모의 안내를 실제 유효한 경로로 해석하지 마세요.",13,true,ORANGE);
        if(e.sensorPossible)add(c,"모의 센서: 승하차 가능성 있음 · 완료·신원은 미확인",12,true,MUTED);
    }
    private String navLabel(){switch(e.nav){case REQUESTED:return "요청 전송됨";case CALCULATED:return "계산 완료 · 안내 시작 전";case GUIDING:return "모의 안내 중";case UNKNOWN:return "안내 상태 미확인";default:return "안내 없음";}}
    private void motionHint(LinearLayout c){add(c,"모의 차량 상태: "+(e.motion==Motion.STOPPED?"정차 확인":e.motion==Motion.MOVING?"주행 중 · 새 요청 보류":"정차 미확인 · 새 요청 보류"),12,true,e.motion==Motion.STOPPED?MUTED:ORANGE);}
    private void eventPanel(LinearLayout parent){
        LinearLayout c=card(parent);add(c,"시연 조작 · 실제 차량 이벤트 아님",13,true,ORANGE);
        if(e.currentRequest!=0){
            if(e.nav==Nav.REQUESTED)button(c,"모의 경로 계산 완료",false,true,()->e.routingCompleted(e.currentRequest));
            else if(e.nav==Nav.CALCULATED)button(c,"모의 안내 시작",false,true,()->e.routeStarted(e.currentRequest));
            else if(e.nav==Nav.GUIDING){String d=e.arrivalDestination();if(d!=null)button(c,"모의 도착 · "+shortName(d),false,true,()->e.arrive(e.currentRequest,d));}
        }else if(e.stage==Stage.WAITING)button(c,"모의 가족 연락 수신",false,true,()->e.contact());
        add(c,"이 버튼들은 심사자가 도착·연락 상황을 재현하는 도구입니다. 실제 센서·내비 이벤트가 아닙니다.",11,false,MUTED);
    }
    private void pickupDialog(){
        if(e.stage!=Stage.WAITING||!canTap())return;
        String[] values={"front","east"};String[] labels=new String[2];for(int i=0;i<2;i++)labels[i]=name(values[i])+(e.pickupValid(values[i])?" · 조건 충족":" · 이용 불가");
        new AlertDialog.Builder(this).setTitle("정차 중 약속한 출입구 확인").setItems(labels,(d,i)->{e.changePickup(values[i]);save();render(true);}).setNegativeButton("닫기",null).show();
    }
    private void evidence(){
        LinearLayout c=card(page);chip(c,"선정 근거 · 등록한 20개 조합 비교");add(c,"가까운 곳부터 고르지 않습니다.",23,true,INK);
        add(c,"이용 허용·정보 유효성·진입 연결·보행 조건을 먼저 확인합니다. 조건을 통과한 조합만 차량 시간 또는 보행 거리로 정렬합니다.",14,false,MUTED);
        Ranking r=rank(e.options);add(c,"조건 충족 "+r.accepted.size()+"개  /  제외 "+r.rejected.size()+"개",16,true,GREEN);
        add(c,"차량 시간 = 출발→하차 + 하차→주차 + 나중의 주차→픽업 + 픽업→귀가. 운전자 보행·진료 대기·주차비는 포함하지 않습니다.",12,false,MUTED);
        Plan picked=e.displayPlan();
        for(Plan p:r.accepted){LinearLayout a=card(page);add(a,(picked!=null&&picked.id.equals(p.id)?"선택된 조합 · ":"조건 충족 조합 · ")+shortName(p.drop)+" 하차",16,true,INK);add(a,shortName(p.park)+" → "+shortName(p.pickup)+" 픽업",14,false,MUTED);add(a,"보행 "+p.walk+"m · 차량 "+p.drive+"분 · "+(p.stairs?"계단 포함":"계단 없음"),15,true,GREEN);add(a,"차량 구간(분): "+p.costs[0]+" + "+p.costs[1]+" + "+p.costs[2]+" + "+p.costs[3]+" = "+p.drive,12,false,MUTED);if(e.stage==Stage.PLAN)button(a,"이 조합 선택",false,canTap(),()->e.select(p.id));}
        Map<String,Integer> counts=new LinkedHashMap<>();for(Plan p:r.rejected)counts.put(p.reason,counts.getOrDefault(p.reason,0)+1);
        LinearLayout excluded=card(page);add(excluded,"제외 이유",18,true,INK);for(Map.Entry<String,Integer> en:counts.entrySet())add(excluded,en.getKey()+" · "+en.getValue()+"개",14,false,ORANGE);
        add(excluded,"전용구역 가정 10m·허용 미확인 가정 5m 후보도 더 가깝다는 이유로 추천하지 않습니다. 수치는 합성 입력이며 기존 내비 대비 실측 성과가 아닙니다.",12,false,MUTED);
    }
    private void lab(){
        LinearLayout c=card(page);chip(c,"심사자용 · 모든 입력은 모의");add(c,"정상 흐름과 실패 흐름을 확인하세요.",22,true,INK);motionHint(c);
        button(c,"모의 정차 확인",false,true,()->e.motion=Motion.STOPPED);
        button(c,"모의 주행 중",false,true,()->{e.motion=Motion.MOVING;e.boarded=false;e.sensorPossible=false;});
        button(c,"모의 정차 미확인",false,true,()->{e.motion=Motion.UNKNOWN;e.boarded=false;e.sensorPossible=false;});
        check(c,"가정한 허용 조건으로 시연 · 실제 허가 아님",e.options.simulatePermissions,v->{e.options.simulatePermissions=v;if(e.stage==Stage.PLAN)e.selected=null;e.log("가정 허용 시연 변경 · 실제 허용 검증 아님");});
        check(c,"모의 시설 정보가 유효함",e.options.evidenceValid,v->{e.options.evidenceValid=v;if(e.stage==Stage.PLAN)e.selected=null;e.log("시연자: 시설 정보 유효성 변경");});
        for(String id:new String[]{"front","side","west","north","south","east"})check(c,"통제 · "+name(id),e.options.closed.contains(id),v->{if(v)e.options.closed.add(id);else e.options.closed.remove(id);if(e.stage==Stage.PLAN)e.selected=null;e.log("시연자: 시설 통제 변경 · "+id);});
        check(c,"모의 승하차 센서 지원",e.sensorAvailable,v->{e.sensorAvailable=v;e.sensorPossible=false;});
        button(c,"모의 문 열림·닫힘 + 단계에 맞는 좌석 변화",false,true,()->e.sensor(true,true));
        button(c,"모의 문 신호만 수신",false,true,()->e.sensor(true,false));
        button(c,"오래된 요청 도착 주입 · 무시되는지 확인",false,true,()->e.arrive(-1,e.arrivalDestination()));
        button(c,"현재 요청 오류 주입",false,e.currentRequest!=0,()->e.error(e.currentRequest));
        button(c,"모의 앱 재시작 · 자동 재요청 없음",false,true,()->{e.restart();tab=0;});
        button(c,"진단 기록 복사",false,true,()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("CareRoute mock trace",e.report()));Toast.makeText(this,"모의 진단 기록을 복사했어요",Toast.LENGTH_SHORT).show();});
        button(c,"전체 초기화",false,true,()->new AlertDialog.Builder(this).setTitle("시연을 처음부터 시작할까요?").setMessage("저장된 가상 방문 계획과 모의 기록을 초기화합니다.").setPositiveButton("초기화",(d,w)->{e=new JourneyEngine();tab=0;save();render(false);}).setNegativeButton("취소",null).show());
        LinearLayout trace=card(page);add(trace,"요청·이벤트 기록",18,true,INK);add(trace,e.report(),12,false,MUTED);
    }
    private void scope(){
        LinearLayout c=card(page);chip(c,"1차 제출용 · 구현과 증거를 구분");add(c,"이번 APK에서 확인할 수 있는 것",22,true,INK);
        add(c,"네이티브 Android에서 조건에 따른 하차·주차·픽업 공동 선정, 제외 이유, 하차→주차 한 요청 유지, 대기·출입구 재확인·명시적 픽업·탑승 확인 후 귀가, 통제·정보 무효·정차 미확인·오래된 이벤트·재시작 처리를 실행합니다.",14,false,MUTED);
        add(c,"실제 연결은 아직 확인하지 않은 것",19,true,ORANGE);
        add(c,"Pleos NaviHelper·차량 센서·실제 지도·실제 병원 허용 위치·실시간 교통/주차·실제 편익은 연결·검증하지 않았습니다. 화면의 도착과 경로 요청은 모두 앱 내부 모의 기록입니다.",14,false,MUTED);
        add(c,"왜 차량용 앱인가",19,true,INK);add(c,"선정한 하차 지점을 경유지, 주차장을 목적지로 넘겨 운전자 재검색을 줄이고 나중의 픽업까지 이어 쓰는 설계입니다. 실제 차량 사용성은 별도 검증이 필요합니다.",14,false,MUTED);
        add(c,"지원 대상",19,true,INK);add(c,"입구까지 스스로 이동할 수 있지만 걷기가 부담스러운 동승자와, 별도로 주차·재픽업을 맡는 운전자입니다. 상시 동행이 필요한 사람의 단독 하차는 이 시연 대상에서 제외합니다.",14,false,MUTED);
        add(c,"개인정보·저장",19,true,INK);add(c,"인터넷·위치·전화·연락처·차량 권한을 요청하지 않습니다. 앱 내부에 합성 계획과 모의 기록만 저장합니다. 재실행 시 정차·안내 상태는 미확인으로 되돌리고 운전자 확인 뒤 재개합니다. 초기화 또는 앱 데이터 삭제로 지울 수 있습니다.",14,false,MUTED);
        add(c,"시설 조사와 시연 가정",19,true,INK);add(c,"실제 확인: 본관 1주차장 공사, 복수 주차장, CMI 안내 경로의 계단. 미확인: 승용차 하차·픽업 허용 위치. A/B/C/U와 주차장 연결은 개발용 가정이며 실제 지도와 좌표를 사용하지 않습니다. 가정 허용을 끄면 추천을 보류합니다.",14,false,MUTED);
        add(c,"기술 연결 계획",19,true,INK);add(c,"첫 안내: requestRouteWithWaypoints(경유 D, 목적지 P). 픽업·귀가: requestRoute(U/귀가지). 요청·계산 완료·안내 시작·도착 이벤트를 구분합니다. 이 명칭을 사용한 모의 기록은 실제 SDK 성공 증거가 아닙니다.",13,false,MUTED);
        add(c,"CareRoute 1.0.2-demo · Android 9 이상 · 한국어\n서울대병원 환경 참고 · 승하차·연결·수치 모두 가정 · 오프라인 실행",12,true,GREEN);
    }
}
