package com.wizzadrds.theworldremembers.memory;

import java.util.UUID;

/**
 * Immutable world event that can become an NPC memory.
 */
public record MemoryEvent(
        UUID npcId,
        UUID playerId,
        MemoryEventType type,
        long gameTime,
        MemoryImportance importance
) {
    public Memory toMemory() {
        return new Memory(npcId, playerId, type, gameTime, importance);
    }
}
