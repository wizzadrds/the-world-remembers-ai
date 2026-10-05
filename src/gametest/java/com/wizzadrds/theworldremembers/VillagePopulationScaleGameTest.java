package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.village.VillageManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
public final class VillagePopulationScaleGameTest {
 @GameTest public void populationObservationScalesToManyVillagers(GameTestHelper context){
  for(int i=0;i<40;i++) context.spawn(EntityTypes.VILLAGER,1+(i%10),1+(i/10),1);
  context.runAtTickTime(1,()->{
   long start=System.nanoTime();
   TheWorldRemembers.processWorld(context.getLevel());
   long elapsed=System.nanoTime()-start;
   var all=VillageManager.get(context.getLevel().getServer()).all();
   if(all.isEmpty()){context.fail("No village was observed");return;}
   int population=all.stream().mapToInt(s->s.population()).sum();
   if(population<40){context.fail("Scale test observed only "+population+" villagers");return;}
   if(elapsed>2_000_000_000L){context.fail("Population observation exceeded 2 seconds: "+elapsed+"ns");return;}
   context.succeed();
  });
 }
}
