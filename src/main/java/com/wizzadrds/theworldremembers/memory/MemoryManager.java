package com.wizzadrds.theworldremembers.memory;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.minecraft.entity.passive.VillagerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MemoryManager extends PersistentState {
    private static final String STATE_ID = "the_world_remembers_memories";
    private static final String MEMORIES_KEY = "memories";
    private final List<Memory> memories = new ArrayList<>();

    private static final PersistentStateType<MemoryManager> TYPE = new PersistentStateType<>(
            STATE_ID, MemoryManager::new, MemoryManager::fromNbt, null
    );

    private MemoryManager() {}

    public static MemoryManager get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not available");
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    public void rememberBreadGift(ServerPlayerEntity player, VillagerEntity villager) {
        memories.add(new Memory(villager.getUuid(), player.getUuid(), MemoryEventType.PLAYER_GAVE_BREAD,
                villager.getEntityWorld().getTime(), MemoryImportance.INTERESTING));
        markDirty();
    }

    public Optional<Memory> findMostRecentMemory(UUID npcId, UUID playerId) {
        for (int i = memories.size() - 1; i >= 0; i--) {
            Memory memory = memories.get(i);
            if (memory.npcId().equals(npcId) && memory.playerId().equals(playerId)) return Optional.of(memory);
        }
        return Optional.empty();
    }

    private static MemoryManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        MemoryManager state = new MemoryManager();
        NbtList list = nbt.getList(MEMORIES_KEY, NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            state.memories.add(new Memory(
                    UUID.fromString(entry.getString("npc")),
                    UUID.fromString(entry.getString("player")),
                    MemoryEventType.valueOf(entry.getString("type")),
                    entry.getLong("time"),
                    MemoryImportance.valueOf(entry.getString("importance"))
            ));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (Memory memory : memories) {
            NbtCompound entry = new NbtCompound();
            entry.putString("npc", memory.npcId().toString());
            entry.putString("player", memory.playerId().toString());
            entry.putString("type", memory.type().name());
            entry.putLong("time", memory.gameTime());
            entry.putString("importance", memory.importance().name());
            list.add(entry);
        }
        nbt.put(MEMORIES_KEY, list);
        return nbt;
    }
}
