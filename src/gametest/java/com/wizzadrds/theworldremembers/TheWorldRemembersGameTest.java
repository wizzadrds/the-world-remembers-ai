package com.wizzadrds.theworldremembers;

import java.lang.reflect.Method;

import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
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

        var first = PersonalityGenerator.generate(context.getLevel().getRandom().nextLong() == Long.MIN_VALUE
                ? java.util.UUID.randomUUID()
                : java.util.UUID.randomUUID());
        if (first.strength(PersonalityTrait.SOCIAL) < -100
                || first.strength(PersonalityTrait.SOCIAL) > 100) {
            context.fail("Generated personality escaped valid bounds");
            return;
        }

        context.assertBlockPresent(Blocks.AIR, 0, 0, 0);
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
