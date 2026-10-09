package com.wizzadrds.theworldremembers.inventory;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class NpcInventoryManager extends SavedData {
    private final Map<UUID,NpcInventory> inventories=new HashMap<>();
    /** Important inherited stacks that did not fit in the heir's live inventory yet. */
    private final Map<UUID,NpcInventory> pendingInherited=new HashMap<>();

    static final Codec<NpcInventory> INVENTORY_CODEC=NpcItemStack.CODEC.listOf().xmap(NpcInventory::new,NpcInventory::items);
    private static final Codec<Map<UUID,NpcInventory>> INVENTORIES_CODEC =
            Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), INVENTORY_CODEC);

    private record PersistedState(Map<UUID,NpcInventory> inventories, Map<UUID,NpcInventory> pendingInherited) {}
    private static final Codec<PersistedState> PERSISTED_STATE_CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<PersistedState> instance) -> instance.group(
                    INVENTORIES_CODEC.fieldOf("inventories").forGetter(PersistedState::inventories),
                    INVENTORIES_CODEC.optionalFieldOf("pending_inherited", Map.of()).forGetter(PersistedState::pendingInherited)
            ).apply(instance, PersistedState::new)
    );

    /*
     * Accept the pre-overflow format (a bare UUID -> inventory map) so existing worlds
     * continue to load, while new saves use the explicit persisted-state object.
     */
    static final Codec<NpcInventoryManager> CODEC = Codec.either(
            PERSISTED_STATE_CODEC,
            INVENTORIES_CODEC
    ).xmap(
            value -> value.map(
                    state -> {
                        NpcInventoryManager manager = new NpcInventoryManager();
                        manager.inventories.putAll(state.inventories());
                        manager.pendingInherited.putAll(state.pendingInherited());
                        return manager;
                    },
                    inventories -> {
                        NpcInventoryManager manager = new NpcInventoryManager();
                        manager.inventories.putAll(inventories);
                        return manager;
                    }
            ),
            manager -> Either.left(
                    new PersistedState(manager.inventories, manager.pendingInherited))
    );

    private static final SavedDataType<NpcInventoryManager> TYPE=new SavedDataType<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "inventories"),
            NpcInventoryManager::new,CODEC,null);

    public static NpcInventoryManager get(MinecraftServer server){
        ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);
        return l==null?new NpcInventoryManager():l.getDataStorage().computeIfAbsent(TYPE);
    }

    public NpcInventory getOrCreate(UUID id){return inventories.computeIfAbsent(id,k->new NpcInventory());}
    public void transferIn(UUID id,String item,int count){if(id==null||item==null||item.isBlank()||count<=0)return;getOrCreate(id).add(item,count);setDirty();}
    public int transferOut(UUID id,String item,int count){NpcInventory inventory=inventories.get(id);if(inventory==null)return 0;boolean removed=inventory.remove(item,count);if(removed)setDirty();return removed?count:0;}
    public int count(UUID id,String item){NpcInventory inventory=inventories.get(id);return inventory==null?0:inventory.count(item);}

    /** Returns a best-effort item-only reconstruction for death drops when the live inventory is already cleared. */
    public java.util.List<ItemStack> snapshot(UUID id) {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        NpcInventory inventory = inventories.get(id);
        if (inventory == null) return result;
        for (NpcItemStack stack : inventory.items()) {
            var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(stack.itemId()));
            if (item != null && !item.equals(net.minecraft.world.item.Items.AIR)) {
                result.add(new ItemStack(item, stack.count()));
            }
        }
        return result;
    }

    /** Removes the persistent carried-inventory mirror after a physical death drop. */
    public void clear(UUID id) {
        boolean changed = inventories.remove(id) != null;
        changed |= pendingInherited.remove(id) != null;
        if (changed) setDirty();
    }

    /**
     * Mirrors ordinary live inventory state while retaining inherited important stacks
     * that are waiting for room in the live inventory.
     */
    public void synchronizeFromVillager(Villager villager){
        UUID id=villager.getUUID();
        NpcInventory previous=inventories.get(id);
        java.util.List<NpcItemStack> previousItems=previous==null?java.util.List.of():previous.items();
        NpcInventory target=new NpcInventory();
        var inventory=villager.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var stack=inventory.getItem(slot);
            if(stack.isEmpty())continue;
            target.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount());
        }

        NpcInventory pending=pendingInherited.get(id);
        java.util.List<NpcItemStack> pendingBefore=pending==null?java.util.List.of():pending.items();
        if(pending!=null){
            for(NpcItemStack stack:pendingBefore){
                target.add(stack.itemId(),stack.count());
            }
            materializePending(villager, pending);
            if(pending.items().isEmpty()) pendingInherited.remove(id);
        }
        java.util.List<NpcItemStack> pendingAfter=pendingInherited.get(id)==null
                ?java.util.List.of():pendingInherited.get(id).items();

        inventories.put(id,target);
        if(!previousItems.equals(target.items()) || !pendingBefore.equals(pendingAfter)) setDirty();
    }

    /**
     * Captures only important live items without replacing an existing snapshot.
     * This is a last-resort death-path guard for cases where the death callback runs
     * before the regular simulation mirror has been populated.
     */
    public int captureImportantItemsIfMissing(Villager villager) {
        NpcInventory target = getOrCreate(villager.getUUID());
        int captured = 0;
        var inventory = villager.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!isImportantItem(itemId)) continue;
            int missing = stack.getCount() - target.count(itemId);
            if (missing <= 0) continue;
            target.add(itemId, missing);
            captured += missing;
        }
        if (captured > 0) setDirty();
        return captured;
    }

    public int inheritImportantItems(UUID from, UUID to) {
        NpcInventory source = inventories.get(from);
        if (source == null || to == null) return 0;
        NpcInventory target = getOrCreate(to);
        int moved = 0;
        for (NpcItemStack stack : source.items()) {
            if (!isImportantItem(stack.itemId())) continue;
            if (source.remove(stack.itemId(), stack.count())) {
                target.add(stack.itemId(), stack.count());
                moved += stack.count();
            }
        }
        if (moved > 0) setDirty();
        return moved;
    }

    /**
     * Moves important persistent possessions and materializes the moved stacks in the
     * live heir inventory. If the inventory is full, the remainder is persisted as
     * pending inheritance and retried by the next normal inventory synchronization.
     */
    public int inheritImportantItems(UUID from, Villager heir) {
        NpcInventory source = getOrCreate(from);
        NpcInventory target = getOrCreate(heir.getUUID());
        NpcInventory pending = pendingInherited.computeIfAbsent(heir.getUUID(), ignored -> new NpcInventory());
        int moved = 0;
        int pendingMoved = 0;

        for (NpcItemStack stack : source.items()) {
            if (!isImportantItem(stack.itemId())) continue;
            if (!source.remove(stack.itemId(), stack.count())) continue;

            int remaining = stack.count();
            var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(stack.itemId()));
            ItemStack live = new ItemStack(item, remaining);
            ItemStack remainder = heir.getInventory().addItem(live);
            int materialized = remaining - remainder.getCount();
            if (materialized > 0) {
                target.add(stack.itemId(), materialized);
                moved += materialized;
            }
            if (!remainder.isEmpty()) {
                pending.add(stack.itemId(), remainder.getCount());
                target.add(stack.itemId(), remainder.getCount());
                pendingMoved += remainder.getCount();
            }
        }

        if (pending.items().isEmpty()) pendingInherited.remove(heir.getUUID());
        if (moved > 0 || !pending.items().isEmpty()) setDirty();
        return moved + pendingMoved;
    }

    private void materializePending(Villager heir, NpcInventory pending) {
        for (NpcItemStack stack : new ArrayList<>(pending.items())) {
            var item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(stack.itemId()));
            ItemStack remainder = heir.getInventory().addItem(new ItemStack(item, stack.count()));
            int materialized = stack.count() - remainder.getCount();
            if (materialized > 0) {
                pending.remove(stack.itemId(), materialized);
            }
        }
    }

    static boolean isImportantItem(String itemId) {
        return itemId.equals("minecraft:diamond")
            || itemId.equals("minecraft:emerald")
            || itemId.equals("minecraft:netherite_ingot")
            || itemId.equals("minecraft:totem_of_undying")
            || itemId.equals("minecraft:enchanted_golden_apple");
    }
}
