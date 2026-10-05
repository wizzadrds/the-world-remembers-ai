package com.wizzadrds.theworldremembers.family;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FamilyManager extends PersistentState {
    private static final String KEY = "npcFamilies";
    private final List<FamilyRelation> relations = new ArrayList<>();

    public static FamilyManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new Type<>(FamilyManager::new, FamilyManager::fromNbt, null), KEY);
    }

    public boolean add(FamilyRelation relation) {
        if (relations.contains(relation)) return false;
        relations.add(relation);
        markDirty();
        return true;
    }

    public boolean remove(FamilyRelation relation) {
        boolean removed = relations.remove(relation);
        if (removed) markDirty();
        return removed;
    }

    public List<FamilyRelation> getRelations(UUID npcId) {
        return relations.stream()
                .filter(r -> r.npcId().equals(npcId) || r.relatedNpcId().equals(npcId))
                .toList();
    }

    public boolean areRelated(UUID first, UUID second) {
        return relations.stream().anyMatch(r ->
                (r.npcId().equals(first) && r.relatedNpcId().equals(second))
                        || (r.npcId().equals(second) && r.relatedNpcId().equals(first)));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (FamilyRelation relation : relations) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("npc", relation.npcId());
            entry.putUuid("related", relation.relatedNpcId());
            entry.putString("type", relation.type().name());
            list.add(entry);
        }
        nbt.put(KEY, list);
        return nbt;
    }

    private static FamilyManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        FamilyManager manager = new FamilyManager();
        if (!nbt.contains(KEY, NbtElement.LIST_TYPE)) return manager;
        NbtList list = nbt.getList(KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            try {
                manager.relations.add(new FamilyRelation(
                        entry.getUuid("npc"),
                        entry.getUuid("related"),
                        FamilyRelationType.valueOf(entry.getString("type"))
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore one invalid relation instead of breaking the whole world.
            }
        }
        return manager;
    }
}
