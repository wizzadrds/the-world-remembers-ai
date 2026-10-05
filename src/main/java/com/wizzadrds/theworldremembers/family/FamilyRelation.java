package com.wizzadrds.theworldremembers.family;

import java.util.UUID;

public record FamilyRelation(UUID npcId, UUID relatedNpcId, FamilyRelationType type) {
    public FamilyRelation {
        if (npcId == null || relatedNpcId == null || type == null) {
            throw new IllegalArgumentException("Family relation fields are required");
        }
        if (npcId.equals(relatedNpcId)) {
            throw new IllegalArgumentException("An NPC cannot be related to itself");
        }
    }
}
