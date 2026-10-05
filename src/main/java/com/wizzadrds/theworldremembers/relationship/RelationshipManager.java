package com.wizzadrds.theworldremembers.relationship;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import com.wizzadrds.theworldremembers.memory.MemoryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RelationshipManager extends PersistentState {
    private static final String STATE_ID = "the_world_remembers_relationships";
    private static final String RELATIONSHIPS_KEY = "relationships";
    private final List<Relationship> relationships = new ArrayList<>();

    private static final PersistentStateType<RelationshipManager> TYPE =
            new PersistentStateType<>(STATE_ID, RelationshipManager::new,
                    RelationshipManager::fromNbt, null);

    private RelationshipManager() {}

    public static RelationshipManager get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not available");
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    public Relationship getOrCreate(UUID npcId, UUID playerId) {
        return relationships.stream()
                .filter(r -> r.npcId().equals(npcId) && r.playerId().equals(playerId))
                .findFirst()
                .orElseGet(() -> {
                    Relationship created = new Relationship(npcId, playerId, 0, 0, 0, 0, 0, 0, 0);
                    relationships.add(created);
                    markDirty();
                    return created;
                });
    }

    public void apply(MemoryEvent event) {
        Relationship current = getOrCreate(event.npcId(), event.playerId());
        RelationshipDelta delta = switch (event.type()) {
            case PLAYER_GAVE_BREAD -> RelationshipDelta.breadGift();
        };

        Relationship updated = new Relationship(
                current.npcId(), current.playerId(),
                current.trust() + delta.trust(),
                current.gratitude() + delta.gratitude(),
                current.fear() + delta.fear(),
                current.respect() + delta.respect(),
                current.affection() + delta.affection(),
                current.resentment() + delta.resentment(),
                current.suspicion() + delta.suspicion()
        ).clamp();

        relationships.remove(current);
        relationships.add(updated);
        markDirty();
    }

    private static RelationshipManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RelationshipManager state = new RelationshipManager();
        NbtList list = nbt.getList(RELATIONSHIPS_KEY, NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound e = (NbtCompound) element;
            state.relationships.add(new Relationship(
                    UUID.fromString(e.getString("npc")),
                    UUID.fromString(e.getString("player")),
                    e.getInt("trust"), e.getInt("gratitude"), e.getInt("fear"),
                    e.getInt("respect"), e.getInt("affection"),
                    e.getInt("resentment"), e.getInt("suspicion")
            ));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (Relationship r : relationships) {
            NbtCompound e = new NbtCompound();
            e.putString("npc", r.npcId().toString());
            e.putString("player", r.playerId().toString());
            e.putInt("trust", r.trust());
            e.putInt("gratitude", r.gratitude());
            e.putInt("fear", r.fear());
            e.putInt("respect", r.respect());
            e.putInt("affection", r.affection());
            e.putInt("resentment", r.resentment());
            e.putInt("suspicion", r.suspicion());
            list.add(e);
        }
        nbt.put(RELATIONSHIPS_KEY, list);
        return nbt;
    }
}
