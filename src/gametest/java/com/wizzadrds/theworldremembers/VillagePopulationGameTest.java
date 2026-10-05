package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.village.VillageManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

public final class VillagePopulationGameTest {
 @GameTest public void liveVillagersCreatePersistentVillageObservation(GameTestHelper context) {
  context.spawn(EntityTypes.VILLAGER,2,1,2);
  context.spawn(EntityTypes.VILLAGER,4,1,2);
  context.spawn(EntityTypes.VILLAGER,3,1,4);
  context.runAtTickTime(1,()->{
   TheWorldRemembers.processWorld(context.getLevel());
   var villages=VillageManager.get(context.getLevel().getServer());
   if(villages.all().size()!=1){context.fail("Expected one village observation");return;}
   var state=villages.all().iterator().next();
   if(state.population()!=3){context.fail("Village population was not observed correctly: "+state.population());return;}
   if(state.lastObservedTick()!=context.getLevel().getGameTime()){context.fail("Village observation tick was not persisted");return;}
   context.succeed();
  });
 }
}
