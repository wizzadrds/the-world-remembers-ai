package com.wizzadrds.theworldremembers.voice;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.UUID;
public final class VoiceRuntimeGameTest {
 @GameTest public void voiceProfileAndPriorityWorkInServer(GameTestHelper c){
  var manager=VoiceProfileManager.get(c.getLevel().getServer()); var id=UUID.randomUUID();
  manager.set(id,new VoiceProfile("es-ES","local",VoiceTemperament.CALM,0,0,50));
  if(manager.get(id)==null){c.fail("Voice profile was not persisted in server state");return;}
  var scheduler=new VoiceScheduler(1);
  scheduler.submit(new VoiceJob(UUID.randomUUID(),"ambient",VoicePriority.AMBIENT,c.getLevel().getGameTime()));
  scheduler.submit(new VoiceJob(UUID.randomUUID(),"direct",VoicePriority.DIRECT,c.getLevel().getGameTime()+1));
  if(scheduler.startNext().priority()!=VoicePriority.DIRECT){c.fail("Direct voice did not outrank ambient voice");return;}
  c.succeed();
 }
}
