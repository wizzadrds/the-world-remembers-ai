package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.village.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
public final class VillageMigrationGameTest {
 @GameTest public void migrationRecordsCanBePersisted(GameTestHelper context){
  context.spawn(EntityTypes.VILLAGER,2,1,2);
  context.runAtTickTime(1,()->{TheWorldRemembers.processWorld(context.getLevel());var v=VillageManager.get(context.getLevel().getServer()).all().iterator().next();var mm=VillageMigrationManager.get(context.getLevel().getServer());var m=new VillageMigration(v.villageId(),v.center(),v.center().offset(64,0,64),context.getLevel().getGameTime(),1);mm.record(m);if(mm.get(v.villageId()).size()!=1){context.fail("Migration was not persisted");return;}context.succeed();});
 }
}
