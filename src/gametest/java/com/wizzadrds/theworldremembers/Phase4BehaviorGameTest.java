package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.behavior.*; import com.wizzadrds.theworldremembers.relationship.*; import net.fabricmc.fabric.api.gametest.v1.GameTest; import net.minecraft.gametest.framework.GameTestHelper; import net.minecraft.world.entity.EntityTypes;
public final class Phase4BehaviorGameTest {
 @GameTest public void sleepingStateIsPersisted(GameTestHelper c){var v=c.spawn(EntityTypes.VILLAGER,2,1,2);c.setNight();c.runAtTickTime(1,()->{TheWorldRemembers.processWorld(c.getLevel());var s=NpcActivityManager.get(c.getLevel().getServer()).get(v.getUUID());if(s==null||s.activity()!=NpcActivity.SLEEPING){c.fail("Villager did not enter persistent sleeping state");return;}c.succeed();});}
}
