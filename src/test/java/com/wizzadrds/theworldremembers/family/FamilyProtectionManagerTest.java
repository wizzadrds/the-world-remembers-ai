package com.wizzadrds.theworldremembers.family;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FamilyProtectionManagerTest {
    @Test
    void protectionAssignmentPersistsInManagerState() {
        FamilyProtectionManager manager = new FamilyProtectionManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        manager.protect(parent, child);

        assertEquals(parent, manager.protectorOf(child));
    }
}
