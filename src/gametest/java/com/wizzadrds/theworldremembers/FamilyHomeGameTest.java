package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.home.NpcHome;
import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
public final class FamilyHomeGameTest {
 @GameTest public void spouseAndChildShareFamilyHome(GameTestHelper context) {
  Villager a=context.spawn(EntityTypes.VILLAGER,2,1,2), b=context.spawn(EntityTypes.VILLAGER,5,1,2), child=context.spawn(EntityTypes.VILLAGER,3,1,4); child.setBaby(true);
  context.runAtTickTime(1,()->{
   FamilyManager f=FamilyManager.get(context.getLevel().getServer()); f.addSpouses(a.getUUID(),b.getUUID()); f.addParentChild(a.getUUID(),child.getUUID());
   TheWorldRemembers.processWorld(context.getLevel());
   NpcHomeManager h=NpcHomeManager.get(context.getLevel()); NpcHome ha=h.get(a.getUUID()), hb=h.get(b.getUUID()), hc=h.get(child.getUUID());
   if(ha==null||hb==null||hc==null){context.fail("Family members did not receive homes");return;}
   if(!ha.homePos().equals(hb.homePos())||!ha.homePos().equals(hc.homePos())){context.fail("Family did not converge on one home");return;}
   context.succeed();
  });
 }
}
