package com.wizzadrds.theworldremembers.family;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FamilyRelationTest {
    @Test
    void selfRelationIsRejected() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new FamilyRelation(id, id, FamilyRelationType.SIBLING));
    }

    @Test
    void relationKeepsExplicitType() {
        FamilyRelation relation = new FamilyRelation(
                UUID.randomUUID(), UUID.randomUUID(), FamilyRelationType.PARENT);
        assertEquals(FamilyRelationType.PARENT, relation.type());
    }
}
