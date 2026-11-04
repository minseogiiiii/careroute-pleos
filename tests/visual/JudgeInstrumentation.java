package com.careroute.firstround.qa;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import java.io.*;
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
    private void tap(String label){
        final float[] xy=new float[2];
        ui(()->{View v=exact(label);if(!v.isEnabled())throw new AssertionError("Disabled UI: "+label);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});
        waitForIdleSync();SystemClock.sleep(180);
        ui(()->{View v=exact(label);Rect r=new Rect();if(!v.getGlobalVisibleRect(r)||r.width()<v.getWidth()-1||r.height()<v.getHeight()-1)throw new AssertionError("Touch target clipped: "+label+" "+r);int[] pos=new int[2];v.getLocationOnScreen(pos);xy[0]=pos[0]+v.getWidth()/2f;xy[1]=pos[1]+v.getHeight()/2f;});
        long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,xy[0],xy[1],0);MotionEvent up=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_UP,xy[0],xy[1],0);
        sendPointerSync(down);sendPointerSync(up);down.recycle();up.recycle();waitForIdleSync();pass("실제 포인터 입력: "+label);
    }
    private void capture(String name,boolean bottom){
        ui(()->{for(View v:views())if(v instanceof ScrollView)((ScrollView)v).fullScroll(bottom?View.FOCUS_DOWN:View.FOCUS_UP);});waitForIdleSync();SystemClock.sleep(180);
        ui(()->{try{View root=activity.getWindow().getDecorView();Bitmap bm=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bm));File dir=new File(getTargetContext().getExternalFilesDir(null),"visual-review");if(!dir.exists()&&!dir.mkdirs())throw new IOException("mkdir failed");try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){if(!bm.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("png failed");}bm.recycle();}catch(Exception ex){throw new RuntimeException(ex);}});
        Bitmap device=getUiAutomation().takeScreenshot();if(device==null)throw new AssertionError("Device screenshot failed: "+name);
        try{File dir=new File(getTargetContext().getExternalFilesDir(null),"visual-review");try(FileOutputStream out=new FileOutputStream(new File(dir,name+"-device.png"))){if(!device.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Device PNG failed");}}catch(IOException ex){throw new AssertionError(ex);}finally{device.recycle();}
        pass("실행 Activity View 및 기기 화면 캡처: "+name);
    }
    private void rotate(int requested,String stage,int count){
        ui(()->activity.setRequestedOrientation(requested));
        int desired=requested==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
        boolean ok=false;for(int i=0;i<80;i++){final boolean[] same={false};ui(()->same[0]=activity.getResources().getConfiguration().orientation==desired);if(same[0]){ok=true;break;}SystemClock.sleep(250);}
        if(!ok)throw new AssertionError("rotation did not complete");waitForIdleSync();contains(stage);count(count);pass("회전 후 단계·요청 유지 "+desired);
    }
    private void rotationPair(String stage,int count,String name){rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,stage,count);capture(name+"-portrait",false);rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,stage,count);}
    private void landscapeLayout(){
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,"방문 계획",0);
        ui(()->{if(activity.getResources().getConfiguration().screenWidthDp<760)throw new AssertionError("Wide test requires >=760dp");View map=null;for(View v:views())if(v.getClass().getSimpleName().equals("FacilityMapView"))map=v;if(map==null)throw new AssertionError("Missing map");int[] m=new int[2],t=new int[2];map.getLocationOnScreen(m);exact("같은 병원도, 내리는 곳이 달라집니다.").getLocationOnScreen(t);if(t[0]<m[0]+map.getWidth())throw new AssertionError("Wide columns overlap");});pass("가로 두 열의 수평 분리 확인");
        ui(()->{View v=exact("이 계획으로 하차·주차 안내 시작");Rect r=new Rect();if(!v.getGlobalVisibleRect(r)||r.height()<v.getHeight()-1||r.width()<v.getWidth()-1)throw new AssertionError("Primary start action is not fully visible without scroll");});pass("스크롤 전 시작 버튼 전체 노출");
    }
    private void guiding(){tap("모의 경로 계산 완료");contains("계산 완료 · 안내 시작 전");tap("모의 안내 시작");contains("모의 안내 중");}
    private void count(int n){contains("모의 경로 요청  "+n+"회");}
    private void normal(){
        contains("180m");capture("01-default-top",false);capture("01-default-bottom",true);tap("대표 조건 적용 · 계단 회피 + 100m");contains("70m");contains("22분");contains("본관 측면 승하차 구역");contains("남측 주차장");contains("동측 출입구 승하차 구역");capture("02-conditions-top",false);capture("02-conditions-bottom",true);
        tap("이 계획으로 하차·주차 안내 시작");count(1);guiding();rotationPair("하차 위치로 안내",1,"03-guiding");capture("03-guiding-landscape",false);tap("모의 도착 · 측면");contains("주차장으로 안내");count(1);capture("04-continue-parking",false);
        tap("모의 도착 · 남측 주차장");contains("주차 후 대기");count(1);tap("모의 가족 연락 수신");count(1);rotationPair("주차 후 대기",1,"05-waiting");capture("05-waiting-top",false);capture("05-waiting-bottom",true);
        tap("픽업 안내 시작");count(2);guiding();tap("모의 도착 · 동측 출입구");contains("픽업 위치 도착");enabled("탑승 확인 후 귀가 안내 시작",false);rotationPair("픽업 위치 도착",2,"06-pickup");capture("06-pickup-top",false);capture("06-pickup-bottom",true);
        tap("운전자가 실제 탑승을 확인했어요");enabled("탑승 확인 후 귀가 안내 시작",true);tap("탑승 확인 후 귀가 안내 시작");count(3);guiding();tap("모의 도착 · 귀가");contains("방문 완료");count(3);capture("07-done-top",false);capture("07-done-bottom",true);
        tap("처음부터 다시 시연");tap("대표 조건 적용 · 계단 회피 + 100m");tap("이 계획으로 하차·주차 안내 시작");guiding();tap("모의 도착 · 측면");contains("주차장으로 안내");count(1);
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
            if(phase.equals("restore")){restore();capture("08-restored",false);}else{landscapeLayout();normal();}
            // Persist checkpoint before the host terminates the real application process.
            ui(()->activity.finish());waitForIdleSync();
            result.putString("stream",report+"ALL "+checks+" UI CHECKS PASSED / "+phase+"\n");result.putString("passed","true");result.putInt("checks",checks);finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream",report+"FAILED: "+t+"\n");result.putString("passed","false");finish(Activity.RESULT_CANCELED,result);}
    }
}
