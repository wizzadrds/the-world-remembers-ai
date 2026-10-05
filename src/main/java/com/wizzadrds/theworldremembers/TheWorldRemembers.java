package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.home.NpcHomeManager;
import com.wizzadrds.theworldremembers.memory.Memory;
import com.wizzadrds.theworldremembers.memory.MemoryEvent;
import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import com.wizzadrds.theworldremembers.memory.MemoryImportance;
import com.wizzadrds.theworldremembers.memory.MemoryManager;
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

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !(entity instanceof VillagerEntity villager)) {
                return ActionResult.PASS;
            }
            ItemStack held = serverPlayer.getStackInHand(hand);
            MemoryManager memories = MemoryManager.get(serverPlayer.getServer());

            if (held.isOf(Items.BREAD)) {
                var previous = memories.findMostRecentMemory(villager.getUuid(), serverPlayer.getUuid());

                if (previous.isPresent()) {
                    serverPlayer.sendMessage(
                            Text.literal(villager.getName().getString()
                                    + " remembers you: " + previous.get().summary()),
                            false
                    );
                } else {
                    Memory memory = memories.rememberBreadGift(serverPlayer, villager);
                    RelationshipManager.get(serverPlayer.getServer()).apply(
                            new MemoryEvent(
                                    memory.npcId(), memory.playerId(), memory.type(),
                                    memory.gameTime(), memory.importance()
                            )
                    );
                    serverPlayer.sendMessage(
                            Text.literal(villager.getName().getString() + " will remember this."),
                            false
                    );
                }
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_WORLD_TICK.register(TheWorldRemembers::tickWorld);
        LOGGER.info("The World Remembers v0.4.0-alpha initialized.");
    }

    private static void tickWorld(ServerWorld world) {
        if (world.getTime() % 20 != 0) return;

        NpcHomeManager homes = NpcHomeManager.get(world);
        NpcStressManager stress = NpcStressManager.get(world);
        MemoryManager memories = MemoryManager.get(world.getServer());
        RelationshipManager relationships = RelationshipManager.get(world.getServer());

        for (VillagerEntity villager : world.getEntitiesByClass(
                VillagerEntity.class,
                villager -> villager.isAlive() && !villager.isRemoved(),
                villager -> true)) {

            BlockPos homePos = villager.getSleepingPosition().orElse(villager.getBlockPos());
            homes.assignIfAbsent(villager.getUuid(), homePos, villager.getSleepingPosition().orElse(null), homePos);

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.getBlockPos().isWithinDistance(homePos, 2.5)) continue;

                var relationship = relationships.get(villager.getUuid(), player.getUuid());
                if (relationship == null) continue;

                var access = com.wizzadrds.theworldremembers.home.HomeAccessPolicy.evaluate(
                        relationship,
                        new com.wizzadrds.theworldremembers.personality.PersonalityProfile(villager.getUuid()),
                        false
                );

                if (access == com.wizzadrds.theworldremembers.home.HomeAccess.DENIED
                        && memories.findMostRecentMemory(
                                villager.getUuid(), player.getUuid(),
                                MemoryEventType.PLAYER_ENTERED_NPC_HOME).map(
                                memory -> world.getTime() - memory.gameTime() < 200).orElse(false) == false) {
                    stress.increase(villager.getUuid(), 3);

                    Memory memory = memories.rememberEvent(
                            villager.getUuid(), player.getUuid(),
                            MemoryEventType.PLAYER_ENTERED_NPC_HOME,
                            world.getTime(), MemoryImportance.IMPORTANT
                    );
                    relationships.apply(new MemoryEvent(
                            memory.npcId(), memory.playerId(), memory.type(),
                            memory.gameTime(), memory.importance()
                    ));
                    player.sendMessage(Text.literal(villager.getName().getString() + " is upset that you entered their home."), false);
                }
            }
        }
    }
}
