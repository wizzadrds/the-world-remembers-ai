package com.wizzadrds.theworldremembers;

import java.lang.reflect.Method;

import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.passive.Villager;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

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
        if (first.strength(PersonalityTrait.SOCIAL) < -100
                || first.strength(PersonalityTrait.SOCIAL) > 100) {
            context.fail("Generated personality escaped valid bounds");
            return;
        }

        context.assertBlockPresent(Blocks.AIR, 0, 0, 0);
        context.succeed();
    }

    @GameTest(timeoutTicks = 60)
    public void villagerReceivesPersistentHome(GameTestHelper context) {
        Villager villager = context.spawn(EntityType.VILLAGER, 2, 1, 2);
        context.runAtTickTime(21, () -> {
            if (!NpcHomeManager.get(context.getLevel()).hasHome(villager.getUuid())) {
                context.fail("Live villager did not receive persistent home state");
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
