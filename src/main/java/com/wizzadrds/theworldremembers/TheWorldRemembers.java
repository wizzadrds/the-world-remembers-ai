package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.home.*;
import com.wizzadrds.theworldremembers.age.NpcAgeGenerator;
import com.wizzadrds.theworldremembers.age.NpcAgeManager;
import com.wizzadrds.theworldremembers.family.FamilyCourtshipManager;
import com.wizzadrds.theworldremembers.family.FamilyManager;
import com.wizzadrds.theworldremembers.family.FamilyProtectionManager;
import com.wizzadrds.theworldremembers.behavior.NpcBehaviorEngine;
import com.wizzadrds.theworldremembers.behavior.NpcDecision;
import com.wizzadrds.theworldremembers.behavior.NpcActivity;
import com.wizzadrds.theworldremembers.stress.NpcStress;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.inventory.NpcInventoryManager;
import com.wizzadrds.theworldremembers.memory.*;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.relationship.*;
import com.wizzadrds.theworldremembers.stress.NpcStressManager;
import com.wizzadrds.theworldremembers.village.VillageManager;
import com.wizzadrds.theworldremembers.village.VillageHistoryManager;
import com.wizzadrds.theworldremembers.village.VillageResourceManager;
import com.wizzadrds.theworldremembers.village.VillageResources;
import com.wizzadrds.theworldremembers.village.VillageDefenseManager;
import com.wizzadrds.theworldremembers.village.VillageDefense;
import com.wizzadrds.theworldremembers.village.VillageLandmarkManager;
import com.wizzadrds.theworldremembers.village.VillageLandmark;
import com.wizzadrds.theworldremembers.village.VillageMigrationManager;
import com.wizzadrds.theworldremembers.village.VillageEventManager;
import com.wizzadrds.theworldremembers.village.VillageEvent;
import com.wizzadrds.theworldremembers.village.VillageState;
import com.wizzadrds.theworldremembers.village.VillageStorageManager;
import com.wizzadrds.theworldremembers.village.VillageStorage;
import net.minecraft.world.entity.animal.golem.IronGolem;
import java.util.UUID;
import net.minecraft.tags.PoiTypeTags;
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
        NpcBehaviorEngine behavior=new NpcBehaviorEngine();
        MemoryManager memories=MemoryManager.get(world.getServer());
        RelationshipManager relationships=RelationshipManager.get(world.getServer());
        FamilyManager families=FamilyManager.get(world.getServer());
        FamilyCourtshipManager courtship=FamilyCourtshipManager.get(world.getServer());
        FamilyProtectionManager protection=FamilyProtectionManager.get(world.getServer());
        VillageManager villages=VillageManager.get(world.getServer());
        VillageHistoryManager villageHistory=VillageHistoryManager.get(world.getServer());
        VillageResourceManager villageResources=VillageResourceManager.get(world.getServer());
        VillageDefenseManager villageDefense=VillageDefenseManager.get(world.getServer());
        VillageLandmarkManager landmarks=VillageLandmarkManager.get(world.getServer());
        VillageMigrationManager migrations=VillageMigrationManager.get(world.getServer());
        VillageEventManager villageEvents=VillageEventManager.get(world.getServer());
        VillageStorageManager villageStorage=VillageStorageManager.get(world.getServer());

        observeVillages(world, villages, villageHistory, villageResources, villageDefense, landmarks, migrations, villageEvents, villageStorage);

        for(Villager villager:world.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),villager -> villager.isAlive()&&!villager.isRemoved())) {
            ages.assignIfAbsent(villager.getUUID(), villager.isBaby() ? NpcAgeGenerator.generateChildAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())) : NpcAgeGenerator.generateAdultAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())));
            if (villager.isBaby() && !families.hasParents(villager.getUUID())) linkBabyToNearbyParents(world, villager, families, memories);
            if (!villager.isBaby() && ages.get(villager.getUUID()).isAdult() && !families.hasSpouse(villager.getUUID())) processCourtship(world, villager, families, courtship, memories, ages);
            if (!villager.isBaby()) maintainFamilyProtection(villager, families, protection);
            NpcHome home=homes.get(villager.getUUID());
            if(home==null) {
                BlockPos pos=villager.blockPosition();
                home=homes.assignIfAbsent(villager.getUUID(),pos,null,pos);
            }
            synchronizeFamilyHome(villager, families, homes);
            home=homes.get(villager.getUUID());
            applyFamilyProtectionBehavior(world, villager, families, protection, homes, stress);
            applyLiveSocialBehavior(world, villager, relationships, stress, behavior, homes);
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

        for (Villager villager : world.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                v -> v.isAlive() && !v.isRemoved())) {
            synchronizeFamilyHome(villager, families, homes);
        }
    }
    private static void applyLiveSocialBehavior(ServerLevel world, Villager villager, RelationshipManager relationships, NpcStressManager stress, NpcBehaviorEngine behavior, NpcHomeManager homes) {
        if (!villager.getNavigation().isDone() && !villager.isTrading()) return;
        for (ServerPlayer player : world.getEntitiesOfClass(ServerPlayer.class, villager.getBoundingBox().inflate(12), p -> p.isAlive())) {
            Relationship relationship=relationships.get(villager.getUUID(),player.getUUID());
            if(relationship==null) continue;
            NpcActivity activity=villager.isSleeping()?NpcActivity.SLEEPING:(villager.isTrading()?NpcActivity.TRADING:NpcActivity.IDLE);
            NpcDecision decision=behavior.decide(activity,relationship,PersonalityGenerator.generate(villager.getUUID()),new NpcStress(stress.value(villager.getUUID())));
            if(decision==NpcDecision.FOLLOW && relationship.trust()>=30) villager.getNavigation().moveTo(player,1.0);
            else if(decision==NpcDecision.LEAVE) {
                NpcHome home=homes.get(villager.getUUID());
                if(home!=null) villager.getNavigation().moveTo(home.homePos().getX(),home.homePos().getY(),home.homePos().getZ(),1.0);
            }
            break;
        }
    }

    private static void maintainFamilyProtection(Villager villager, FamilyManager families, FamilyProtectionManager protection) {
        for (java.util.UUID child : families.childrenOf(villager.getUUID())) {
            if (protection.protectorOf(child) == null) protection.protect(villager.getUUID(), child);
        }
    }

    private static void synchronizeFamilyHome(Villager villager, FamilyManager families, NpcHomeManager homes) {
        java.util.UUID anchor = families.spouseOf(villager.getUUID());
        if (anchor == null) {
            var parents = families.parentsOf(villager.getUUID());
            if (!parents.isEmpty()) anchor = parents.get(0);
        }
        if (anchor == null) {
            var children = families.childrenOf(villager.getUUID());
            if (!children.isEmpty()) anchor = children.get(0);
        }
        if (anchor != null && homes.hasHome(anchor)) homes.assignFamilyHome(villager.getUUID(), anchor);
    }

    private static void applyFamilyProtectionBehavior(ServerLevel world, Villager villager, FamilyManager families,
                                                       FamilyProtectionManager protection, NpcHomeManager homes,
                                                       NpcStressManager stress) {
        java.util.List<java.util.UUID> children = families.childrenOf(villager.getUUID());
        if (children.isEmpty()) return;
        NpcBehaviorEngine engine = new NpcBehaviorEngine();
        NpcStress npcStress = new NpcStress(stress.value(villager.getUUID()));
        for (java.util.UUID childId : children) {
            if (!villager.getUUID().equals(protection.protectorOf(childId))) continue;
            if (!(world.getEntity(childId) instanceof Villager child) || !child.isAlive()) continue;
            boolean dangerPresent = !world.getEntitiesOfClass(LivingEntity.class, child.getBoundingBox().inflate(8),
                entity -> entity.isAlive() && entity instanceof net.minecraft.world.entity.monster.Monster).isEmpty();
            NpcDecision decision = engine.decideFamilyResponse(true, dangerPresent, npcStress);
            if (decision == NpcDecision.FOLLOW || decision == NpcDecision.CALL_FOR_HELP) {
                villager.getNavigation().moveTo(child, decision == NpcDecision.CALL_FOR_HELP ? 1.25 : 1.0);
            } else if (decision == NpcDecision.RETURN_HOME) {
                NpcHome home = homes.get(villager.getUUID());
                if (home != null) villager.getNavigation().moveTo(home.homePos().getX(), home.homePos().getY(), home.homePos().getZ(), 1.0);
            }
        }
    }

    private static void linkBabyToNearbyParents(ServerLevel world, Villager child, com.wizzadrds.theworldremembers.family.FamilyManager families, MemoryManager memories) {
        java.util.List<Villager> adults=world.getEntitiesOfClass(Villager.class, child.getBoundingBox().inflate(8), v -> v.isAlive() && !v.isBaby() && !v.getUUID().equals(child.getUUID()));
        if(adults.size()!=2) return;
        families.addParentChild(adults.get(0).getUUID(), child.getUUID());
        families.addParentChild(adults.get(1).getUUID(), child.getUUID());
        if (families.parentsOf(child.getUUID()).size() == 2) {
            long time=world.getGameTime();
            memories.rememberEvent(child.getUUID(), adults.get(0).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            memories.rememberEvent(child.getUUID(), adults.get(1).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            memories.inheritFamilyHistory(adults.get(0).getUUID(), child.getUUID(), time);
            memories.inheritFamilyHistory(adults.get(1).getUUID(), child.getUUID(), time);
            families.linkSiblingsFromSharedParent(child.getUUID());
        }
    }
    private static void processCourtship(ServerLevel world, Villager villager, FamilyManager families, FamilyCourtshipManager courtship, MemoryManager memories, NpcAgeManager ages) {
        java.util.List<Villager> candidates = world.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(4),
            other -> other.isAlive() && !other.isBaby() && !other.getUUID().equals(villager.getUUID())
                && ages.get(other.getUUID()) != null && ages.get(other.getUUID()).isAdult()
                && !families.hasSpouse(other.getUUID()) && !families.areRelated(villager.getUUID(), other.getUUID())
                && marriageCompatible(villager, other, ages));
        if (candidates.isEmpty()) return;
        Villager partner = candidates.stream().min(java.util.Comparator.comparingDouble(villager::distanceToSqr)).orElse(null);
        if (partner == null || villager.getUUID().compareTo(partner.getUUID()) > 0) return;
        int progress = courtship.advance(villager.getUUID(), partner.getUUID(), TICK_INTERVAL);
        if (progress < 1200) return;
        if (!families.addSpouses(villager.getUUID(), partner.getUUID())) { courtship.clear(villager.getUUID(), partner.getUUID()); return; }
        long time = world.getGameTime();
        memories.rememberEvent(villager.getUUID(), partner.getUUID(), MemoryEventType.NPC_MARRIED, time, MemoryImportance.IMPORTANT);
        memories.rememberEvent(partner.getUUID(), villager.getUUID(), MemoryEventType.NPC_MARRIED, time, MemoryImportance.IMPORTANT);
        courtship.clear(villager.getUUID(), partner.getUUID());
    }

    private static boolean marriageCompatible(Villager first, Villager second, NpcAgeManager ages) {
        var firstAge = ages.get(first.getUUID());
        var secondAge = ages.get(second.getUUID());
        if (firstAge == null || secondAge == null || Math.abs(firstAge.years() - secondAge.years()) > 12) return false;
        var firstPersonality = PersonalityGenerator.generate(first.getUUID());
        var secondPersonality = PersonalityGenerator.generate(second.getUUID());
        boolean firstInterested = firstPersonality.strength(PersonalityTrait.SOCIAL) >= 50 || firstPersonality.strength(PersonalityTrait.FAMILY_ORIENTED) >= 50;
        boolean secondInterested = secondPersonality.strength(PersonalityTrait.SOCIAL) >= 50 || secondPersonality.strength(PersonalityTrait.FAMILY_ORIENTED) >= 50;
        return firstInterested && secondInterested;
    }
    private static void handleDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel world)) return;
        VillageManager villages=VillageManager.get(world.getServer());
        VillageEventManager villageEvents=VillageEventManager.get(world.getServer());
        VillageHistoryManager villageHistory=VillageHistoryManager.get(world.getServer());
        for(var vs:villages.all()) if(vs.center().distSqr(entity.blockPosition())<=32*32){
            String type=entity instanceof IronGolem ? "golem_died" : entity instanceof Villager ? "npc_died" : null;
            if(type!=null){ villageEvents.record(vs.villageId(),new VillageEvent(type,world.getGameTime(),entity.getUUID(),entity.blockPosition())); villageHistory.recordImportantEvent(vs.villageId()); }
            break;
        }
        if(entity instanceof IronGolem) return;
        if (!(entity instanceof Villager villager)) return;
        var families=FamilyManager.get(world.getServer());
        var memories=MemoryManager.get(world.getServer());
        var protection=FamilyProtectionManager.get(world.getServer());
        protection.clearProtector(villager.getUUID());
        var inventories=NpcInventoryManager.get(world.getServer());
        java.util.List<java.util.UUID> related = families.getRelations(villager.getUUID()).stream()
            .map(r -> r.npcId().equals(villager.getUUID()) ? r.relatedNpcId() : r.npcId()).distinct().toList();
        for(java.util.UUID id : related) {
            memories.rememberEvent(id, villager.getUUID(), MemoryEventType.NPC_DIED, world.getGameTime(), MemoryImportance.IMPORTANT);
            memories.rememberEvent(id, villager.getUUID(), MemoryEventType.NPC_FAMILY_LOST, world.getGameTime(), MemoryImportance.IMPORTANT);
        }
        java.util.UUID heir = families.childrenOf(villager.getUUID()).stream().findFirst()
            .orElseGet(() -> families.spouseOf(villager.getUUID()));
        if (heir != null) {
            int inherited = inventories.inheritImportantItems(villager.getUUID(), heir);
            if (inherited > 0) {
                memories.rememberEvent(heir, villager.getUUID(), MemoryEventType.NPC_INHERITED_ITEM, world.getGameTime(), MemoryImportance.HISTORICAL);
            }
        }
    }
    private static void observeVillages(ServerLevel world, VillageManager villages, VillageHistoryManager history, VillageResourceManager resources, VillageDefenseManager defense, VillageLandmarkManager landmarks, VillageMigrationManager migrations, VillageEventManager villageEvents, VillageStorageManager villageStorage) {
        java.util.Map<Long, java.util.List<Villager>> clusters = new java.util.HashMap<>();
        for (Villager v : world.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),v -> v.isAlive()&&!v.isRemoved())) {
            long key=(((long)(v.blockPosition().getX()>>5))<<32)^((v.blockPosition().getZ()>>5)&0xffffffffL);
            clusters.computeIfAbsent(key,ignored->new java.util.ArrayList<>()).add(v);
        }
        java.util.Set<java.util.UUID> claimed=new java.util.HashSet<>();
        for(var entry:clusters.entrySet()){
            var members=entry.getValue(); if(members.isEmpty()) continue;
            long sx=0,sz=0; for(var v:members){sx+=v.blockPosition().getX();sz+=v.blockPosition().getZ();}
            BlockPos center=new BlockPos((int)(sx/members.size()),members.get(0).blockPosition().getY(),(int)(sz/members.size()));
            VillageState previous=villages.findNearest(center,claimed);
            BlockPos previousCenter=previous==null?null:previous.center();
            VillageState state=villages.observeNearest(center,members.size(),world.getGameTime(),claimed);
            UUID villageId=state.villageId();
            history.observe(villageId,members.size(),world.getGameTime());
            if(previousCenter!=null&&previousCenter.distSqr(center)>32*32){
                migrations.record(new com.wizzadrds.theworldremembers.village.VillageMigration(villageId,previousCenter,center,world.getGameTime(),members.size()));
                villageEvents.record(villageId,new VillageEvent("migration",world.getGameTime(),villageId,center));
            }
            int food=members.stream().mapToInt(v->v.getInventory().countItem(Items.BREAD)).sum();
            int containers=0,occupied=0,capacity=0; for(BlockPos p:BlockPos.betweenClosed(center.offset(-16,-4,-16),center.offset(16,8,16))){var be=world.getBlockEntity(p);if(be instanceof net.minecraft.world.Container container){containers++;capacity+=container.getContainerSize();for(int slot=0;slot<container.getContainerSize();slot++)if(!container.getItem(slot).isEmpty())occupied++;}} villageStorage.observe(villageId,new VillageStorage(containers,occupied,capacity));
            resources.observe(villageId,new VillageResources(food,0,occupied,capacity));
            int golems=world.getEntitiesOfClass(IronGolem.class,new net.minecraft.world.phys.AABB(center).inflate(32),g->g.isAlive()).size();
            defense.observe(villageId,new VillageDefense(golems,0,0));
            for(var pos:world.getPoiManager().findAllWithType(type->type.is(PoiTypeTags.VILLAGE),pos->true,center,32,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).map(pair->pair.getSecond()).toList()) landmarks.add(villageId,new VillageLandmark("village_poi",pos,world.getGameTime()));
        }
    }
    private static boolean hasRecentIntrusion(MemoryManager memories,Villager villager,ServerPlayer player,long gameTime){
        return memories.findMostRecentMemory(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_ENTERED_NPC_HOME).map(m->gameTime-m.gameTime()<INTRUSION_COOLDOWN).orElse(false);
    }
}
