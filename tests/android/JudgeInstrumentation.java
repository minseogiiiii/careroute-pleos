package com.careroute.firstround.qa;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.careroute.firstround.MainActivity;
import java.util.*;

/** Android UI integration checks, signed with the target release key; no test framework dependency. */
public final class JudgeInstrumentation extends Instrumentation {
    private MainActivity activity;private String phase="normal";private int checks=0;private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)phase=args.getString("phase","normal");start();}
    private void ui(Runnable action){final Throwable[] problem={null};runOnMainSync(()->{try{action.run();}catch(Throwable t){problem[0]=t;}});if(problem[0]!=null)throw new AssertionError(problem[0]);}
    private void collect(View v,List<View> all){all.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),all);}}
    private List<View> views(){List<View> r=new ArrayList<>();collect(activity.getWindow().getDecorView(),r);return r;}
    private View exact(String label){for(View v:views())if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;throw new AssertionError("Missing UI: "+label);}
    private void pass(String label){checks++;report.append("PASS ").append(checks).append(" · ").append(label).append('\n');Bundle b=new Bundle();b.putString("stream",label+"\n");sendStatus(0,b);}
    private void contains(String value){ui(()->{boolean found=false;for(View v:views())if(v instanceof TextView&&((TextView)v).getText().toString().contains(value))found=true;if(!found)throw new AssertionError("Missing text: "+value);});pass("화면: "+value);}
    private void enabled(String label,boolean expected){ui(()->{if(exact(label).isEnabled()!=expected)throw new AssertionError("Enabled mismatch: "+label);});pass("버튼 활성 "+expected+": "+label);}
    private void tap(String label){ui(()->{View v=exact(label);if(!v.isEnabled())throw new AssertionError("Disabled UI: "+label);boolean handled=v.performClick();if(!handled&&!(v instanceof CompoundButton))throw new AssertionError("No click handler: "+label);});waitForIdleSync();pass("버튼: "+label);}
    private void guiding(){tap("모의 경로 계산 완료");contains("계산 완료 · 안내 시작 전");tap("모의 안내 시작");contains("모의 안내 중");}
    private void count(int n){contains("모의 경로 요청  "+n+"회");}
    private void normal(){
        contains("180m");tap("대표 조건 적용 · 계단 회피 + 100m");contains("70m");contains("22분");contains("B · 무단차 가정 하차");contains("2주차장 연결 가정");contains("U · 별도 가정 픽업");
        tap("이 계획으로 하차·주차 안내 시작");count(1);guiding();tap("모의 도착 · 가정 B");contains("주차장으로 안내");count(1);
        tap("모의 도착 · 2주차장 연결 가정");contains("주차 후 대기");count(1);tap("모의 가족 연락 수신");count(1);
        tap("픽업 안내 시작");count(2);guiding();tap("모의 도착 · 가정 U");contains("픽업 위치 도착");enabled("탑승 확인 후 귀가 안내 시작",false);
        tap("운전자가 실제 탑승을 확인했어요");enabled("탑승 확인 후 귀가 안내 시작",true);tap("탑승 확인 후 귀가 안내 시작");count(3);guiding();tap("모의 도착 · 귀가");contains("방문 완료");count(3);
        tap("처음부터 다시 시연");tap("대표 조건 적용 · 계단 회피 + 100m");tap("이 계획으로 하차·주차 안내 시작");guiding();tap("모의 도착 · 가정 B");contains("주차장으로 안내");count(1);
    }
    private void restore(){
        contains("안내 상태 확인 필요");count(1);enabled("저장한 단계 확인 후 재개",false);
        tap("모의 정차 상태 확인");tap("저장한 단계 확인 후 재개");contains("주차장으로 안내");count(2);contains("요청 전송됨");
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Intent i=new Intent(getTargetContext(),MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=(MainActivity)startActivitySync(i);waitForIdleSync();
            if(phase.equals("restore"))restore();else normal();
            // Persist checkpoint before the host terminates the real application process.
            ui(()->activity.finish());waitForIdleSync();
            result.putString("stream",report+"ALL "+checks+" UI CHECKS PASSED / "+phase+"\n");result.putString("passed","true");result.putInt("checks",checks);finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream",report+"FAILED: "+t+"\n");result.putString("passed","false");finish(Activity.RESULT_CANCELED,result);}
    }
}
