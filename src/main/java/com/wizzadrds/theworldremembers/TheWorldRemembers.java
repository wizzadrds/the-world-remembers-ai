package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.home.*;
import com.wizzadrds.theworldremembers.age.NpcAgeGenerator;
import com.wizzadrds.theworldremembers.age.NpcAgeManager;
import com.wizzadrds.theworldremembers.memory.*;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.relationship.*;
import com.wizzadrds.theworldremembers.stress.NpcStressManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheWorldRemembers implements ModInitializer {
    public static final String MOD_ID="the_world_remembers";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    private static final int TICK_INTERVAL=20;
    private static final int INTRUSION_COOLDOWN=200;
    private static final double HOME_RADIUS=3.5;

    @Override public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(entity instanceof Villager villager)) return InteractionResult.PASS;
            if (!serverPlayer.getItemInHand(hand).is(Items.BREAD)) return InteractionResult.PASS;
            MemoryManager memories=MemoryManager.get(serverPlayer.level().getServer());
            if (memories.findMostRecentMemory(villager.getUUID(),serverPlayer.getUUID(),MemoryEventType.PLAYER_GAVE_BREAD).isPresent()) {
                serverPlayer.sendSystemMessage(Component.literal(villager.getName().getString()+" remembers you: you gave me bread."));
                return InteractionResult.PASS;
            }
            Memory memory=memories.rememberBreadGift(serverPlayer,villager);
            RelationshipManager.get(serverPlayer.level().getServer()).apply(new MemoryEvent(memory.npcId(),memory.playerId(),memory.type(),memory.gameTime(),memory.importance()));
            serverPlayer.sendSystemMessage(Component.literal(villager.getName().getString()+" will remember this."));
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_LEVEL_TICK.register(TheWorldRemembers::tickWorld);
        ServerLivingEntityEvents.AFTER_DEATH.register(TheWorldRemembers::handleDeath);
        LOGGER.info("The World Remembers v0.4.0-alpha initialized.");
    }

    static void tickWorld(ServerLevel world) {
        if (world.getGameTime() % TICK_INTERVAL != 0) return;
        processWorld(world);
    }

    static void processWorld(ServerLevel world) {
        NpcHomeManager homes=NpcHomeManager.get(world);
        NpcAgeManager ages=NpcAgeManager.get(world.getServer());
        NpcStressManager stress=NpcStressManager.get(world);
        MemoryManager memories=MemoryManager.get(world.getServer());
        RelationshipManager relationships=RelationshipManager.get(world.getServer());
        com.wizzadrds.theworldremembers.family.FamilyManager families=com.wizzadrds.theworldremembers.family.FamilyManager.get(world.getServer());

        for(Villager villager:world.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),villager -> villager.isAlive()&&!villager.isRemoved())) {
            ages.assignIfAbsent(villager.getUUID(), villager.isBaby() ? NpcAgeGenerator.generateChildAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())) : NpcAgeGenerator.generateAdultAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())));
            if (villager.isBaby() && !families.hasParents(villager.getUUID())) linkBabyToNearbyParents(world, villager, families, memories);
            NpcHome home=homes.get(villager.getUUID());
            if(home==null) {
                BlockPos pos=villager.blockPosition();
                home=homes.assignIfAbsent(villager.getUUID(),pos,null,pos);
            }
            BlockPos entrance=home.entrancePos()!=null?home.entrancePos():home.homePos();
            for(ServerPlayer player:world.players()) {
                if(player.blockPosition().distSqr(entrance)>HOME_RADIUS*HOME_RADIUS) continue;
                Relationship relationship=relationships.get(villager.getUUID(),player.getUUID());
                if(relationship==null) continue;
                HomeAccess access=HomeAccessPolicy.evaluate(relationship,PersonalityGenerator.generate(villager.getUUID()),false);
                if(access==HomeAccess.DENIED&&!hasRecentIntrusion(memories,villager,player,world.getGameTime())) {
                    stress.increase(villager.getUUID(),3);
                    Memory memory=memories.rememberEvent(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_ENTERED_NPC_HOME,world.getGameTime(),MemoryImportance.IMPORTANT);
                    relationships.apply(new MemoryEvent(memory.npcId(),memory.playerId(),memory.type(),memory.gameTime(),memory.importance()));
                    player.sendSystemMessage(Component.literal(villager.getName().getString()+" is upset that you entered their home."));
                } else if(access==HomeAccess.ALLOWED&&stress.value(villager.getUUID())>0) stress.recover(villager.getUUID(),1);
            }
            if(world.getGameTime()%200==0&&!world.getEntitiesOfClass(ServerPlayer.class,villager.getBoundingBox().inflate(8),p->true).iterator().hasNext()) stress.recover(villager.getUUID(),1);
        }
    }
    private static void linkBabyToNearbyParents(ServerLevel world, Villager child, com.wizzadrds.theworldremembers.family.FamilyManager families, MemoryManager memories) {
        java.util.List<Villager> adults=world.getEntitiesOfClass(Villager.class, child.getBoundingBox().inflate(8), v -> v.isAlive() && v.isAdult() && !v.getUUID().equals(child.getUUID()));
        if(adults.size()!=2) return;
        if(families.addParentChild(adults.get(0).getUUID(), child.getUUID()) && families.addParentChild(adults.get(1).getUUID(), child.getUUID())) {
            long time=world.getGameTime();
            memories.rememberEvent(child.getUUID(), adults.get(0).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            memories.rememberEvent(child.getUUID(), adults.get(1).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
        }
    }
    private static void handleDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!(entity instanceof Villager villager) || !(entity.level() instanceof ServerLevel world)) return;
        var families=com.wizzadrds.theworldremembers.family.FamilyManager.get(world.getServer());
        var memories=MemoryManager.get(world.getServer());
        for(java.util.UUID related : families.getRelations(villager.getUUID()).stream().map(r -> r.npcId().equals(villager.getUUID()) ? r.relatedNpcId() : r.npcId()).distinct().toList()) {
            memories.rememberEvent(related, villager.getUUID(), MemoryEventType.NPC_DIED, world.getGameTime(), MemoryImportance.IMPORTANT);
            memories.rememberEvent(related, villager.getUUID(), MemoryEventType.NPC_FAMILY_LOST, world.getGameTime(), MemoryImportance.IMPORTANT);
        }
    }
    private static boolean hasRecentIntrusion(MemoryManager memories,Villager villager,ServerPlayer player,long gameTime){
        return memories.findMostRecentMemory(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_ENTERED_NPC_HOME).map(m->gameTime-m.gameTime()<INTRUSION_COOLDOWN).orElse(false);
    }
}
