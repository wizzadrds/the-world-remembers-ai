package com.wizzadrds.theworldremembers.inventory;

import com.mojang.serialization.Codec;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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
    static boolean isImportantItem(String itemId) {
        return itemId.equals("minecraft:diamond")
            || itemId.equals("minecraft:emerald")
            || itemId.equals("minecraft:netherite_ingot")
            || itemId.equals("minecraft:totem_of_undying")
            || itemId.equals("minecraft:enchanted_golden_apple");
    }
}
