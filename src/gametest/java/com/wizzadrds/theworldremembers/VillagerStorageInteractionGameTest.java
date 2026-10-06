package com.wizzadrds.theworldremembers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class VillagerStorageInteractionGameTest {
    @GameTest
    public void villagerCanDepositPickedUpFoodIntoHomeChest(GameTestHelper c) {
        c.setBlock(3, 1, 3, Blocks.CHEST);
        var villager = c.spawn(EntityTypes.VILLAGER, 3, 1, 4);
        var dropped = new net.minecraft.world.entity.item.ItemEntity(
                c.getLevel(), 3.5, 1.0, 3.5, new ItemStack(Items.BREAD, 3));
        dropped.setPickUpDelay(0);
        c.getLevel().addFreshEntity(dropped);

        c.runAtTickTime(2, () -> {
            TheWorldRemembers.processWorld(c.getLevel());
            c.runAtTickTime(2, () -> {
                TheWorldRemembers.processWorld(c.getLevel());
                var chest = c.getLevel().getBlockEntity(new net.minecraft.core.BlockPos(3, 1, 3));
                if (!(chest instanceof net.minecraft.world.Container container)) {
                    c.fail("Home chest was not available as a container");
                    return;
                }
                if (villager.getInventory().countItem(Items.BREAD) > 0) {
                    c.fail("Villager still held bread after reaching home storage");
                    return;
                }
                if (container.countItem(Items.BREAD) < 3) {
                    c.fail("Picked-up bread was not deposited into the home chest");
                    return;
                }
                c.succeed();
            });
        });
    }
}
