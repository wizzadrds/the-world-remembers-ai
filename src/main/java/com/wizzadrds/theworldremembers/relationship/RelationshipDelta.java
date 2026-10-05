package com.wizzadrds.theworldremembers.relationship;

import com.wizzadrds.theworldremembers.memory.MemoryEventType;

public record RelationshipDelta(
        int trust,
        int gratitude,
        int fear,
        int respect,
        int affection,
        int resentment,
        int suspicion
) {
    public static RelationshipDelta breadGift() {
        return new RelationshipDelta(2, 8, 0, 1, 2, -1, -2);
    }

    public static RelationshipDelta threatened() {
        return new RelationshipDelta(-10, 0, 15, -2, -2, 15, 10);
    }

    public static RelationshipDelta attacked() {
        return new RelationshipDelta(-15, 0, 20, -5, -5, 25, 15);
    }

    public static RelationshipDelta saved() {
        return new RelationshipDelta(10, 20, -5, 8, 5, -5, -5);
    }

    public static RelationshipDelta golemKilled() {
        return new RelationshipDelta(-25, 0, 25, -10, -10, 30, 25);
    }

    public static RelationshipDelta forEvent(MemoryEventType type) {
        return switch (type) {
            case PLAYER_GAVE_BREAD -> breadGift();
            case PLAYER_THREATENED_NPC -> threatened();
            case PLAYER_ATTACKED_NPC -> attacked();
            case PLAYER_SAVED_NPC -> saved();
            case GOLEM_KILLED -> golemKilled();
            default -> new RelationshipDelta(0, 0, 0, 0, 0, 0, 0);
        };
    }
}
