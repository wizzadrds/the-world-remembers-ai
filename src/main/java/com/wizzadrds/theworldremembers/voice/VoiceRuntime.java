package com.wizzadrds.theworldremembers.voice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class VoiceRuntime {
    private VoiceRuntime() {}
}

enum VoiceTemperament { TIMID, CALM, WARM, CHEERFUL, ASSERTIVE, NERVOUS, IRRITABLE, TIRED, SERIOUS, EXCITED }

record VoiceProfile(String language,String model,VoiceTemperament temperament,int pitch,int rate,int expressiveness) {
    VoiceProfile {
        if(language==null||language.isBlank()||model==null||model.isBlank()) throw new IllegalArgumentException();
        if(pitch<-100||pitch>100||rate<-100||rate>100||expressiveness<0||expressiveness>100) throw new IllegalArgumentException();
    }
}

record VoiceDeliveryState(VoiceTemperament temperament,int rate,int pitch,int intensity) {
    VoiceDeliveryState {
        rate=Math.max(-100,Math.min(100,rate));
        pitch=Math.max(-100,Math.min(100,pitch));
        intensity=Math.max(0,Math.min(100,intensity));
    }
}

final class VoiceDelivery {
    private VoiceDelivery(){}
    static VoiceDeliveryState adapt(VoiceProfile p,int stress,boolean danger,boolean tired,boolean excited) {
        VoiceTemperament t=danger?VoiceTemperament.NERVOUS:tired?VoiceTemperament.TIRED:excited?VoiceTemperament.EXCITED:p.temperament();
        int rate=p.rate()+(danger?15:0)+(tired?-20:0);
        int pitch=p.pitch()+(danger?10:0);
        int intensity=Math.min(100,p.expressiveness()+stress/2+(danger?20:0));
        return new VoiceDeliveryState(t,rate,pitch,intensity);
    }
}

final class VoiceProfileManager extends SavedData {
    private final Map<UUID,VoiceProfile> profiles=new HashMap<>();
    private static final Codec<VoiceTemperament> T=Codec.STRING.xmap(VoiceTemperament::valueOf,Enum::name);
    private static final Codec<VoiceProfile> P=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("language").forGetter(VoiceProfile::language),
        Codec.STRING.fieldOf("model").forGetter(VoiceProfile::model),
        T.fieldOf("temperament").forGetter(VoiceProfile::temperament),
        Codec.INT.fieldOf("pitch").forGetter(VoiceProfile::pitch),
        Codec.INT.fieldOf("rate").forGetter(VoiceProfile::rate),
        Codec.INT.fieldOf("expressiveness").forGetter(VoiceProfile::expressiveness)).apply(i,VoiceProfile::new));
    private static final Codec<VoiceProfileManager> C=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString,UUID::toString),P)
        .xmap(m->{var x=new VoiceProfileManager();x.profiles.putAll(m);return x;},x->x.profiles);
    private static final SavedDataType<VoiceProfileManager> TYPE=new SavedDataType<>(
        Identifier.fromNamespaceAndPath("the_world_remembers","voice_profiles"),VoiceProfileManager::new,C,null);
    static VoiceProfileManager get(MinecraftServer s){ServerLevel l=s.getLevel(ServerLevel.OVERWORLD);return l==null?new VoiceProfileManager():l.getDataStorage().computeIfAbsent(TYPE);}
    VoiceProfile get(UUID id){return profiles.get(id);}
    void set(UUID id,VoiceProfile p){profiles.put(id,p);setDirty();}
    int size(){return profiles.size();}
}

interface SpeechToTextAdapter { String transcribe(Path audio) throws IOException,InterruptedException; }
interface TextToSpeechAdapter { Path synthesize(String text,VoiceProfile profile,Path output) throws IOException,InterruptedException; }

final class ProcessSpeechToTextAdapter implements SpeechToTextAdapter {
    private final Path executable;
    ProcessSpeechToTextAdapter(Path executable){this.executable=executable;}
    public String transcribe(Path audio)throws IOException,InterruptedException{
        if(!Files.isRegularFile(audio))throw new FileNotFoundException(audio.toString());
        Process p=new ProcessBuilder(executable.toString(),audio.toAbsolutePath().toString()).redirectErrorStream(true).start();
        String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
        int code=p.waitFor(); if(code!=0)throw new IOException("Local STT exited with "+code); return out;
    }
}

final class ProcessTextToSpeechAdapter implements TextToSpeechAdapter {
    private final Path executable;
    ProcessTextToSpeechAdapter(Path executable){this.executable=executable;}
    public Path synthesize(String text,VoiceProfile profile,Path output)throws IOException,InterruptedException{
        if(text==null||text.isBlank())throw new IllegalArgumentException("text");
        Path parent=output.toAbsolutePath().getParent(); if(parent!=null)Files.createDirectories(parent);
        Process p=new ProcessBuilder(executable.toString(),text,output.toAbsolutePath().toString(),profile.model(),profile.language()).redirectErrorStream(true).start();
        String log=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        int code=p.waitFor(); if(code!=0)throw new IOException("Local TTS exited "+code+": "+log);
        if(!Files.isRegularFile(output))throw new IOException("TTS produced no output"); return output;
    }
}

record SpatialVoice(double sourceX,double sourceY,double sourceZ,double maxDistance) {
    SpatialVoice { if(maxDistance<=0)throw new IllegalArgumentException("maxDistance"); }
    float volumeAt(double x,double y,double z){
        double d=Math.sqrt(Math.pow(x-sourceX,2)+Math.pow(y-sourceY,2)+Math.pow(z-sourceZ,2));
        return d>=maxDistance?0f:(float)Math.max(0,1-d/maxDistance);
    }
}

enum VoicePriority { AMBIENT,DIRECT,IMPORTANT,DANGER }
record VoiceJob(UUID npcId,String text,VoicePriority priority,long createdTick) {}

final class VoiceScheduler {
    private final int capacity;
    private final PriorityQueue<VoiceJob> queue=new PriorityQueue<>(Comparator.comparingInt((VoiceJob j)->j.priority().ordinal()).reversed().thenComparingLong(VoiceJob::createdTick));
    private final Set<UUID> active=new HashSet<>();
    VoiceScheduler(int capacity){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;}
    boolean submit(VoiceJob j){
        if(active.contains(j.npcId()))return false;
        if(queue.size()>=capacity){
            VoiceJob lowest=queue.stream().min(Comparator.comparingInt((VoiceJob j2)->j2.priority().ordinal()).thenComparingLong(VoiceJob::createdTick)).orElse(null);
            if(lowest==null||j.priority().ordinal()<=lowest.priority().ordinal())return false;
            queue.remove(lowest);
        }
        queue.add(j); return true;
    }
    VoiceJob startNext(){var j=queue.poll();if(j!=null)active.add(j.npcId());return j;}
    void finish(UUID id){active.remove(id);}
    int queued(){return queue.size();}
}
