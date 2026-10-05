package com.wizzadrds.theworldremembers.relationship;

import com.wizzadrds.theworldremembers.memory.MemoryEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDeltaTest {
    @Test
    void homeIntrusionHasSocialConsequences() {
        RelationshipDelta delta = RelationshipDelta.forEvent(MemoryEventType.PLAYER_ENTERED_NPC_HOME);
        assertTrue(delta.trust() < 0);
        assertTrue(delta.resentment() > 0);
        assertTrue(delta.suspicion() > 0);
        assertTrue(delta.fear() > 0);
    }
}
