package com.wizzadrds.theworldremembers.age;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NpcAgeManagerTest {
    @Test
    void ageBoundariesAreMeaningful() {
        UUID id = UUID.randomUUID();
        assertTrue(new NpcAge(id, 4).isChild());
        assertTrue(new NpcAge(id, 13).isChild());
        assertTrue(new NpcAge(id, 18).isAdult());
        assertTrue(new NpcAge(id, 60).isAdult());
    }

    @Test
    void invalidAgeIsRejected() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new NpcAge(id, 3));
        assertThrows(IllegalArgumentException.class, () -> new NpcAge(id, 61));
    }
}
