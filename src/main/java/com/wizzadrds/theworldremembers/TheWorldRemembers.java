package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.home.HomeAccess;
import com.wizzadrds.theworldremembers.home.HomeAccessPolicy;
import com.wizzadrds.theworldremembers.home.NpcHome;
import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.memory.Memory;
import com.wizzadrds.theworldremembers.memory.MemoryEvent;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.memory.MemoryImportance;
import com.wizzadrds.theworldremembers.memory.MemoryManager;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.relationship.Relationship;
import com.wizzadrds.theworldremembers.relationship.RelationshipManager;
import com.wizzadrds.theworldremembers.stress.NpcStressManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheWorldRemembers implements ModInitializer {
    public static final String MOD_ID = "the_world_remembers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int TICK_INTERVAL = 20;
    private static final int INTRUSION_COOLDOWN = 200;
    private static final double HOME_RADIUS = 3.5;

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !(entity instanceof VillagerEntity villager)) {
                return ActionResult.PASS;
            }

            ItemStack held = serverPlayer.getStackInHand(hand);
            if (!held.isOf(Items.BREAD)) return ActionResult.PASS;

            MemoryManager memories = MemoryManager.get(serverPlayer.getServer());
            var previous = memories.findMostRecentMemory(
                    villager.getUuid(), serverPlayer.getUuid(), MemoryEventType.PLAYER_GAVE_BREAD);

            if (previous.isPresent()) {
                serverPlayer.sendMessage(
                        Text.literal(villager.getName().getString() + " remembers you: you gave me bread."),
                        false
                );
                return ActionResult.PASS;
            }

            Memory memory = memories.rememberBreadGift(serverPlayer, villager);
            RelationshipManager.get(serverPlayer.getServer()).apply(toEvent(memory));
            serverPlayer.sendMessage(
                    Text.literal(villager.getName().getString() + " will remember this."),
                    false
            );
            return ActionResult.PASS;
        });

        ServerTickEvents.END_WORLD_TICK.register(TheWorldRemembers::tickWorld);
        LOGGER.info("The World Remembers v0.4.0-alpha initialized.");
    }

    private static MemoryEvent toEvent(Memory memory) {
        return new MemoryEvent(
                memory.npcId(), memory.playerId(), memory.type(),
                memory.gameTime(), memory.importance()
        );
    }

    private static void tickWorld(ServerWorld world) {
        if (world.getTime() % TICK_INTERVAL != 0) return;

        NpcHomeManager homes = NpcHomeManager.get(world);
        NpcStressManager stress = NpcStressManager.get(world);
        MemoryManager memories = MemoryManager.get(world.getServer());
        RelationshipManager relationships = RelationshipManager.get(world.getServer());

        for (VillagerEntity villager : world.getEntitiesByClass(
                VillagerEntity.class,
                villager -> villager.isAlive() && !villager.isRemoved(),
                villager -> true)) {

            NpcHome home = homes.get(villager.getUuid());
            if (home == null) {
                BlockPos bed = villager.getSleepingPosition().orElse(null);
                BlockPos homePos = bed != null ? bed : villager.getBlockPos();
                home = homes.assignIfAbsent(villager.getUuid(), homePos, bed, homePos);
            }

            NpcStressManager finalStress = stress;
            NpcHome finalHome = home;
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.getBlockPos().isWithinDistance(finalHome.entrancePos() != null
                        ? finalHome.entrancePos() : finalHome.homePos(), HOME_RADIUS)) {
                    continue;
                }

                Relationship relationship = relationships.getOrCreate(
                        villager.getUuid(), player.getUuid());

                HomeAccess access = HomeAccessPolicy.evaluate(
                        relationship,
                        PersonalityGenerator.generate(villager.getUuid()),
                        false
                );

                if (access == HomeAccess.DENIED
                        && !hasRecentIntrusion(memories, villager, player, world.getTime())) {
                    finalStress.increase(villager.getUuid(), 3);
                    Memory memory = memories.rememberEvent(
                            villager.getUuid(), player.getUuid(),
                            MemoryEventType.PLAYER_ENTERED_NPC_HOME,
                            world.getTime(), MemoryImportance.IMPORTANT
                    );
                    relationships.apply(toEvent(memory));
                    player.sendMessage(
                            Text.literal(villager.getName().getString()
                                    + " is upset that you entered their home."),
                            false
                    );
                } else if (access == HomeAccess.ALLOWED && finalStress.value(villager.getUuid()) > 0) {
                    finalStress.recover(villager.getUuid(), 1);
                }
            }

            // Calm villagers slowly recover when they are not under immediate intrusion.
            if (world.getTime() % 200 == 0
                    && !world.getEntitiesByClass(
                    ServerPlayerEntity.class,
                    villager.getBoundingBox().expand(8.0),
                    player -> true).stream().findAny().isPresent()) {
                stress.recover(villager.getUuid(), 1);
            }
        }
    }

    private static boolean hasRecentIntrusion(
            MemoryManager memories,
            VillagerEntity villager,
            ServerPlayerEntity player,
            long gameTime) {
        return memories.findMostRecentMemory(
                        villager.getUuid(),
                        player.getUuid(),
                        MemoryEventType.PLAYER_ENTERED_NPC_HOME)
                .map(memory -> gameTime - memory.gameTime() < INTRUSION_COOLDOWN)
                .orElse(false);
    }
}
