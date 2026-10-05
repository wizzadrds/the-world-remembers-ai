package com.wizzadrds.theworldremembers.age;

import org.junit.jupiter.api.Test;
import com.mojang.serialization.JsonOps;
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
    void ageAdvancesOnlyAfterOneMinecraftWeek() {
        NpcAgeManager manager = new NpcAgeManager();
        UUID id = UUID.randomUUID();
        manager.assignIfAbsent(id, 20);
        assertFalse(manager.advanceIfDue(id, 100_000L));
        assertEquals(20, manager.get(id).years());
        assertTrue(manager.advanceIfDue(id, 168_000L));
        assertEquals(21, manager.get(id).years());
    }

    @Test
    void agingCalendarRoundTripsThroughCodec() {
        NpcAgeManager manager = new NpcAgeManager();
        UUID id = UUID.randomUUID();
        manager.assignIfAbsent(id, 20);
        assertTrue(manager.advanceIfDue(id, 168_000L));

        var encoded = NpcAgeManager.CODEC.encodeStart(JsonOps.INSTANCE, manager).getOrThrow();
        NpcAgeManager restored = NpcAgeManager.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(21, restored.get(id).years());
        assertFalse(restored.advanceIfDue(id, 168_001L));
        assertTrue(restored.advanceIfDue(id, 336_000L));
        assertEquals(22, restored.get(id).years());
    }

    @Test
    void ageStopsAtSixty() {
        NpcAgeManager manager = new NpcAgeManager();
        UUID id = UUID.randomUUID();
        manager.assignIfAbsent(id, 60);
        assertFalse(manager.advanceIfDue(id, 1_000_000L));
        assertEquals(60, manager.get(id).years());
    }

    @Test
    void invalidAgeIsRejected() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new NpcAge(id, 3));
        assertThrows(IllegalArgumentException.class, () -> new NpcAge(id, 61));
    }
}
