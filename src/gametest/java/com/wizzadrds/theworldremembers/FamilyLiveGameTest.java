package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.memory.MemoryManager;
import com.wizzadrds.theworldremembers.memory.MemoryOrigin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

public final class FamilyLiveGameTest {
    @GameTest
    public void newbornLinksToExistingSiblings(GameTestHelper context) {
        Villager parentA = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager parentB = context.spawn(EntityTypes.VILLAGER, 4, 1, 2);
        Villager older = context.spawn(EntityTypes.VILLAGER, 3, 1, 4);
        older.setBaby(true);
        Villager newborn = context.spawn(EntityTypes.VILLAGER, 3, 1, 3);
        newborn.setBaby(true);

        context.runAtTickTime(1, () -> {
            FamilyManager families = FamilyManager.get(context.getLevel().getServer());
            families.addParentChild(parentA.getUUID(), older.getUUID());
            families.addParentChild(parentB.getUUID(), older.getUUID());

            MemoryManager memories = MemoryManager.get(context.getLevel().getServer());
            memories.rememberEvent(parentA.getUUID(), parentB.getUUID(), MemoryEventType.NPC_MARRIED,
                context.getLevel().getGameTime(), com.wizzadrds.theworldremembers.memory.MemoryImportance.IMPORTANT);

            TheWorldRemembers.processWorld(context.getLevel());

            if (families.parentsOf(newborn.getUUID()).size() != 2) {
                context.fail("Newborn was not linked to both nearby parents");
                return;
            }
            if (!families.siblingsOf(newborn.getUUID()).contains(older.getUUID())
                    || !families.siblingsOf(older.getUUID()).contains(newborn.getUUID())) {
                context.fail("Newborn and existing child were not linked as symmetric siblings");
                return;
            }
            long inheritedMarriageCount = memories.memoriesOf(newborn.getUUID()).stream()
                .filter(m -> m.origin() == MemoryOrigin.INHERITED && m.type() == MemoryEventType.NPC_MARRIED)
                .count();
            if (inheritedMarriageCount != 1) {
                context.fail("Newborn did not inherit exactly one direct marriage memory");
                return;
            }
            context.succeed();
        });
    }
}
