package com.wizzadrds.theworldremembers.memory;
import java.util.UUID;
public record Memory(UUID npcId, UUID playerId, MemoryEventType type, long gameTime, MemoryImportance importance) {
    public String summary() {
        return switch (type) {
            case PLAYER_GAVE_BREAD -> "you gave me bread.";
        };
    }
}
