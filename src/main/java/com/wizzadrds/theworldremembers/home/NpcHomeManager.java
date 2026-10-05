package com.wizzadrds.theworldremembers.home;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcHomeManager extends PersistentState {
    private static final String KEY = "npcHomes";
    private final Map<UUID, NpcHome> homes = new HashMap<>();

    public static NpcHomeManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new Type<>(NpcHomeManager::new, NpcHomeManager::fromNbt, null),
                KEY
        );
    }

    public NpcHome get(UUID npcId) {
        return homes.get(npcId);
    }

    public NpcHome assignIfAbsent(UUID npcId, BlockPos homePos, BlockPos bedPos, BlockPos entrancePos) {
        return homes.computeIfAbsent(npcId,
                id -> new NpcHome(id, homePos.toImmutable(), bedPos == null ? null : bedPos.toImmutable(),
                        entrancePos == null ? null : entrancePos.toImmutable()));
    }

    public boolean hasHome(UUID npcId) {
        return homes.containsKey(npcId);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        homes.values().forEach(home -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("npc", home.npcId());
            writePos(entry, "home", home.homePos());
            if (home.bedPos() != null) writePos(entry, "bed", home.bedPos());
            if (home.entrancePos() != null) writePos(entry, "entrance", home.entrancePos());
            list.add(entry);
        });
        nbt.put("homes", list);
        return nbt;
    }

    private static void writePos(NbtCompound nbt, String key, BlockPos pos) {
        nbt.putInt(key + "X", pos.getX());
        nbt.putInt(key + "Y", pos.getY());
        nbt.putInt(key + "Z", pos.getZ());
    }

    private static NpcHomeManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NpcHomeManager manager = new NpcHomeManager();
        if (!nbt.contains("homes", NbtElement.LIST_TYPE)) return manager;
        NbtList list = nbt.getList("homes", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            UUID npc = entry.getUuid("npc");
            BlockPos home = readPos(entry, "home");
            BlockPos bed = readOptionalPos(entry, "bed");
            BlockPos entrance = readOptionalPos(entry, "entrance");
            manager.homes.put(npc, new NpcHome(npc, home, bed, entrance));
        }
        return manager;
    }

    private static BlockPos readPos(NbtCompound nbt, String key) {
        return new BlockPos(nbt.getInt(key + "X"), nbt.getInt(key + "Y"), nbt.getInt(key + "Z"));
    }

    private static BlockPos readOptionalPos(NbtCompound nbt, String key) {
        if (!nbt.contains(key + "X")) return null;
        return readPos(nbt, key);
    }
}
