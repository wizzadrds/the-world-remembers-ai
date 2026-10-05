package com.wizzadrds.theworldremembers.memory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {
    @Test
    void importantFamilyHistoryIsInheritedWithoutChangingItsOrigin() {
        MemoryManager manager = new MemoryManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID subject = UUID.randomUUID();

        manager.rememberEvent(parent, subject, MemoryEventType.NPC_DIED, 120L, MemoryImportance.IMPORTANT);
        assertEquals(1, manager.inheritFamilyHistory(parent, child, 200L));

        Memory inherited = manager.memoriesOf(child).stream()
            .filter(m -> m.type() == MemoryEventType.NPC_DIED)
            .findFirst()
            .orElseThrow();

        assertEquals(subject, inherited.playerId());
        assertEquals(120L, inherited.gameTime());
        assertEquals(MemoryOrigin.INHERITED, inherited.origin());
    }

    @Test
    void inheritedHistoryDoesNotReinheritItself() {
        MemoryManager manager = new MemoryManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID subject = UUID.randomUUID();

        manager.rememberEvent(parent, subject, MemoryEventType.NPC_MARRIED, 10L, MemoryImportance.IMPORTANT);
        assertEquals(1, manager.inheritFamilyHistory(parent, child, 20L));
        assertEquals(0, manager.inheritFamilyHistory(child, UUID.randomUUID(), 30L));
    }
}
