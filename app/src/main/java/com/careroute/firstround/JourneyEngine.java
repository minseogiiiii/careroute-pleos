package com.careroute.firstround;

import java.util.*;

/** Pure Java domain model. All locations, costs, vehicle signals and navigation are synthetic. */
public final class JourneyEngine {
    public static final String VERSION="1.0.2-demo";
    public static final String FACILITY="서울대병원 환경 참고 · 가정 배치";
    public static final String ASSUMPTION_NOTICE="승하차 위치·허용·거리·시간은 시연 가정입니다. 실제 시설 승인·경로가 아닙니다.";
    /** Every stop in this fixture is assumed, never an externally verified permission. */
    public static boolean permissionVerified(String id){return false;}

    public static final String[] DROPS={"front","side","west","taxi","unknown"};
    public static final String[] PARKS={"north","south"}, PICKUPS={"front","east"};
    public static final Map<String,Node> NODES=new LinkedHashMap<>();
    public static final Map<String,Map<String,Integer>> EDGES=new LinkedHashMap<>();
    static {
        node("origin","고정 출발지","출발",0,false,"allowed",78,398);
        node("home","고정 귀가지","귀가",0,false,"allowed",700,398);
        node("front","A · 로비 앞 가정 승하차","가정 A",90,true,"allowed",380,308);
        node("side","B · 무단차 가정 하차","가정 B",35,false,"allowed",215,208);
        node("west","C · 대체 가정 하차","가정 C",55,false,"allowed",86,154);
        node("taxi","전용구역 가정","전용 가정",10,false,"prohibited",482,304);
        node("unknown","허용 미확인 가정","미확인",5,false,"unknown",584,330);
        node("north","CMI 연결 가정","CMI 연결 가정",0,false,"allowed",378,62);
        node("south","2주차장 연결 가정","2주차장 연결 가정",0,false,"allowed",161,354);
        node("east","U · 별도 가정 픽업","가정 U",35,false,"allowed",586,202);
        edge("origin","front",3);edge("origin","side",4);edge("origin","west",3);edge("origin","taxi",2);edge("origin","unknown",1);
        edge("front","north",4);edge("front","south",10);edge("front","home",5);
        edge("side","north",14);edge("side","south",6);edge("west","north",11);edge("west","south",15);
        edge("taxi","north",3);edge("taxi","south",4);edge("unknown","north",1);edge("unknown","south",1);
        edge("north","front",4);edge("north","east",4);edge("south","front",10);edge("south","east",4);edge("east","home",8);
    }
    public static final class Node {
        public final String id,name,shortName,permission; public final int walk,x,y; public final boolean stairs;
        Node(String i,String n,String sh,int w,boolean st,String p,int x,int y){id=i;name=n;shortName=sh;walk=w;stairs=st;permission=p;this.x=x;this.y=y;}
    }
    static void node(String i,String n,String sh,int w,boolean st,String p,int x,int y){NODES.put(i,new Node(i,n,sh,w,st,p,x,y));}
    static void edge(String a,String b,int c){EDGES.computeIfAbsent(a,k->new LinkedHashMap<>()).put(b,c);}
    public static int cost(String a,String b){return EDGES.containsKey(a)?EDGES.get(a).getOrDefault(b,-1):-1;}
    public static String name(String id){return NODES.containsKey(id)?NODES.get(id).name:id;}
    public static String shortName(String id){return NODES.containsKey(id)?NODES.get(id).shortName:id;}
    public static final class Options {
        public boolean stepFree=false,evidenceValid=true,walkFirst=false;
        public boolean simulatePermissions=true;
        public Integer maxWalk=null;
        public final Set<String> closed=new LinkedHashSet<>();
    }
    public static final class Plan {
        public final String drop,park,pickup,id; public final int walk,drive; public final boolean stairs;
        public final int[] costs; public final String reason;
        Plan(String d,String p,String u,String reason){
            drop=d;park=p;pickup=u;id=d+"/"+p+"/"+u;
            walk=NODES.get(d).walk+NODES.get(u).walk;stairs=NODES.get(d).stairs||NODES.get(u).stairs;
            costs=new int[]{cost("origin",d),cost(d,p),cost(p,u),cost(u,"home")};
            drive=Arrays.stream(costs).sum();this.reason=reason;
        }
    }
    public static final class Ranking {
        public final List<Plan> accepted=new ArrayList<>(),rejected=new ArrayList<>();
        public Plan best(){return accepted.isEmpty()?null:accepted.get(0);}
    }
    public static Ranking rank(Options o){
        Ranking r=new Ranking();
        for(String d:DROPS)for(String p:PARKS)for(String u:PICKUPS){
            String why=null; List<Node> ns=Arrays.asList(NODES.get(d),NODES.get(p),NODES.get(u));
            if(!o.simulatePermissions)why="실제 승하차 허용 미확인 · 가정 시연 꺼짐";
            else if(!o.evidenceValid)why="필수 정보의 재확인 기한이 지남";
            else if(ns.stream().anyMatch(n->n.permission.equals("prohibited")))why="일반 승용차 승하차 불허";
            else if(ns.stream().anyMatch(n->!n.permission.equals("allowed")))why="이용 허용 정보 미확인";
            else if(ns.stream().anyMatch(n->o.closed.contains(n.id)))why="현재 이용 불가";
            Plan candidate=new Plan(d,p,u,null);
            if(why==null&&Arrays.stream(candidate.costs).anyMatch(c->c<0))why="차량 연결 비용 미확인";
            if(why==null&&o.stepFree&&candidate.stairs)why="계단 없는 이동 조건 위반";
            if(why==null&&o.maxWalk!=null&&candidate.walk>o.maxWalk)why="보행 합계 제한 초과";
            Plan result=new Plan(d,p,u,why);(why==null?r.accepted:r.rejected).add(result);
        }
        r.accepted.sort((a,b)->{
            int c=Integer.compare(o.walkFirst?a.walk:a.drive,o.walkFirst?b.walk:b.drive);
            if(c==0)c=Integer.compare(o.walkFirst?a.drive:a.walk,o.walkFirst?b.drive:b.walk);
            return c!=0?c:a.id.compareTo(b.id);
        });return r;
    }
    public enum Stage { PLAN,TO_DROP,TO_PARK,WAITING,TO_PICKUP,AT_PICKUP,HOME,DONE,RECOVERY }
    public enum Motion { STOPPED,MOVING,UNKNOWN }
    public enum Nav { IDLE,REQUESTED,CALCULATED,GUIDING,UNKNOWN }
    public static final class Request {
        public final int number;public final String api,destination,waypoint;
        Request(int n,String a,String d,String w){number=n;api=a;destination=d;waypoint=w;}
    }
    public Options options=new Options();public Stage stage=Stage.PLAN,savedStage=Stage.PLAN;
    public Motion motion=Motion.STOPPED;public Nav nav=Nav.IDLE;public Plan selected=null;
    public int requestCount=0,currentRequest=0;public boolean contact=false,boarded=false,sensorAvailable=true,sensorPossible=false;
    public String notice="";public final List<Request> requests=new ArrayList<>();public final List<String> logs=new ArrayList<>();
    public void log(String message){logs.add(message);if(logs.size()>120)logs.remove(0);}
    void issue(String api,String dest,String waypoint){
        currentRequest=++requestCount;nav=Nav.REQUESTED;
        requests.add(new Request(currentRequest,api,dest,waypoint));if(requests.size()>50)requests.remove(0);
        log("#"+currentRequest+" 모의 요청 "+api+" → "+name(dest)+(waypoint==null?"":" / 경유 "+name(waypoint)));
    }
    public Plan displayPlan(){return selected==null?rank(options).best():selected;}
    boolean stopped(){if(motion!=Motion.STOPPED){notice="정차가 확인된 뒤 선택해 주세요. 현재는 "+(motion==Motion.MOVING?"주행 중":"정차 미확인")+" 모의 상태예요.";return false;}return true;}
    public void configure(boolean step,Integer max,boolean walk){notice="";if(stage!=Stage.PLAN||!stopped())return;options.stepFree=step;options.maxWalk=max;options.walkFirst=walk;selected=null;}
    public void select(String id){if(stage!=Stage.PLAN||!stopped())return;for(Plan p:rank(options).accepted)if(p.id.equals(id)){selected=p;return;}}
    public void start(){
        notice="";if(stage!=Stage.PLAN||!stopped())return;Ranking r=rank(options);Plan p=selected==null?r.best():r.accepted.stream().filter(a->a.id.equals(selected.id)).findFirst().orElse(null);
        if(p==null){notice="현재 조건에 맞는 조합이 없어요. 미확인 후보로 안내하지 않아요.";return;}
        selected=p;stage=Stage.TO_DROP;contact=false;issue("requestRouteWithWaypoints",p.park,p.drop);
    }
    public void routingCompleted(int id){if(currentRequest!=0&&id==currentRequest&&nav==Nav.REQUESTED){nav=Nav.CALCULATED;log("경로 계산 완료 · 도착 아님");}else log("이전·중복 계산 이벤트 제외");}
    public void routeStarted(int id){if(currentRequest!=0&&id==currentRequest&&(nav==Nav.REQUESTED||nav==Nav.CALCULATED)){nav=Nav.GUIDING;log("모의 안내 시작 · 도착 아님");}else log("이전·중복 안내 시작 제외");}
    public String arrivalDestination(){if(selected==null)return null;switch(stage){case TO_DROP:return selected.drop;case TO_PARK:return selected.park;case TO_PICKUP:return selected.pickup;case HOME:return "home";default:return null;}}
    public void arrive(int id,String destination){
        notice="";if(id==0||id!=currentRequest||nav!=Nav.GUIDING){log("소유권이 없거나 이전 요청의 도착 제외");return;}
        if(destination==null||!destination.equals(arrivalDestination())){log("현재 단계·목적지와 다른 도착 제외");return;}
        motion=Motion.STOPPED;sensorPossible=false;
        switch(stage){
            case TO_DROP:stage=Stage.TO_PARK;log("하차 경유지 도착 · 기존 주차 안내 유지 · 사람의 하차 완료는 미확인");return;
            case TO_PARK:stage=Stage.WAITING;log("주차장 도착 · 요청 없이 대기");break;
            case TO_PICKUP:stage=Stage.AT_PICKUP;boarded=false;log("픽업 지점 도착 · 운전자가 실제 탑승 확인");break;
            case HOME:stage=Stage.DONE;log("모의 귀가 도착");break;
            default:return;
        }nav=Nav.IDLE;currentRequest=0;
    }
    public boolean usable(){
        if(selected==null)return false;Stage st=stage==Stage.RECOVERY?savedStage:stage;
        if(st==Stage.AT_PICKUP||st==Stage.HOME||st==Stage.DONE)return true;
        Plan p=selected;
        if(!options.simulatePermissions)return false;
        if(st==Stage.TO_PARK)return options.evidenceValid&&!options.closed.contains(p.park)&&cost(p.drop,p.park)>=0;
        if(st==Stage.WAITING||st==Stage.TO_PICKUP)return pickupValid(p.pickup);
        return rank(options).accepted.stream().anyMatch(v->v.id.equals(p.id));
    }
    public boolean pickupValid(String u){
        if(selected==null||!Arrays.asList(PICKUPS).contains(u))return false;
        Node n=NODES.get(u);return options.simulatePermissions&&options.evidenceValid&&!options.closed.contains(selected.park)&&!options.closed.contains(u)&&n.permission.equals("allowed")&&(!options.stepFree||!n.stairs)&&(options.maxWalk==null||NODES.get(selected.drop).walk+n.walk<=options.maxWalk)&&cost(selected.park,u)>=0&&cost(u,"home")>=0;
    }
    public void changePickup(String u){
        notice="";if(stage!=Stage.WAITING||!stopped())return;
        if(!pickupValid(u)){notice="이 출입구는 현재 조건을 충족하지 않아요. 확인된 대안이 필요해요.";return;}
        selected=new Plan(selected.drop,selected.park,u,null);log("운전자 선택: 픽업 위치 재확인 → "+name(u)+" · 새 경로 요청 없음");
    }
    public void pickup(){notice="";if(stage!=Stage.WAITING||!stopped())return;if(!usable()){notice="현재 픽업 정보가 유효하지 않아요. 출입구·통제·보행 조건을 다시 확인해 주세요.";return;}stage=Stage.TO_PICKUP;issue("requestRoute",selected.pickup,null);}
    public void home(){notice="";if(stage!=Stage.AT_PICKUP||!stopped())return;if(!boarded){notice="운전자가 실제 탑승을 확인한 뒤 귀가 안내를 선택해 주세요.";return;}stage=Stage.HOME;issue("requestRoute","home",null);}
    public void contact(){if(stage==Stage.WAITING){contact=true;log("시연 입력: 기존 전화·메시지로 연락을 받은 상황 · 자동 요청 없음");}}
    public void sensor(boolean door,boolean correctSeatDirection){sensorPossible=sensorAvailable&&motion==Motion.STOPPED&&(stage==Stage.TO_PARK||stage==Stage.AT_PICKUP)&&door&&correctSeatDirection;log(sensorPossible?"모의 센서: 승하차 가능성만 있음 · 완료·신원 확정 없음":"모의 센서: 승하차 미확인");}
    public void restart(){savedStage=stage==Stage.RECOVERY?savedStage:stage;stage=Stage.RECOVERY;nav=Nav.UNKNOWN;currentRequest=0;motion=Motion.UNKNOWN;sensorPossible=false;boarded=false;log("재시작 · 계획만 복원 · 안내 활성·정차 미확인 · 자동 재요청 없음");}
    public void error(int id){if(id==0||id!=currentRequest){log("이전 요청 오류 제외");return;}restart();notice="모의 내비 오류가 발생했어요. 정차 후 계획을 확인하고 재개해 주세요.";}
    public void resume(){
        notice="";if(stage!=Stage.RECOVERY||!stopped())return;if(savedStage!=Stage.PLAN&&!usable()){notice="남은 경로의 이용 조건이 달라졌어요. 현재 상태로 재개할 수 없어요.";return;}
        stage=savedStage;
        switch(stage){
            case TO_DROP:issue("requestRouteWithWaypoints",selected.park,selected.drop);break;
            case TO_PARK:issue("requestRoute",selected.park,null);break;
            case TO_PICKUP:issue("requestRoute",selected.pickup,null);break;
            case HOME:issue("requestRoute","home",null);break;
            default:nav=Nav.IDLE;log("저장한 화면 복원 · 새 경로 요청 없음");
        }
    }
    public String report(){
        StringBuilder b=new StringBuilder("CareRoute "+VERSION+" | Android native | OFFLINE SIMULATION\n"+FACILITY+"\n"+ASSUMPTION_NOTICE+"\n실제 Pleos SDK 호출: 0 / 실제 허용 확인 지점: 0\n");
        b.append("단계: ").append(stage).append(" / 모의 경로 요청: ").append(requestCount).append("\n");
        if(selected!=null)b.append("계획: ").append(selected.id).append(" / 합성 차량 ").append(selected.drive).append("분 / 보행 ").append(selected.walk).append("m\n");
        b.append("가정 허용 시연: ").append(options.simulatePermissions).append(" / 실제 허용 검증: false\n");
        b.append("계단 회피: ").append(options.stepFree).append(" / 보행 상한: ").append(options.maxWalk).append(" / 정차: ").append(motion).append("\n");
        for(Request r:requests)b.append('#').append(r.number).append(' ').append(r.api).append(" destination=").append(r.destination).append(" waypoint=").append(r.waypoint).append(" [mock]\n");
        for(String l:logs)b.append("· ").append(l).append('\n');return b.toString();
    }
}
