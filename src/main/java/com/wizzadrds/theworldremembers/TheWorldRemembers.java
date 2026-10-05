package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.memory.MemoryManager;
import com.wizzadrds.theworldremembers.relationship.RelationshipManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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
                    var memory = memories.rememberBreadGift(serverPlayer, villager);
                    RelationshipManager.get(serverPlayer.getServer()).apply(
                            new com.wizzadrds.theworldremembers.memory.MemoryEvent(
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
        LOGGER.info("The World Remembers v0.2.0-alpha initialized.");
    }
}
