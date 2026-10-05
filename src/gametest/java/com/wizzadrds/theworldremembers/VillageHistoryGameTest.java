package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.village.VillageHistoryManager;
import com.wizzadrds.theworldremembers.village.VillageManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;

public final class VillageHistoryGameTest {
 @GameTest public void villageHistoryTracksPeakPopulation(GameTestHelper context) {
  context.spawn(EntityTypes.VILLAGER,2,1,2);
  context.spawn(EntityTypes.VILLAGER,4,1,2);
  context.runAtTickTime(1,()->{
   TheWorldRemembers.processWorld(context.getLevel());
   var villages=VillageManager.get(context.getLevel().getServer());
   var history=VillageHistoryManager.get(context.getLevel().getServer());
   var state=villages.all().iterator().next();
   var h=history.get(state.villageId());
   if(h==null||h.peakPopulation()!=2){context.fail("Village history did not record population peak");return;}
   if(h.firstObservedTick()!=context.getLevel().getGameTime()){context.fail("History first observation is wrong");return;}
   context.succeed();
  });
 }
}
