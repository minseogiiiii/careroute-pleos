package com.careroute.firstround;

import java.io.*;
import java.util.Base64;
import static com.careroute.firstround.JourneyEngine.*;

/** Local synthetic checkpoint; no external input or Java object deserialization. */
public final class SessionCodec {
    public static String encode(JourneyEngine e) {
        try {
            ByteArrayOutputStream raw=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(raw);
            d.writeInt(1);d.writeUTF(e.stage.name());d.writeUTF(e.savedStage.name());
            d.writeBoolean(e.options.stepFree);d.writeBoolean(e.options.evidenceValid);d.writeBoolean(e.options.walkFirst);
            d.writeInt(e.options.maxWalk==null?-1:e.options.maxWalk);d.writeInt(e.options.closed.size());for(String c:e.options.closed)d.writeUTF(c);
            d.writeUTF(e.selected==null?"":e.selected.id);d.writeInt(e.requestCount);d.writeBoolean(e.contact);d.writeBoolean(e.sensorAvailable);
            d.writeInt(e.requests.size());for(Request r:e.requests){d.writeInt(r.number);d.writeUTF(r.api);d.writeUTF(r.destination);d.writeUTF(r.waypoint==null?"":r.waypoint);}
            d.writeInt(e.logs.size());for(String l:e.logs)d.writeUTF(l);d.flush();return Base64.getEncoder().encodeToString(raw.toByteArray());
        }catch(IOException ex){throw new IllegalStateException(ex);}
    }
    public static JourneyEngine decode(String encoded) {
        if(encoded==null)return null;
        try {
            if(encoded.length()>200000)return null;
            DataInputStream d=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
            if(d.readInt()!=1)return null;JourneyEngine e=new JourneyEngine();
            e.stage=Stage.valueOf(d.readUTF());e.savedStage=Stage.valueOf(d.readUTF());
            e.options.stepFree=d.readBoolean();e.options.evidenceValid=d.readBoolean();e.options.walkFirst=d.readBoolean();
            int max=d.readInt();if(max< -1||max>10000)return null;e.options.maxWalk=max<0?null:max;
            int c=d.readInt();if(c<0||c>NODES.size())return null;for(int i=0;i<c;i++){String n=d.readUTF();if(!NODES.containsKey(n))return null;e.options.closed.add(n);}
            String id=d.readUTF();if(!id.isEmpty()){
                String[] parts=id.split("/");if(parts.length!=3||!java.util.Arrays.asList(DROPS).contains(parts[0])||!java.util.Arrays.asList(PARKS).contains(parts[1])||!java.util.Arrays.asList(PICKUPS).contains(parts[2]))return null;
                e.selected=new Plan(parts[0],parts[1],parts[2],null);
            }
            if(e.stage!=Stage.PLAN&&e.selected==null&&!(e.stage==Stage.RECOVERY&&e.savedStage==Stage.PLAN))return null;
            e.requestCount=d.readInt();if(e.requestCount<0||e.requestCount>1000000)return null;e.contact=d.readBoolean();e.sensorAvailable=d.readBoolean();
            c=d.readInt();if(c<0||c>50)return null;int previous=0;
            for(int i=0;i<c;i++){int n=d.readInt();String a=d.readUTF(),dst=d.readUTF(),w=d.readUTF();if(n<=previous||n>e.requestCount||!NODES.containsKey(dst)||(!w.isEmpty()&&!NODES.containsKey(w)))return null;previous=n;e.requests.add(new Request(n,a,dst,w.isEmpty()?null:w));}
            c=d.readInt();if(c<0||c>120)return null;for(int i=0;i<c;i++)e.logs.add(d.readUTF());
            if(d.available()!=0)return null;
            e.restart();return e;
        }catch(IOException|IllegalArgumentException ex){return null;}
    }
}
