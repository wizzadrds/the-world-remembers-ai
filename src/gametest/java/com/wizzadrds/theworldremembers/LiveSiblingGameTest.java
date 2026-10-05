package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.family.FamilyManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

public final class LiveSiblingGameTest {
 @GameTest public void newbornLinksToExistingSiblings(GameTestHelper context) {
  Villager parentA=context.spawn(EntityTypes.VILLAGER,1,1,1), parentB=context.spawn(EntityTypes.VILLAGER,5,1,1);
  Villager olderChild=context.spawn(EntityTypes.VILLAGER,2,1,4), newborn=context.spawn(EntityTypes.VILLAGER,4,1,4);
  olderChild.setBaby(true); newborn.setBaby(true);
  context.runAtTickTime(1,()->{
   FamilyManager f=FamilyManager.get(context.getLevel().getServer());
   if(!f.addParentChild(parentA.getUUID(),olderChild.getUUID())||!f.addParentChild(parentB.getUUID(),olderChild.getUUID())){context.fail("Could not establish existing child's parents");return;}
   TheWorldRemembers.processWorld(context.getLevel());
   if(f.parentsOf(newborn.getUUID()).size()!=2){context.fail("Newborn did not receive two parents");return;}
   if(!f.siblingsOf(newborn.getUUID()).contains(olderChild.getUUID())||!f.siblingsOf(olderChild.getUUID()).contains(newborn.getUUID())){context.fail("Sibling link was not symmetric");return;}
   context.succeed();
  });
 }
}