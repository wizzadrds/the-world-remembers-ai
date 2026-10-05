package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.village.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
public final class VillageDefenseGameTest {
 @GameTest public void nearbyIronGolemBecomesVillageDefense(GameTestHelper context){
  context.spawn(EntityTypes.VILLAGER,2,1,2); context.spawn(EntityTypes.VILLAGER,4,1,2);
  context.spawn(EntityTypes.IRON_GOLEM,3,1,5);
  context.runAtTickTime(1,()->{TheWorldRemembers.processWorld(context.getLevel());var v=VillageManager.get(context.getLevel().getServer()).all().iterator().next();var d=VillageDefenseManager.get(context.getLevel().getServer()).get(v.villageId());if(d==null||d.livingGolems()!=1){context.fail("Village defense did not observe golem");return;}context.succeed();});
 }
}
