package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.inventory.NpcInventoryManager;
import com.wizzadrds.theworldremembers.memory.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SpouseInheritanceGameTest {
    @GameTest
    public void directSpouseReceivesImportantPossession(GameTestHelper context) {
        Villager deceased = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager spouse = context.spawn(EntityTypes.VILLAGER, 3, 1, 2);
        context.runAtTickTime(1, () -> {
            FamilyManager f = FamilyManager.get(context.getLevel().getServer());
            f.addSpouses(deceased.getUUID(), spouse.getUUID());

            NpcInventoryManager inv = NpcInventoryManager.get(context.getLevel().getServer());
            inv.transferIn(deceased.getUUID(), "minecraft:diamond", 1);
            deceased.kill(context.getLevel());

            context.runAtTickTime(1, () -> {
                if (inv.count(spouse.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Spouse did not inherit diamond");
                    return;
                }
                if (spouse.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) != 1) {
                    context.fail("Inherited diamond was not materialized in the live heir inventory");
                    return;
                }

                // Re-run the normal simulation mirror: inherited possessions must survive
                // synchronization instead of disappearing from persistent state.
                TheWorldRemembers.processWorld(context.getLevel());
                if (inv.count(spouse.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Inventory synchronization erased inherited diamond");
                    return;
                }
                if (spouse.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) != 1) {
                    context.fail("Inventory synchronization erased live inherited diamond");
                    return;
                }

                MemoryManager m = MemoryManager.get(context.getLevel().getServer());
                if (m.findMostRecentMemory(
                        spouse.getUUID(), deceased.getUUID(), MemoryEventType.NPC_INHERITED_ITEM).isEmpty()) {
                    context.fail("Inheritance memory missing");
                    return;
                }
                context.succeed();
            });
        });
    }

    @GameTest
    public void fullHeirInventoryPersistsOverflowUntilSpaceOpens(GameTestHelper context) {
        Villager deceased = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager heir = context.spawn(EntityTypes.VILLAGER, 3, 1, 2);
        context.runAtTickTime(1, () -> {
            FamilyManager families = FamilyManager.get(context.getLevel().getServer());
            families.addSpouses(deceased.getUUID(), heir.getUUID());

            var inventory = heir.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                inventory.setItem(slot, new ItemStack(Items.DIRT, 1));
            }

            NpcInventoryManager inventories = NpcInventoryManager.get(context.getLevel().getServer());
            inventories.transferIn(deceased.getUUID(), "minecraft:diamond", 1);
            deceased.kill(context.getLevel());

            context.runAtTickTime(1, () -> {
                if (inventories.count(heir.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Overflow inheritance was lost from persistent inventory state");
                    return;
                }
                if (heir.getInventory().countItem(Items.DIAMOND) != 0) {
                    context.fail("Diamond should remain pending while heir inventory is full");
                    return;
                }

                TheWorldRemembers.processWorld(context.getLevel());
                if (inventories.count(heir.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Normal inventory synchronization dropped pending inheritance");
                    return;
                }
                if (heir.getInventory().countItem(Items.DIAMOND) != 0) {
                    context.fail("Full inventory unexpectedly materialized pending diamond");
                    return;
                }

                inventory.setItem(0, ItemStack.EMPTY);
                TheWorldRemembers.processWorld(context.getLevel());

                if (heir.getInventory().countItem(Items.DIAMOND) != 1) {
                    context.fail("Pending inherited diamond was not materialized after space opened");
                    return;
                }
                if (inventories.count(heir.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Materialized inheritance disappeared from persistent inventory state");
                    return;
                }
                context.succeed();
            });
        });
    }
}
