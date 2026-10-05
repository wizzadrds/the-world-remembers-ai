package com.wizzadrds.theworldremembers;

import java.lang.reflect.Method;
import java.util.UUID;

import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.age.NpcAgeManager;
import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.family.FamilyRelation;
import com.wizzadrds.theworldremembers.family.FamilyRelationType;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;

public final class TheWorldRemembersGameTest implements CustomTestMethodInvoker {
    @GameTest
    public void modLoadsAndCoreContractsExist(GameTestHelper context) {
        if (!FabricLoader.getInstance().isModLoaded(TheWorldRemembers.MOD_ID)) {
            context.fail("The World Remembers main mod is not loaded");
            return;
        }
        if (MemoryEventType.PLAYER_ENTERED_NPC_HOME == null) {
            context.fail("Home intrusion memory event is missing");
            return;
        }
        var first = PersonalityGenerator.generate(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        if (first.strength(PersonalityTrait.SOCIAL) < -100 || first.strength(PersonalityTrait.SOCIAL) > 100) {
            context.fail("Generated personality escaped valid bounds");
            return;
        }
        context.assertBlockPresent(Blocks.AIR, 0, 0, 0);
        context.succeed();
    }

    @GameTest
    public void villagerReceivesPersistentHome(GameTestHelper context) {
        Villager villager = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        context.runAtTickTime(1, () -> {
            TheWorldRemembers.processWorld(context.getLevel());
            if (!NpcHomeManager.get(context.getLevel()).hasHome(villager.getUUID())) {
                context.fail("Live villager did not receive persistent home state");
                return;
            }
            if (!NpcAgeManager.get(context.getLevel().getServer()).hasAge(villager.getUUID())) {
                context.fail("Live villager did not receive persistent age state");
                return;
            }
            context.succeed();
        });
    }

    @GameTest
    public void familyRelationsPersist(GameTestHelper context) {
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        FamilyManager families = FamilyManager.get(context.getLevel().getServer());
        if (!families.addParentChild(parent, child)) { context.fail("Parent-child relation was not added"); return; }
        if (!families.areRelated(parent, child)) { context.fail("Family relation cannot be queried"); return; }
        if (families.getRelations(parent).size() != 2) { context.fail("Symmetric family relation was not persisted"); return; }
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
