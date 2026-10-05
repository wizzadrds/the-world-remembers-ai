package com.wizzadrds.theworldremembers.family;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FamilyManagerSpouseTest {
    @Test
    void spouseLookupReturnsDirectSpouse() {
        FamilyManager manager = new FamilyManager();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        manager.addSpouses(first, second);
        assertEquals(second, manager.spouseOf(first));
        assertEquals(first, manager.spouseOf(second));
    }
}
