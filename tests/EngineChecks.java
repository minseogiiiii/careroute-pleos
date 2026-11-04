package com.careroute.firstround;
import java.nio.file.*;
import java.util.*;
import static com.careroute.firstround.JourneyEngine.*;

public final class EngineChecks {
    static int checks=0;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+(++checks)+" · "+label);}
    static JourneyEngine constrained(){JourneyEngine e=new JourneyEngine();e.configure(true,100,false);return e;}
    static void beginGuidance(JourneyEngine e){e.routingCompleted(e.currentRequest);e.routeStarted(e.currentRequest);}
    static void arrival(JourneyEngine e){e.arrive(e.currentRequest,e.arrivalDestination());}
    static JourneyEngine waiting(){JourneyEngine e=constrained();e.start();beginGuidance(e);arrival(e);arrival(e);return e;}
    public static void main(String[] args)throws Exception {
        JourneyEngine e=new JourneyEngine();Plan p=e.displayPlan();check(p.id.equals("front/north/front")&&p.drive==16&&p.walk==180,"기본 16분/180m 정문 조합");
        e=constrained();p=e.displayPlan();check(p.id.equals("side/south/east")&&p.drive==22&&p.walk==70,"계단회피+100m → 측면/남측/동측 22분/70m");
        check(rank(e.options).accepted.stream().noneMatch(v->v.drop.equals("taxi")||v.drop.equals("unknown")),"가까운 불허·미확인 후보 제외");
        e.options.maxWalk=0;check(e.displayPlan()==null,"충족 후보 없음");e.start();check(e.requestCount==0,"후보 없으면 요청 없음");
        e=constrained();e.options.evidenceValid=false;e.start();check(e.requestCount==0,"정보 만료 시 시작 차단");
        e=constrained();e.motion=Motion.MOVING;e.start();check(e.requestCount==0,"주행 중 시작 차단");e.motion=Motion.UNKNOWN;e.start();check(e.requestCount==0,"정차 미확인 시작 차단");
        e.motion=Motion.STOPPED;e.start();e.start();check(e.requestCount==1&&e.requests.get(0).waypoint.equals("side")&&e.requests.get(0).destination.equals("south"),"중복 시작 방지 / 경유D·목적지P 한 요청");
        e.arrive(e.currentRequest,"side");check(e.stage==Stage.TO_DROP,"안내 시작 전 도착 거부");e.routingCompleted(e.currentRequest);check(e.stage==Stage.TO_DROP&&e.nav==Nav.CALCULATED,"계산 완료와 도착 구분");e.routeStarted(e.currentRequest);arrival(e);
        check(e.stage==Stage.TO_PARK&&e.requestCount==1&&e.currentRequest==1&&e.nav==Nav.GUIDING,"하차 뒤 기존 주차 안내 유지 / 추가 요청 없음");
        e.sensor(true,true);check(e.sensorPossible&&e.requestCount==1&&!e.boarded,"센서 가능성은 완료·탑승·요청 확정 아님");
        arrival(e);check(e.stage==Stage.WAITING&&e.requestCount==1,"주차 뒤 조용히 대기");e.contact();e.contact();check(e.requestCount==1&&e.stage==Stage.WAITING,"연락 반복 시 자동 요청 없음");
        e.options.closed.add("side");check(e.usable(),"이미 지난 하차 지점 통제는 남은 픽업 차단 안함");
        e.options.closed.add("east");e.pickup();check(e.requestCount==1&&e.stage==Stage.WAITING,"픽업 지점 통제 재확인 / 요청 차단");e.changePickup("front");check(e.selected.pickup.equals("east"),"계단 조건 위반 출입구로 변경 차단");
        e.options.closed.remove("east");e.motion=Motion.UNKNOWN;e.pickup();check(e.requestCount==1,"대기 후 정차 미확인 픽업 차단");e.motion=Motion.STOPPED;e.pickup();e.pickup();check(e.requestCount==2,"명시적 픽업 1회만 요청");
        int owned=e.currentRequest;e.arrive(1,e.selected.pickup);e.error(1);check(e.currentRequest==owned&&e.stage==Stage.TO_PICKUP,"이전 요청 도착·오류 제외");beginGuidance(e);arrival(e);e.home();check(e.stage==Stage.AT_PICKUP&&e.requestCount==2,"실제 탑승 확인 전 귀가 차단");
        e.sensor(true,true);check(!e.boarded&&e.requestCount==2,"좌석 변화로 탑승 확정·귀가 없음");e.boarded=true;e.home();check(e.requestCount==3&&e.stage==Stage.HOME,"직접 탑승 확인 후 명시적 귀가");beginGuidance(e);arrival(e);check(e.stage==Stage.DONE&&e.requestCount==3,"정상 방문 요청 1→1→2→3");
        e=constrained();e.start();beginGuidance(e);arrival(e);e=SessionCodec.decode(SessionCodec.encode(e));check(e!=null&&e.stage==Stage.RECOVERY&&e.currentRequest==0&&e.motion==Motion.UNKNOWN&&e.requestCount==1,"실제 저장·복원 뒤 자동 요청 없이 미확인 상태");
        e.resume();check(e.stage==Stage.RECOVERY&&e.requestCount==1,"재시작 후 정차 확인 전 재개 차단");e.motion=Motion.STOPPED;e.resume();check(e.stage==Stage.TO_PARK&&e.requestCount==2&&e.requests.get(1).waypoint==null&&e.requests.get(1).destination.equals("south"),"재시작 시 지나간 하차 재방문 없이 남은 주차만 요청");
        e=waiting();e.restart();e.motion=Motion.STOPPED;e.resume();check(e.stage==Stage.WAITING&&e.requestCount==1,"대기 화면 재개 시 요청 없음");
        e=constrained();e.start();int old=e.currentRequest;e.error(old);e.error(old);check(e.stage==Stage.RECOVERY&&e.requestCount==1,"내비 오류 후 자동 재시도 없음 / 이전 오류 무시");
        e=waiting();e.options.evidenceValid=false;e.restart();e.motion=Motion.STOPPED;e.resume();check(e.stage==Stage.RECOVERY&&e.requestCount==1,"정보 만료 상태 재개 차단");
        e=waiting();e.sensorAvailable=false;e.sensor(true,true);check(!e.sensorPossible,"센서 미지원 시 가능성 미확인");
        e=new JourneyEngine();e.start();beginGuidance(e);arrival(e);arrival(e);e.changePickup("east");check(e.selected.pickup.equals("east")&&e.requestCount==1,"정차 중 유효한 다른 출입구 수동 변경 / 요청 없음");
        check(SessionCodec.decode("garbage")==null&&SessionCodec.decode("")==null,"손상된 체크포인트 안전 초기화");
        JourneyEngine fresh=new JourneyEngine();check(rank(fresh.options).accepted.size()+rank(fresh.options).rejected.size()==20,"20개 조합 전수 평가");
        if(args.length>0){int parity=0;for(String line:Files.readAllLines(Path.of(args[0]))){String[] v=line.split("\t",-1);e=new JourneyEngine();e.options.stepFree=v[0].equals("1");e.options.maxWalk=v[1].equals("null")?null:Integer.valueOf(v[1]);e.options.walkFirst=v[2].equals("walk");e.options.evidenceValid=v[3].equals("1");if(!v[4].isEmpty())e.options.closed.addAll(Arrays.asList(v[4].split(",")));Ranking r=rank(e.options);String result=r.best()==null?"none":r.best().id+":"+r.best().walk+":"+r.best().drive; if(!result.equals(v[5])||r.accepted.size()!=Integer.parseInt(v[6]))throw new AssertionError("web/native parity "+line+" actual="+result);parity++;}check(parity==2048,"웹 기준 2,048개 조건 조합과 네이티브 추천 결과 일치");}
        System.out.println("ALL "+checks+" CHECKS PASSED");
    }
}
