package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.inventory.NpcInventoryManager;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.memory.MemoryManager;
import com.wizzadrds.theworldremembers.memory.MemoryImportance;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

public final class SpouseInheritanceGameTest {
    @GameTest
    public void directSpouseReceivesImportantPossession(GameTestHelper context) {
        Villager deceased = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager spouse = context.spawn(EntityTypes.VILLAGER, 3, 1, 2);

        context.runAtTickTime(1, () -> {
            FamilyManager families = FamilyManager.get(context.getLevel().getServer());
            if (!families.addSpouses(deceased.getUUID(), spouse.getUUID())) {
                context.fail("Could not create spouse relation");
                return;
            }

            NpcInventoryManager inventories = NpcInventoryManager.get(context.getLevel().getServer());
            inventories.transferIn(deceased.getUUID(), "minecraft:diamond", 1);
            deceased.kill(context.getLevel());

            context.runAtTickTime(1, () -> {
                if (inventories.count(spouse.getUUID(), "minecraft:diamond") != 1) {
                    context.fail("Direct spouse did not inherit important possession");
                    return;
                }
                MemoryManager memories = MemoryManager.get(context.getLevel().getServer());
                if (memories.findMostRecentMemory(spouse.getUUID(), deceased.getUUID(), MemoryEventType.NPC_INHERITED_ITEM).isEmpty()) {
                    context.fail("Spouse inheritance memory was not recorded");
                    return;
                }
                if (memories.findMostRecentMemory(spouse.getUUID(), deceased.getUUID(), MemoryEventType.NPC_DIED).isEmpty()) {
                    context.fail("Spouse did not remember the death");
                    return;
                }
                context.succeed();
            });
        });
    }
}
