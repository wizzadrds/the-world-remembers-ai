package com.wizzadrds.theworldremembers;
import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.family.FamilyProtectionManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
public final class FamilyProtectionGameTest {
 @GameTest public void protectorFollowsChild(GameTestHelper context) {
  Villager parent=context.spawn(EntityTypes.VILLAGER,1,1,1), child=context.spawn(EntityTypes.VILLAGER,8,1,1); child.setBaby(true);
  context.runAtTickTime(1,()->{
   FamilyManager f=FamilyManager.get(context.getLevel().getServer()); f.addParentChild(parent.getUUID(),child.getUUID());
   TheWorldRemembers.processWorld(context.getLevel());
   if(!parent.getUUID().equals(FamilyProtectionManager.get(context.getLevel().getServer()).protectorOf(child.getUUID()))){context.fail("Protector was not assigned");return;}
   double before=parent.distanceToSqr(child);
   context.runAtTickTime(20,()->{if(parent.distanceToSqr(child)>=before)context.fail("Protector did not move toward child");else context.succeed();});
  });
 }
 @GameTest public void deadProtectorIsReleasedAndReplacementAssigned(GameTestHelper context) {
  Villager parentA=context.spawn(EntityTypes.VILLAGER,1,1,1), parentB=context.spawn(EntityTypes.VILLAGER,4,1,1), child=context.spawn(EntityTypes.VILLAGER,7,1,1);
  child.setBaby(true);
  context.runAtTickTime(1,()->{
   FamilyManager f=FamilyManager.get(context.getLevel().getServer());
   f.addParentChild(parentA.getUUID(),child.getUUID());
   f.addParentChild(parentB.getUUID(),child.getUUID());
   TheWorldRemembers.processWorld(context.getLevel());
   FamilyProtectionManager p=FamilyProtectionManager.get(context.getLevel().getServer());
   java.util.UUID protector=p.protectorOf(child.getUUID());
   if(protector==null){context.fail("First protector was not assigned");return;}
   Villager currentProtector=protector.equals(parentA.getUUID())?parentA:parentB;
   Villager replacement=protector.equals(parentA.getUUID())?parentB:parentA;
   currentProtector.kill(context.getLevel());
   context.runAtTickTime(1,()->{
    if(parentA.getUUID().equals(p.protectorOf(child.getUUID()))){context.fail("Dead protector was not released");return;}
    TheWorldRemembers.processWorld(context.getLevel());
    if(!replacement.getUUID().equals(p.protectorOf(child.getUUID()))){context.fail("Replacement protector was not assigned");return;}
    context.succeed();
   });
  });
 }
}
