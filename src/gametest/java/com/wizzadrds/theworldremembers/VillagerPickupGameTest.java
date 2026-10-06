package com.wizzadrds.theworldremembers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VillagerPickupGameTest {
    @GameTest
    public void villagerCanPickUpDroppedItems(GameTestHelper c) {
        var villager = c.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        var level = c.getLevel();
        var dropped = new ItemEntity(level, 2.5, 1.0, 2.5, new ItemStack(Items.BREAD, 3));
        dropped.setPickUpDelay(0);
        level.addFreshEntity(dropped);

        c.runAtTickTime(1, () -> {
            TheWorldRemembers.processWorld(level);
            if (villager.getInventory().countItem(Items.BREAD) < 3) {
                c.fail("Villager did not pick up dropped bread");
                return;
            }
            if (dropped.isAlive()) {
                c.fail("Dropped item remained after villager pickup");
                return;
            }
            c.succeed();
        });
    }
}
