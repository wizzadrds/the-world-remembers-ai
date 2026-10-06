package com.wizzadrds.theworldremembers.voice;
import org.junit.jupiter.api.Test; import java.nio.file.Path; import java.util.UUID; import static org.junit.jupiter.api.Assertions.*;
class VoiceRuntimeTest {
 @Test void deliveryRespondsToState(){var p=new VoiceProfile("es","local",VoiceTemperament.CHEERFUL,0,0,50);assertEquals(VoiceTemperament.NERVOUS,VoiceDelivery.adapt(p,0,true,false,false).temperament());assertTrue(VoiceDelivery.adapt(p,0,true,false,false).rate()>0);}
 @Test void spatialAttenuationIsDeterministic(){var s=new SpatialVoice(0,0,0,10);assertEquals(1f,s.volumeAt(0,0,0),.001);assertEquals(.5f,s.volumeAt(5,0,0),.001);assertEquals(0f,s.volumeAt(10,0,0),.001);}
 @Test void schedulerPrioritizesDirect(){var s=new VoiceScheduler(1);assertTrue(s.submit(new VoiceJob(UUID.randomUUID(),"ambient",VoicePriority.AMBIENT,1)));assertTrue(s.submit(new VoiceJob(UUID.randomUUID(),"direct",VoicePriority.DIRECT,2)));assertEquals(VoicePriority.DIRECT,s.startNext().priority());}
 @Test void adaptersRemainLocalAndDeterministic(){var stt=new ProcessSpeechToTextAdapter(Path.of("missing-local-stt"));assertThrows(Exception.class,()->stt.transcribe(Path.of("missing.wav")));var tts=new ProcessTextToSpeechAdapter(Path.of("missing-local-tts"));assertThrows(IllegalArgumentException.class,()->tts.synthesize("",new VoiceProfile("es","local",VoiceTemperament.CALM,0,0,50),Path.of("out.wav")));}
}
