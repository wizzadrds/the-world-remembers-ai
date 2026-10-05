package com.wizzadrds.theworldremembers.relationship;

import java.util.UUID;

/**
 * NPC perception of a player. Values are deliberately separate:
 * trust, gratitude, fear, respect, affection, resentment and suspicion
 * can evolve independently.
 */
public record Relationship(
        UUID npcId,
        UUID playerId,
        int trust,
        int gratitude,
        int fear,
        int respect,
        int affection,
        int resentment,
        int suspicion
) {
    public Relationship clamp() {
        return new Relationship(
                npcId, playerId,
                clamp(trust), clamp(gratitude), clamp(fear), clamp(respect),
                clamp(affection), clamp(resentment), clamp(suspicion)
        );
    }

    private static int clamp(int value) {
        return Math.max(-100, Math.min(100, value));
    }
}
