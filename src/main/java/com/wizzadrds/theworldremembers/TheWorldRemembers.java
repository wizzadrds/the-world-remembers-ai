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
import com.wizzadrds.theworldremembers.behavior.NpcActivityManager;
import com.wizzadrds.theworldremembers.stress.NpcFatigueManager;
import com.wizzadrds.theworldremembers.stress.NpcStress;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.inventory.NpcInventoryManager;
import com.wizzadrds.theworldremembers.memory.*;
import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.relationship.*;
import com.wizzadrds.theworldremembers.stress.NpcStressManager;
import com.wizzadrds.theworldremembers.village.VillageManager;
import com.wizzadrds.theworldremembers.dream.DreamManager;
import com.wizzadrds.theworldremembers.chronicle.ChronicleNetworking;
import com.wizzadrds.theworldremembers.rumor.*;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
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
    private static final int TICK_INTERVAL=1;
    /** Maximum number of villagers whose expensive TWR simulation is advanced by one live tick. */
    private static final int VILLAGER_BUDGET_PER_TICK=12;
    /** Global village scans are deliberately much less frequent than individual NPC simulation. */
    private static final int VILLAGE_SCAN_INTERVAL=40;
    /** A village observation processes only a bounded number of village clusters per live tick. */
    private static final int VILLAGE_SCAN_BUDGET=2;
    /** Registry discovery is much less frequent than the per-tick simulation rotation. */
    private static final int VILLAGER_DISCOVERY_INTERVAL=200;
    private static final int SOCIAL_BUDGET_PER_TICK=8;
    private static final int SOCIAL_INTERVAL=10;
    private static final int KNOWLEDGE_DECAY_INTERVAL=200;
    private static final int INTRUSION_COOLDOWN=200;
    private static final java.util.Map<ServerLevel,Integer> VILLAGER_CURSORS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel,Integer> SOCIAL_CURSORS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel,Integer> VILLAGE_CURSORS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel,java.util.List<UUID>> VILLAGER_REGISTRY = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel,SchedulerMetrics> SCHEDULER_METRICS = new java.util.WeakHashMap<>();
    private static final double HOME_RADIUS=3.5;
    private static final NpcBehaviorEngine BEHAVIOR_ENGINE = new NpcBehaviorEngine();

    private static final class SchedulerMetrics {
        long samples;
        long totalNanos;
        long maxNanos;
        long discoveryNanos;
        long socialNanos;
        long villageNanos;
        long villagerNanos;

        void record(long elapsedNanos) {
            samples++;
            totalNanos += elapsedNanos;
            maxNanos = Math.max(maxNanos, elapsedNanos);
        }

        void recordDiscovery(long elapsedNanos) { discoveryNanos += elapsedNanos; }
        void recordSocial(long elapsedNanos) { socialNanos += elapsedNanos; }
        void recordVillage(long elapsedNanos) { villageNanos += elapsedNanos; }
        void recordVillagers(long elapsedNanos) { villagerNanos += elapsedNanos; }

        void reset() {
            samples = 0;
            totalNanos = 0;
            maxNanos = 0;
            discoveryNanos = 0;
            socialNanos = 0;
            villageNanos = 0;
            villagerNanos = 0;
        }
    }

    @Override public void onInitialize() {
        ChronicleNetworking.init();
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
        com.wizzadrds.theworldremembers.voice.VoiceNetworking.init();
        LOGGER.info("The World Remembers v1.0.0 initialized.");
    }

    static void tickWorld(ServerLevel world) {
        // Keep the live server loop cheap: expensive global systems are scheduled independently,
        // and only a bounded number of villagers advance through the full simulation each tick.
        if (world.getGameTime() % TICK_INTERVAL != 0) return;
        long started = System.nanoTime();
        processWorld(world, true);
        long elapsed = System.nanoTime() - started;
        SchedulerMetrics metrics = SCHEDULER_METRICS.computeIfAbsent(world, ignored -> new SchedulerMetrics());
        metrics.record(elapsed);
        if (world.getGameTime() % 200 == 0 && metrics.samples > 0) {
            long averageMicros = metrics.totalNanos / metrics.samples / 1_000L;
            long maxMicros = metrics.maxNanos / 1_000L;
            LOGGER.info("Live simulation scheduler: samples={}, avg={}us, max={}us, discovery={}us, social={}us, village={}us, villagers={}us",
                    metrics.samples, averageMicros, maxMicros,
                    metrics.discoveryNanos / metrics.samples / 1_000L,
                    metrics.socialNanos / metrics.samples / 1_000L,
                    metrics.villageNanos / metrics.samples / 1_000L,
                    metrics.villagerNanos / metrics.samples / 1_000L);
            metrics.reset();
        }
    }

    /** Full processing entry point retained for GameTests and deterministic validation. */
    static void processWorld(ServerLevel world) {
        processWorld(world, false);
    }

    private static void processWorld(ServerLevel world, boolean budgeted) {
        NpcHomeManager homes=NpcHomeManager.get(world);
        NpcAgeManager ages=NpcAgeManager.get(world.getServer());
        NpcStressManager stress=NpcStressManager.get(world);
        NpcFatigueManager fatigue=NpcFatigueManager.get(world.getServer());
        NpcActivityManager activities=NpcActivityManager.get(world.getServer());
        NpcInventoryManager inventories=NpcInventoryManager.get(world.getServer());
        com.wizzadrds.theworldremembers.equipment.NpcEquipmentManager equipment=com.wizzadrds.theworldremembers.equipment.NpcEquipmentManager.get(world.getServer());
        NpcHomeStorageManager homeStorage=NpcHomeStorageManager.get(world.getServer());
        NpcBehaviorEngine behavior=BEHAVIOR_ENGINE;
        MemoryManager memories=MemoryManager.get(world.getServer());
        RelationshipManager relationships=RelationshipManager.get(world.getServer());
        FamilyManager families=FamilyManager.get(world.getServer());
        FamilyCourtshipManager courtship=FamilyCourtshipManager.get(world.getServer());
        FamilyProtectionManager protection=FamilyProtectionManager.get(world.getServer());
        VillageManager villages=VillageManager.get(world.getServer());
        DreamManager dreams=DreamManager.get(world.getServer());
        KnowledgeManager knowledge=KnowledgeManager.get(world.getServer());
        ConversationManager conversations=ConversationManager.get(world.getServer());
        VillageHistoryManager villageHistory=VillageHistoryManager.get(world.getServer());
        VillageResourceManager villageResources=VillageResourceManager.get(world.getServer());
        VillageDefenseManager villageDefense=VillageDefenseManager.get(world.getServer());
        VillageLandmarkManager landmarks=VillageLandmarkManager.get(world.getServer());
        VillageMigrationManager migrations=VillageMigrationManager.get(world.getServer());
        VillageEventManager villageEvents=VillageEventManager.get(world.getServer());
        VillageStorageManager villageStorage=VillageStorageManager.get(world.getServer());

        SchedulerMetrics metrics = budgeted
                ? SCHEDULER_METRICS.computeIfAbsent(world, ignored -> new SchedulerMetrics())
                : null;

        if (budgeted && (world.getGameTime() % VILLAGER_DISCOVERY_INTERVAL == 0 || !VILLAGER_REGISTRY.containsKey(world))) {
            long phaseStarted = System.nanoTime();
            refreshVillagerRegistry(world);
            metrics.recordDiscovery(System.nanoTime() - phaseStarted);
        }

        java.util.List<Villager> loadedVillagers = budgeted
                ? nextBudgetedVillagers(world, VILLAGER_BUDGET_PER_TICK, VILLAGER_CURSORS)
                : new java.util.ArrayList<>(world.getEntitiesOfClass(
                    Villager.class,
                    new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                    villager -> villager.isAlive() && !villager.isRemoved()));
        if (budgeted) {
            if (world.getGameTime() % SOCIAL_INTERVAL == 0) {
                long phaseStarted = System.nanoTime();
                processConversations(world, memories, knowledge, conversations,
                        nextBudgetedVillagers(world, SOCIAL_BUDGET_PER_TICK, SOCIAL_CURSORS));
                metrics.recordSocial(System.nanoTime() - phaseStarted);
            }
            if (world.getGameTime() % VILLAGE_SCAN_INTERVAL == 0) {
                long phaseStarted = System.nanoTime();
                observeVillages(world, villages, villageHistory, villageResources, villageDefense, landmarks, migrations, villageEvents, villageStorage,
                        loadedVillagersFromRegistry(world), VILLAGE_SCAN_BUDGET, VILLAGE_CURSORS);
                metrics.recordVillage(System.nanoTime() - phaseStarted);
            }
        } else {
            processConversations(world, memories, knowledge, conversations);
            observeVillages(world, villages, villageHistory, villageResources, villageDefense, landmarks, migrations, villageEvents, villageStorage,
                    loadedVillagers, Integer.MAX_VALUE, VILLAGE_CURSORS);
        }

        long villagersStarted = budgeted ? System.nanoTime() : 0L;
        for(Villager villager : loadedVillagers) {
            if(villager.isSleeping() && dreams.latest(villager.getUUID()).map(d -> world.getGameTime()-d.generatedAt() >= 1200).orElse(true)) dreams.generateForSleepingNpc(villager.getUUID(),world.getGameTime(),memories.memoriesOf(villager.getUUID()));
            ages.assignIfAbsent(villager.getUUID(), villager.isBaby() ? NpcAgeGenerator.generateChildAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())) : NpcAgeGenerator.generateAdultAge(new java.util.Random(villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits())));
            if (villager.isBaby() && !families.hasParents(villager.getUUID())) linkBabyToNearbyParents(world, villager, families, memories);
            if (!villager.isBaby() && ages.get(villager.getUUID()).isAdult() && !families.hasSpouse(villager.getUUID())) processCourtship(world, villager, families, courtship, memories, ages);
            if (!villager.isBaby()) maintainFamilyProtection(villager, families, protection);
            inventories.synchronizeFromVillager(villager);
            equipment.sync(villager);
            pickupNearbyItems(world, villager);
            consumeFoodIfNeeded(villager);
            NpcHome home=homes.get(villager.getUUID());
            if(home==null) {
                BlockPos pos=findNearbyHomePoi(world,villager.blockPosition());
                if(pos==null)pos=villager.blockPosition();
                BlockPos door=findNearbyDoor(world,pos);
                home=homes.assignIfAbsent(villager.getUUID(),pos,pos,door==null?pos:door);
            }
            synchronizeFamilyHome(villager, families, homes);
            home=homes.get(villager.getUUID());
            if(homeStorage.get(villager.getUUID())==null){BlockPos storage=findNearestContainer(world,home.homePos(),8);if(storage!=null)homeStorage.link(villager.getUUID(),storage);}
            activities.set(villager.getUUID(),villager.isSleeping()?NpcActivity.SLEEPING:(villager.getNavigation().isInProgress()?NpcActivity.WALKING:NpcActivity.IDLE),10,villager.blockPosition(),world.getGameTime());
            if(villager.getNavigation().isInProgress())fatigue.increase(villager.getUUID(),1);else fatigue.recover(villager.getUUID(),1);
            applyFamilyProtectionBehavior(world, villager, families, protection, homes, stress, behavior);
            var personality = PersonalityGenerator.generate(villager.getUUID());
            java.util.List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(ServerPlayer.class,
                    villager.getBoundingBox().inflate(12), p -> p.isAlive());
            applyLiveSocialBehavior(world, villager, relationships, stress, behavior, homes, homeStorage, nearbyPlayers, personality);
            depositInventoryIntoHomeStorage(world, villager, homeStorage.get(villager.getUUID()));
            BlockPos entrance=home.entrancePos()!=null?home.entrancePos():home.homePos();
            for(ServerPlayer player:nearbyPlayers) {
                if(player.blockPosition().distSqr(entrance)>HOME_RADIUS*HOME_RADIUS) continue;
                if(villager.distanceToSqr(player)>12*12) continue;
                Relationship relationship=relationships.get(villager.getUUID(),player.getUUID());
                if(relationship==null) continue;
                HomeAccess access=HomeAccessPolicy.evaluate(relationship,personality,false);
                if(access==HomeAccess.DENIED&&!hasRecentIntrusion(memories,villager,player,world.getGameTime())) {
                    stress.increase(villager.getUUID(),3);
                    Memory memory=memories.rememberEvent(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_ENTERED_NPC_HOME,world.getGameTime(),MemoryImportance.IMPORTANT);
                    relationships.apply(new MemoryEvent(memory.npcId(),memory.playerId(),memory.type(),memory.gameTime(),memory.importance()));
                    player.sendSystemMessage(Component.literal(villager.getName().getString()+" is upset that you entered their home."));
                } else if(access==HomeAccess.ALLOWED&&stress.value(villager.getUUID())>0) stress.recover(villager.getUUID(),1);
            }
            if(world.getGameTime()%200==0
                    && nearbyPlayers.stream().noneMatch(player -> villager.distanceToSqr(player) <= 8 * 8)) {
                stress.recover(villager.getUUID(),1);
            }
        }
        if (budgeted) metrics.recordVillagers(System.nanoTime() - villagersStarted);

        if (!budgeted) {
            for (Villager villager : world.getEntitiesOfClass(Villager.class,
                    new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                    v -> v.isAlive() && !v.isRemoved())) {
                synchronizeFamilyHome(villager, families, homes);
            }
        }
    }
    /**
     * Gives villagers an actual gameplay pickup path instead of only mirroring whatever is already in their inventory.
     * The operation is bounded to the villager's local area and to one item entity per simulation pass.
     */
    private static void pickupNearbyItems(ServerLevel world, Villager villager) {
        if (!villager.isAlive() || villager.isSleeping() || villager.isTrading()) return;
        java.util.List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class,
                villager.getBoundingBox().inflate(2.5),
                item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty());
        for (ItemEntity entity : items) {
            ItemStack offered = entity.getItem();
            if (offered.isEmpty() || !villagerCanUseItem(offered)) continue;
            ItemStack before = offered.copy();
            ItemStack remainder = villager.getInventory().addItem(offered.copy());
            int picked = before.getCount() - remainder.getCount();
            if (picked <= 0) continue;
            entity.setItem(remainder);
            if (remainder.isEmpty()) entity.discard();
            break;
        }
    }

    /** Lets a villager actually use food it carries when injured instead of keeping food as inert inventory state. */
    private static void consumeFoodIfNeeded(Villager villager) {
        if (!villager.isAlive() || villager.isBaby() || villager.getHealth() >= villager.getMaxHealth() - 4.0f) return;
        var inventory = villager.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isVillagerFood(stack)) continue;
            stack.shrink(1);
            inventory.setItem(slot, stack);
            villager.heal(4.0f);
            return;
        }
    }

    private static boolean isVillagerFood(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
    }

    /** Keeps the live pickup path compatible with vanilla villager priorities. */
    private static boolean villagerCanUseItem(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)
                || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.EMERALD);
    }

    /** A linked home chest is now a real destination: the villager must reach it before depositing one stack. */
    private static void depositInventoryIntoHomeStorage(ServerLevel world, Villager villager, BlockPos storagePos) {
        if (storagePos == null || !villager.isAlive() || villager.isTrading()) return;
        if (villager.blockPosition().distSqr(storagePos) > 4 * 4) return;
        if (!world.hasChunkAt(storagePos)) return;
        if (!(world.getBlockEntity(storagePos) instanceof Container container)) return;
        var inventory = villager.getInventory();
        for (int sourceSlot = 0; sourceSlot < inventory.getContainerSize(); sourceSlot++) {
            ItemStack source = inventory.getItem(sourceSlot);
            if (source.isEmpty() || !villagerCanUseItem(source)) continue;
            for (int targetSlot = 0; targetSlot < container.getContainerSize(); targetSlot++) {
                ItemStack target = container.getItem(targetSlot);
                if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(source, target)) continue;
                if (target.isEmpty()) {
                    container.setItem(targetSlot, source.copy());
                    inventory.setItem(sourceSlot, ItemStack.EMPTY);
                    container.setChanged();
                    return;
                }
                int room = Math.min(target.getMaxStackSize(), container.getMaxStackSize()) - target.getCount();
                if (room <= 0) continue;
                int moved = Math.min(room, source.getCount());
                target.grow(moved);
                source.shrink(moved);
                inventory.setItem(sourceSlot, source);
                container.setItem(targetSlot, target);
                container.setChanged();
                return;
            }
        }
    }
    private static void applyLiveSocialBehavior(ServerLevel world, Villager villager, RelationshipManager relationships, NpcStressManager stress, NpcBehaviorEngine behavior, NpcHomeManager homes, NpcHomeStorageManager homeStorage, java.util.List<ServerPlayer> nearbyPlayers, PersonalityProfile personality) {
        if (!villager.getNavigation().isDone() && !villager.isTrading()) return;
        String role=villager.getVillagerData().toString().toLowerCase(java.util.Locale.ROOT);
        boolean worker=role.contains("farmer")||role.contains("librarian")||role.contains("cleric")||role.contains("armorer")||role.contains("toolsmith")||role.contains("weaponsmith");
        boolean danger=!world.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,villager.getBoundingBox().inflate(8),m->m.isAlive()).isEmpty();
        if(danger){NpcHome home=homes.get(villager.getUUID());if(home!=null)villager.getNavigation().moveTo(home.homePos().getX(),home.homePos().getY(),home.homePos().getZ(),1.15);stress.increase(villager.getUUID(),2);return;}
        if(worker && villager.getInventory().getContainerSize()>0 && homeStorage.get(villager.getUUID())!=null){BlockPos storage=homeStorage.get(villager.getUUID());if(villager.blockPosition().distSqr(storage)>4*4)villager.getNavigation().moveTo(storage.getX(),storage.getY(),storage.getZ(),0.8);}
        for (ServerPlayer player : nearbyPlayers) {
            Relationship relationship=relationships.get(villager.getUUID(),player.getUUID());
            if(relationship==null) continue;
            NpcActivity activity=villager.isSleeping()?NpcActivity.SLEEPING:(villager.isTrading()?NpcActivity.TRADING:NpcActivity.IDLE);
            NpcDecision decision=behavior.decide(activity,relationship,personality,new NpcStress(stress.value(villager.getUUID())));
            if(decision==NpcDecision.FOLLOW && relationship.trust()>=30) villager.getNavigation().moveTo(player,1.0);
            else if(decision==NpcDecision.LEAVE) {
                NpcHome home=homes.get(villager.getUUID());
                if(home!=null) villager.getNavigation().moveTo(home.homePos().getX(),home.homePos().getY(),home.homePos().getZ(),1.0);
            }
            break;
        }
    }

    private static void processConversations(ServerLevel world, MemoryManager memories, KnowledgeManager knowledge, ConversationManager conversations) {
        processConversations(world, memories, knowledge, conversations,
                new java.util.ArrayList<>(world.getEntitiesOfClass(Villager.class,
                        new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                        v -> v.isAlive() && !v.isRemoved())));
    }

    private static void processConversations(ServerLevel world, MemoryManager memories, KnowledgeManager knowledge,
                                              ConversationManager conversations, java.util.List<Villager> villagers) {
        for (Villager villager : villagers) {
            for (Memory memory : memories.memoriesOf(villager.getUUID()).stream().limit(8).toList()) {
                knowledge.learn(villager.getUUID(), new KnowledgeFact(memory.playerId(), memory.type(), memory.gameTime(),
                        villager.getUUID(), KnowledgeOrigin.DIRECT, 100, memory.gameTime()));
            }
        }
        if (world.getGameTime() % KNOWLEDGE_DECAY_INTERVAL == 0) knowledge.decay(world.getGameTime());
        if (world.getGameTime() % SOCIAL_INTERVAL != 0) return;
        for (Villager first : villagers) {
            Villager second = world.getEntitiesOfClass(Villager.class, first.getBoundingBox().inflate(4),
                    v -> v.isAlive() && !v.getUUID().equals(first.getUUID())).stream().findFirst().orElse(null);
            if (second == null) continue;
            java.util.List<KnowledgeFact> shared = knowledge.facts(first.getUUID()).stream()
                    .filter(f -> f.confidence() >= 20).limit(2)
                    .map(f -> f.reported(world.getGameTime())).toList();
            if (shared.isEmpty()) continue;
            for (KnowledgeFact fact : shared) knowledge.learn(second.getUUID(), fact);
            conversations.record(new Conversation(first.getUUID(), second.getUUID(), world.getGameTime(), shared));
        }
    }

    private static void refreshVillagerRegistry(ServerLevel world) {
        java.util.List<UUID> previous = VILLAGER_REGISTRY.getOrDefault(world, java.util.List.of());
        java.util.Set<UUID> previousSet = new java.util.HashSet<>(previous);
        java.util.List<UUID> discovered = new java.util.ArrayList<>();
        for (Villager villager : world.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                v -> v.isAlive() && !v.isRemoved())) {
            discovered.add(villager.getUUID());
        }
        java.util.Set<UUID> discoveredSet = new java.util.HashSet<>(discovered);
        java.util.List<UUID> ids = new java.util.ArrayList<>(discovered.size());
        // Preserve rotation order so discovery never resets the live scheduler to index zero.
        for (UUID id : previous) if (discoveredSet.contains(id)) ids.add(id);
        for (UUID id : discovered) if (!previousSet.contains(id)) ids.add(id);
        VILLAGER_REGISTRY.put(world, ids);
        if (ids.isEmpty()) {
            VILLAGER_CURSORS.put(world, 0);
            SOCIAL_CURSORS.put(world, 0);
            VILLAGE_CURSORS.put(world, 0);
        } else {
            VILLAGER_CURSORS.put(world, VILLAGER_CURSORS.getOrDefault(world, 0) % ids.size());
            SOCIAL_CURSORS.put(world, SOCIAL_CURSORS.getOrDefault(world, 0) % ids.size());
        }
    }

    private static java.util.List<Villager> loadedVillagersFromRegistry(ServerLevel world) {
        java.util.List<Villager> result = new java.util.ArrayList<>();
        java.util.List<UUID> ids = VILLAGER_REGISTRY.get(world);
        if (ids == null) return result;
        for (UUID id : ids) {
            Entity entity = world.getEntity(id);
            if (entity instanceof Villager villager && villager.isAlive() && !villager.isRemoved() && world.hasChunkAt(villager.blockPosition())) result.add(villager);
        }
        return result;
    }

    private static java.util.List<Villager> nextBudgetedVillagers(ServerLevel world, int budget, java.util.Map<ServerLevel,Integer> cursors) {
        java.util.List<UUID> ids = VILLAGER_REGISTRY.get(world);
        if (ids == null || ids.isEmpty() || budget <= 0) return new java.util.ArrayList<>();
        int start = cursors.getOrDefault(world, 0) % ids.size();
        java.util.List<Villager> result = new java.util.ArrayList<>(Math.min(budget, ids.size()));
        int checked = 0;
        int index = start;
        while (checked < ids.size() && result.size() < budget) {
            UUID id = ids.get(index);
            Entity entity = world.getEntity(id);
            if (entity instanceof Villager villager && villager.isAlive() && !villager.isRemoved()
                    && world.hasChunkAt(villager.blockPosition())) result.add(villager);
            index = (index + 1) % ids.size();
            checked++;
        }
        cursors.put(world, index);
        return result;
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
                                                       NpcStressManager stress, NpcBehaviorEngine engine) {
        java.util.List<java.util.UUID> children = families.childrenOf(villager.getUUID());
        if (children.isEmpty()) return;
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
            memories.inheritFamilyHistory(adults.get(0).getUUID(), child.getUUID(), time);
            memories.inheritFamilyHistory(adults.get(1).getUUID(), child.getUUID(), time);
            memories.rememberEvent(child.getUUID(), adults.get(0).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            memories.rememberEvent(child.getUUID(), adults.get(1).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
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
        int progress = courtship.advance(villager.getUUID(), partner.getUUID(), 20);
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
            // AFTER_DEATH may run after vanilla has already cleared the live inventory.
            // Prefer the persistent mirror, but capture any important live item that was
            // missed by the regular simulation loop before attempting inheritance.
            inventories.captureImportantItemsIfMissing(villager);
            Villager liveHeir = world.getEntity(heir) instanceof Villager candidate ? candidate : null;
            int inherited = liveHeir != null
                ? inventories.inheritImportantItems(villager.getUUID(), liveHeir)
                : inventories.inheritImportantItems(villager.getUUID(), heir);
            if (inherited > 0) {
                memories.rememberEvent(heir, villager.getUUID(), MemoryEventType.NPC_INHERITED_ITEM, world.getGameTime(), MemoryImportance.HISTORICAL);
            }
        }
    }
    private static void observeVillages(ServerLevel world, VillageManager villages, VillageHistoryManager history,
            VillageResourceManager resources, VillageDefenseManager defense, VillageLandmarkManager landmarks,
            VillageMigrationManager migrations, VillageEventManager villageEvents, VillageStorageManager villageStorage,
            java.util.List<Villager> source, int budget, java.util.Map<ServerLevel,Integer> cursors) {
        java.util.Map<Long, java.util.List<Villager>> clusters = new java.util.HashMap<>();
        for (Villager v : source) {
            long key=(((long)(v.blockPosition().getX()>>5))<<32)^((v.blockPosition().getZ()>>5)&0xffffffffL);
            clusters.computeIfAbsent(key,ignored->new java.util.ArrayList<>()).add(v);
        }
        if (clusters.isEmpty()) return;
        java.util.List<Long> keys = new java.util.ArrayList<>(clusters.keySet());
        java.util.Collections.sort(keys);
        int start = cursors.getOrDefault(world, 0) % keys.size();
        int processed = 0;
        for (int offset=0; offset<keys.size() && processed<budget; offset++) {
            int index=(start+offset)%keys.size();
            var members=clusters.get(keys.get(index)); if(members==null||members.isEmpty()) continue;
            long sx=0,sz=0; for(var v:members){sx+=v.blockPosition().getX();sz+=v.blockPosition().getZ();}
            BlockPos center=new BlockPos((int)(sx/members.size()),members.get(0).blockPosition().getY(),(int)(sz/members.size()));
            java.util.Set<UUID> claimed=new java.util.HashSet<>();
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
            int containers=0,occupied=0,capacity=0;
            for(BlockPos p:BlockPos.betweenClosed(center.offset(-16,-4,-16),center.offset(16,8,16))){
                var be=world.getBlockEntity(p);
                if(be instanceof net.minecraft.world.Container container){
                    containers++; capacity+=container.getContainerSize();
                    for(int slot=0;slot<container.getContainerSize();slot++)if(!container.getItem(slot).isEmpty())occupied++;
                }
            }
            villageStorage.observe(villageId,new VillageStorage(containers,occupied,capacity));
            resources.observe(villageId,new VillageResources(food,0,occupied,capacity));
            int golems=world.getEntitiesOfClass(IronGolem.class,new net.minecraft.world.phys.AABB(center).inflate(32),g->g.isAlive()).size();
            defense.observe(villageId,new VillageDefense(golems,0,0));
            for(var pos:world.getPoiManager().findAllWithType(type->type.is(PoiTypeTags.VILLAGE),pos->true,center,32,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).map(pair->pair.getSecond()).toList())
                landmarks.add(villageId,new VillageLandmark("village_poi",pos,world.getGameTime()));
            processed++;
        }
        cursors.put(world,(start+Math.max(1,processed))%keys.size());
    }

    private static BlockPos findNearbyHomePoi(ServerLevel world,BlockPos pos){return world.getPoiManager().findClosest(type->type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),pos,16,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).orElse(null);}
    private static BlockPos findNearbyDoor(ServerLevel world,BlockPos pos){BlockPos best=null;double d=257;for(BlockPos p:BlockPos.betweenClosed(pos.offset(-8,-2,-8),pos.offset(8,4,8)))if(world.getBlockState(p).is(net.minecraft.tags.BlockTags.DOORS)){double x=p.distSqr(pos);if(x<d){d=x;best=p.immutable();}}return best;}
    private static BlockPos findNearestContainer(ServerLevel world,BlockPos center,int radius){BlockPos best=null;double d=Double.MAX_VALUE;for(BlockPos p:BlockPos.betweenClosed(center.offset(-radius,-3,-radius),center.offset(radius,3,radius)))if(world.getBlockEntity(p) instanceof net.minecraft.world.Container){double x=p.distSqr(center);if(x<d){d=x;best=p.immutable();}}return best;}

    private static boolean hasRecentIntrusion(MemoryManager memories,Villager villager,ServerPlayer player,long gameTime){
        return memories.findMostRecentMemory(villager.getUUID(),player.getUUID(),MemoryEventType.PLAYER_ENTERED_NPC_HOME).map(m->gameTime-m.gameTime()<INTRUSION_COOLDOWN).orElse(false);
    }
}