package com.wizzadrds.theworldremembers.family;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class FamilyProtectionManagerTest {
    @Test void protectionAssignmentPersistsInManagerState() {
        FamilyProtectionManager manager = new FamilyProtectionManager();
        UUID parent = UUID.randomUUID(), child = UUID.randomUUID();
        manager.protect(parent, child);
        assertEquals(parent, manager.protectorOf(child));
    }
    @Test void clearProtectorReleasesProtectedMembers() {
        FamilyProtectionManager manager = new FamilyProtectionManager();
        UUID parent = UUID.randomUUID(), child = UUID.randomUUID();
        manager.protect(parent, child);
        manager.clearProtector(parent);
        assertNull(manager.protectorOf(child));
    }
}
