package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.village.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VillageResourceGameTest {
 @GameTest public void villageFoodPressureUsesLiveInventory(GameTestHelper context) {
  var a=context.spawn(EntityTypes.VILLAGER,2,1,2);
  var b=context.spawn(EntityTypes.VILLAGER,4,1,2);
  a.getInventory().addItem(new ItemStack(Items.BREAD,3));
  b.getInventory().addItem(new ItemStack(Items.BREAD,2));
  context.runAtTickTime(1,()->{
   TheWorldRemembers.processWorld(context.getLevel());
   var v=VillageManager.get(context.getLevel().getServer()).all().iterator().next();
   var r=VillageResourceManager.get(context.getLevel().getServer()).get(v.villageId());
   if(r==null||r.food()!=5){context.fail("Live village food was not observed: "+(r==null?"null":r.food()));return;}
   if(r.pressure()<=0){context.fail("Expected non-zero resource pressure with partial food reserve");return;}
   context.succeed();
  });
 }
}
