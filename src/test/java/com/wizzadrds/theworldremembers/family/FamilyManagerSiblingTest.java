package com.wizzadrds.theworldremembers.family;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FamilyManagerSiblingTest {
    @Test
    void sharedParentCreatesSiblingLinks() {
        FamilyManager manager = new FamilyManager();
        UUID parent = UUID.randomUUID();
        UUID older = UUID.randomUUID();
        UUID younger = UUID.randomUUID();

        assertTrue(manager.addParentChild(parent, older));
        assertTrue(manager.addParentChild(parent, younger));

        manager.linkSiblingsFromSharedParent(younger);

        assertTrue(manager.siblingsOf(younger).contains(older));
        assertTrue(manager.siblingsOf(older).contains(younger));
    }

    @Test
    void childIsNotItsOwnSibling() {
        FamilyManager manager = new FamilyManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        manager.addParentChild(parent, child);
        manager.linkSiblingsFromSharedParent(child);

        assertFalse(manager.siblingsOf(child).contains(child));
    }
}
