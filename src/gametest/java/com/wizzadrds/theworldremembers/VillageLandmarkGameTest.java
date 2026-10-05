package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.village.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
public final class VillageLandmarkGameTest {
 @GameTest public void villagePoiIsRecordedAsLandmark(GameTestHelper context){
  context.spawn(EntityTypes.VILLAGER,2,1,2);
  context.runAtTickTime(1,()->{TheWorldRemembers.processWorld(context.getLevel());var v=VillageManager.get(context.getLevel().getServer()).all().iterator().next();var list=VillageLandmarkManager.get(context.getLevel().getServer()).get(v.villageId());if(list==null){context.fail("Landmark state missing");return;}context.succeed();});
 }
}
