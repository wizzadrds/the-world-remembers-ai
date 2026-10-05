package com.wizzadrds.theworldremembers.stress;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcStressManager extends PersistentState {
    private static final String KEY = "npcStress";
    private final Map<UUID, NpcStress> stress = new HashMap<>();

    public static NpcStressManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new Type<>(NpcStressManager::new, NpcStressManager::fromNbt, null),
                KEY
        );
    }

    public NpcStress getOrCreate(UUID npcId) {
        return stress.computeIfAbsent(npcId, id -> new NpcStress(0));
    }

    public void increase(UUID npcId, int amount) {
        stress.put(npcId, getOrCreate(npcId).increase(amount));
        markDirty();
    }

    public void decrease(UUID npcId, int amount) {
        stress.put(npcId, getOrCreate(npcId).decrease(amount));
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        stress.forEach((npc, value) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("npc", npc);
            entry.putInt("value", value.value());
            list.add(entry);
        });
        nbt.put("stress", list);
        return nbt;
    }

    private static NpcStressManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NpcStressManager manager = new NpcStressManager();
        if (!nbt.contains("stress", NbtElement.LIST_TYPE)) return manager;
        NbtList list = nbt.getList("stress", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            manager.stress.put(entry.getUuid("npc"), new NpcStress(entry.getInt("value")));
        }
        return manager;
    }
}
