package com.wizzadrds.theworldremembers.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public record Memory(UUID npcId, UUID playerId, MemoryEventType type, long gameTime, MemoryImportance importance, MemoryOrigin origin) {
    public Memory(UUID npcId, UUID playerId, MemoryEventType type, long gameTime, MemoryImportance importance) {
        this(npcId, playerId, type, gameTime, importance, MemoryOrigin.DIRECT);
    }
    public static final Codec<Memory> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("npc").forGetter(Memory::npcId),
            UUIDUtil.CODEC.fieldOf("player").forGetter(Memory::playerId),
            Codec.STRING.xmap(MemoryEventType::valueOf, MemoryEventType::name).fieldOf("type").forGetter(Memory::type),
            Codec.LONG.fieldOf("time").forGetter(Memory::gameTime),
            Codec.STRING.xmap(MemoryImportance::valueOf, MemoryImportance::name).fieldOf("importance").forGetter(Memory::importance),
            Codec.STRING.xmap(MemoryOrigin::valueOf, MemoryOrigin::name).optionalFieldOf("origin", MemoryOrigin.DIRECT).forGetter(Memory::origin)
    ).apply(i, Memory::new));

    public String summary() {
        return switch (type) {
            case PLAYER_GAVE_BREAD -> "you gave me bread.";
            default -> type.name().toLowerCase().replace('_', ' ') + ".";
        };
    }
}
