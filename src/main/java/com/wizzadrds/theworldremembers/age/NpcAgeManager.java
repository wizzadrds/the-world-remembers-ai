package com.wizzadrds.theworldremembers.age;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcAgeManager extends PersistentState {
    private static final String KEY = "npcAges";
    private final Map<UUID, NpcAge> ages = new HashMap<>();

    public static NpcAgeManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new Type<>(NpcAgeManager::new, NpcAgeManager::fromNbt, null), KEY);
    }

    public NpcAge get(UUID npcId) {
        return ages.get(npcId);
    }

    public NpcAge assignIfAbsent(UUID npcId, int years) {
        return ages.computeIfAbsent(npcId, id -> new NpcAge(id, years));
    }

    public boolean hasAge(UUID npcId) {
        return ages.containsKey(npcId);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        ages.values().forEach(age -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("npc", age.npcId());
            entry.putInt("years", age.years());
            list.add(entry);
        });
        nbt.put(KEY, list);
        return nbt;
    }

    private static NpcAgeManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NpcAgeManager manager = new NpcAgeManager();
        if (!nbt.contains(KEY, NbtElement.LIST_TYPE)) return manager;
        NbtList list = nbt.getList(KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            try {
                UUID npc = entry.getUuid("npc");
                manager.ages.put(npc, new NpcAge(npc, entry.getInt("years")));
            } catch (IllegalArgumentException ignored) {
                // Skip obsolete/corrupt records without preventing world loading.
            }
        }
        return manager;
    }
}
