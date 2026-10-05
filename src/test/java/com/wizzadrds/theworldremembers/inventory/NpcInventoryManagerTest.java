package com.wizzadrds.theworldremembers.inventory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcInventoryManagerTest {
    @Test
    void onlyDesignatedImportantItemsAreInherited() {
        NpcInventoryManager manager = new NpcInventoryManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        manager.transferIn(parent, "minecraft:diamond", 2);
        manager.transferIn(parent, "minecraft:bread", 8);

        assertEquals(2, manager.inheritImportantItems(parent, child));
        assertEquals(2, manager.count(child, "minecraft:diamond"));
        assertEquals(0, manager.count(child, "minecraft:bread"));
        assertEquals(0, manager.count(parent, "minecraft:diamond"));
        assertEquals(8, manager.count(parent, "minecraft:bread"));
    }

    @Test
    void inheritanceDoesNotDuplicateOnSecondTransfer() {
        NpcInventoryManager manager = new NpcInventoryManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        manager.transferIn(parent, "minecraft:emerald", 3);
        assertEquals(3, manager.inheritImportantItems(parent, child));
        assertEquals(0, manager.inheritImportantItems(parent, child));
        assertEquals(3, manager.count(child, "minecraft:emerald"));
    }
}
