package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.home.NpcHome;
import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

public final class FamilyHomeGameTest {
    @GameTest
    public void spouseAndChildShareFamilyHome(GameTestHelper context) {
        Villager spouseA = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager spouseB = context.spawn(EntityTypes.VILLAGER, 5, 1, 2);
        Villager child = context.spawn(EntityTypes.VILLAGER, 3, 1, 4);
        child.setBaby(true);

        context.runAtTickTime(1, () -> {
            FamilyManager families = FamilyManager.get(context.getLevel().getServer());
            families.addSpouses(spouseA.getUUID(), spouseB.getUUID());
            families.addParentChild(spouseA.getUUID(), child.getUUID());

            TheWorldRemembers.processWorld(context.getLevel());

            NpcHomeManager homes = NpcHomeManager.get(context.getLevel());
            NpcHome homeA = homes.get(spouseA.getUUID());
            NpcHome homeB = homes.get(spouseB.getUUID());
            NpcHome homeChild = homes.get(child.getUUID());
            if (homeA == null || homeB == null || homeChild == null) {
                context.fail("Family members did not receive persistent homes");
                return;
            }
            if (!homeA.homePos().equals(homeB.homePos())) {
                context.fail("Spouses did not converge on a shared home");
                return;
            }
            if (!homeA.homePos().equals(homeChild.homePos())) {
                context.fail("Child did not share the family home");
                return;
            }
            context.succeed();
        });
    }
}
