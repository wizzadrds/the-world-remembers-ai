package com.wizzadrds.theworldremembers.inventory;

import com.mojang.serialization.Codec;

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
    private static final Codec<NpcInventory> INVENTORY_CODEC=NpcItemStack.CODEC.listOf().xmap(NpcInventory::new,NpcInventory::items);
    private static final Codec<NpcInventoryManager> CODEC=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString),INVENTORY_CODEC).xmap(m->{NpcInventoryManager x=new NpcInventoryManager();x.inventories.putAll(m);return x;},x->x.inventories);
    private static final SavedDataType<NpcInventoryManager> TYPE=new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "inventories"),NpcInventoryManager::new,CODEC,null);
    public static NpcInventoryManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new NpcInventoryManager():l.getDataStorage().computeIfAbsent(TYPE);}
    public NpcInventory getOrCreate(UUID id){return inventories.computeIfAbsent(id,k->new NpcInventory());}
    public void transferIn(UUID id,String item,int count){getOrCreate(id).add(item,count);setDirty();}
    public int transferOut(UUID id,String item,int count){boolean removed=getOrCreate(id).remove(item,count);if(removed)setDirty();return removed?count:0;}
    public int count(UUID id,String item){return getOrCreate(id).count(item);}
    public void synchronizeFromVillager(Villager villager){NpcInventory target=new NpcInventory();var inventory=villager.getInventory();for(int slot=0;slot<inventory.getContainerSize();slot++){var stack=inventory.getItem(slot);if(stack.isEmpty())continue;target.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount());}inventories.put(villager.getUUID(),target);setDirty();}
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
            if (!isImportantItem(itemId) || target.count(itemId) > 0) continue;
            target.add(itemId, stack.getCount());
            captured += stack.getCount();
        }
        if (captured > 0) setDirty();
        return captured;
    }
    public int inheritImportantItems(UUID from, UUID to) {
        NpcInventory source = getOrCreate(from);
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
     * live heir inventory so the next simulation mirror cannot erase the inheritance.
     */
    public int inheritImportantItems(UUID from, Villager heir) {
        NpcInventory source = getOrCreate(from);
        NpcInventory target = getOrCreate(heir.getUUID());
        int moved = 0;
        for (NpcItemStack stack : source.items()) {
            if (!isImportantItem(stack.itemId())) continue;
            if (!source.remove(stack.itemId(), stack.count())) continue;

            int remaining = stack.count();
            var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.Identifier.parse(stack.itemId()));
            ItemStack live = new ItemStack(item, remaining);
            ItemStack remainder = heir.getInventory().addItem(live);
            int materialized = remaining - remainder.getCount();
            if (materialized > 0) {
                target.add(stack.itemId(), materialized);
                moved += materialized;
            }
            if (!remainder.isEmpty()) {
                target.add(stack.itemId(), remainder.getCount());
            }
        }
        if (moved > 0) setDirty();
        return moved;
    }

    static boolean isImportantItem(String itemId) {
        return itemId.equals("minecraft:diamond")
            || itemId.equals("minecraft:emerald")
            || itemId.equals("minecraft:netherite_ingot")
            || itemId.equals("minecraft:totem_of_undying")
            || itemId.equals("minecraft:enchanted_golden_apple");
    }
}
