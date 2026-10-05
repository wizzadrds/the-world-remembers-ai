package com.wizzadrds.theworldremembers.relationship;

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
}
