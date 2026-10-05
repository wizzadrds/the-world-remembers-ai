package com.wizzadrds.theworldremembers.inventory;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NpcInventoryManager extends PersistentState {
    private static final String STATE_ID = "the_world_remembers_npc_inventories";
    private static final String INVENTORIES_KEY = "inventories";
    private final List<Entry> entries = new ArrayList<>();

    private static final PersistentStateType<NpcInventoryManager> TYPE =
            new PersistentStateType<>(STATE_ID, NpcInventoryManager::new, NpcInventoryManager::fromNbt, null);

    private NpcInventoryManager() {}

    public static NpcInventoryManager get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not available");
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    public NpcInventory getOrCreate(UUID npcId) {
        return entries.stream().filter(e -> e.npcId().equals(npcId)).findFirst().map(Entry::inventory).orElseGet(() -> {
            NpcInventory inventory = new NpcInventory();
            entries.add(new Entry(npcId, inventory));
            markDirty();
            return inventory;
        });
    }

    public boolean transferIn(UUID npcId, String itemId, int count) {
        getOrCreate(npcId).add(itemId, count);
        markDirty();
        return true;
    }

    public boolean transferOut(UUID npcId, String itemId, int count) {
        boolean removed = getOrCreate(npcId).remove(itemId, count);
        if (removed) markDirty();
        return removed;
    }

    public int count(UUID npcId, String itemId) {
        return getOrCreate(npcId).count(itemId);
    }

    private record Entry(UUID npcId, NpcInventory inventory) {}

    private static NpcInventoryManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NpcInventoryManager state = new NpcInventoryManager();
        NbtList list = nbt.getList(INVENTORIES_KEY, NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            NpcInventory inventory = new NpcInventory();
            for (NbtElement itemElement : entry.getList("items", NbtElement.COMPOUND_TYPE)) {
                NbtCompound item = (NbtCompound) itemElement;
                inventory.add(item.getString("id"), item.getInt("count"));
            }
            state.entries.add(new Entry(UUID.fromString(entry.getString("npc")), inventory));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (Entry entry : entries) {
            NbtCompound serialized = new NbtCompound();
            serialized.putString("npc", entry.npcId().toString());
            NbtList items = new NbtList();
            for (NpcItemStack stack : entry.inventory().items()) {
                NbtCompound item = new NbtCompound();
                item.putString("id", stack.itemId());
                item.putInt("count", stack.count());
                items.add(item);
            }
            serialized.put("items", items);
            list.add(serialized);
        }
        nbt.put(INVENTORIES_KEY, list);
        return nbt;
    }
}
