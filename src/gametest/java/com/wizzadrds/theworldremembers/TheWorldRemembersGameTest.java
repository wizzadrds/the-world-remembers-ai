package com.wizzadrds.theworldremembers;

import java.lang.reflect.Method;
import java.util.UUID;

import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.age.NpcAgeManager;
import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.family.FamilyRelation;
import com.wizzadrds.theworldremembers.family.FamilyRelationType;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.memory.MemoryManager;
import com.wizzadrds.theworldremembers.memory.MemoryOrigin;
import com.wizzadrds.theworldremembers.inventory.NpcInventoryManager;
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
            int before = NpcAgeManager.get(context.getLevel().getServer()).get(villager.getUUID()).years();
            NpcAgeManager.get(context.getLevel().getServer()).advanceIfDue(villager.getUUID(), 168_000L);
            if (NpcAgeManager.get(context.getLevel().getServer()).get(villager.getUUID()).years() != before + 1) { context.fail("Persistent age did not advance by one year"); return; }
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

    @GameTest
    public void liveFamilyLinksAndDeathMemory(GameTestHelper context) {
        Villager parentA = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager parentB = context.spawn(EntityTypes.VILLAGER, 4, 1, 2);
        Villager child = context.spawn(EntityTypes.VILLAGER, 3, 1, 3);
        child.setBaby(true);
        context.runAtTickTime(1, () -> {
            MemoryManager memories = MemoryManager.get(context.getLevel().getServer());
            memories.rememberEvent(parentA.getUUID(), parentB.getUUID(), MemoryEventType.NPC_MARRIED,
                context.getLevel().getGameTime(), com.wizzadrds.theworldremembers.memory.MemoryImportance.IMPORTANT);
            TheWorldRemembers.processWorld(context.getLevel());
            FamilyManager families=FamilyManager.get(context.getLevel().getServer());
            if (families.parentsOf(child.getUUID()).size()!=2) { context.fail("Live baby was not linked to exactly two nearby parents"); return; }
            if (memories.memoriesOf(child.getUUID()).stream().noneMatch(m -> m.origin() == MemoryOrigin.INHERITED && m.type() == MemoryEventType.NPC_MARRIED)) {
                context.fail("Child did not inherit an important family memory");
                return;
            }
            parentA.kill(context.getLevel());
            context.runAtTickTime(1, () -> {
                if (MemoryManager.get(context.getLevel().getServer()).findMostRecentMemory(child.getUUID(), parentA.getUUID(), MemoryEventType.NPC_DIED).isEmpty()) { context.fail("Family member did not remember death"); return; }
                NpcInventoryManager inventories = NpcInventoryManager.get(context.getLevel().getServer());
                inventories.transferIn(parentA.getUUID(), "minecraft:diamond", 1);
                parentA.kill(context.getLevel());
                if (inventories.count(child.getUUID(), "minecraft:diamond") != 1) { context.fail("Important possession was not inherited"); return; }
                if (MemoryManager.get(context.getLevel().getServer()).findMostRecentMemory(child.getUUID(), parentA.getUUID(), MemoryEventType.NPC_INHERITED_ITEM).isEmpty()) { context.fail("Inheritance memory was not recorded"); return; }
                context.succeed();
            });
        });
    }

    @GameTest
    public void liveVillagersCanFormPersistentMarriage(GameTestHelper context) {
        Villager first = context.spawn(EntityTypes.VILLAGER, 2, 1, 2);
        Villager second = context.spawn(EntityTypes.VILLAGER, 3, 1, 2);
        first.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        second.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        NpcAgeManager ages = NpcAgeManager.get(context.getLevel().getServer());
        ages.assignIfAbsent(first.getUUID(), 24);
        ages.assignIfAbsent(second.getUUID(), 26);

        context.runAtTickTime(1, () -> {
            for (int i = 0; i < 60; i++) {
                TheWorldRemembers.processWorld(context.getLevel());
            }
            FamilyManager families = FamilyManager.get(context.getLevel().getServer());
            if (!families.hasSpouse(first.getUUID()) || !families.hasSpouse(second.getUUID())) {
                context.fail("Compatible adult villagers did not form a persistent spouse relation");
                return;
            }
            if (MemoryManager.get(context.getLevel().getServer())
                    .findMostRecentMemory(first.getUUID(), second.getUUID(), MemoryEventType.NPC_MARRIED).isEmpty()) {
                context.fail("Marriage memory was not recorded for the first spouse");
                return;
            }
            context.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
