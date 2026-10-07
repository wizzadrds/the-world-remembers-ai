package com.wizzadrds.theworldremembers.inventory;

import com.mojang.serialization.JsonOps;
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

    @Test
    void invalidTransferInDoesNotCreateInventoryState() {
        NpcInventoryManager manager = new NpcInventoryManager();
        UUID owner = UUID.randomUUID();

        assertEquals(0, manager.transferIn(owner, "minecraft:diamond", 0));
        assertEquals(0, manager.transferIn(owner, "", 3));
        assertEquals(0, manager.transferIn(owner, null, 3));
        assertEquals(0, manager.count(owner, "minecraft:diamond"));

        var encoded = NpcInventoryManager.CODEC.encodeStart(JsonOps.INSTANCE, manager)
                .resultOrPartial(message -> fail("Codec encode failed: " + message))
                .orElseThrow();
        assertTrue(encoded.toString().isEmpty() || encoded.toString().equals("{}"));
    }

    @Test
    void missingInventoryReadsAndTransfersDoNotCreateState() {
        NpcInventoryManager manager = new NpcInventoryManager();
        UUID owner = UUID.randomUUID();

        assertEquals(0, manager.count(owner, "minecraft:diamond"));
        assertEquals(0, manager.transferOut(owner, "minecraft:diamond", 1));
        assertEquals(0, manager.inheritImportantItems(owner, UUID.randomUUID()));

        var encoded = NpcInventoryManager.CODEC.encodeStart(JsonOps.INSTANCE, manager)
                .resultOrPartial(message -> fail("Codec encode failed: " + message))
                .orElseThrow();
        assertTrue(encoded.toString().isEmpty() || encoded.toString().equals("{}"));
    }

    @Test
    void savedCodecRoundTripsCurrentInventoryFormat() {
        NpcInventoryManager manager = new NpcInventoryManager();
        UUID owner = UUID.randomUUID();
        manager.transferIn(owner, "minecraft:diamond", 4);
        manager.transferIn(owner, "minecraft:bread", 7);

        var encoded = NpcInventoryManager.CODEC.encodeStart(JsonOps.INSTANCE, manager)
                .resultOrPartial(message -> fail("Codec encode failed: " + message))
                .orElseThrow();
        NpcInventoryManager restored = NpcInventoryManager.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> fail("Codec decode failed: " + message))
                .orElseThrow();

        assertEquals(4, restored.count(owner, "minecraft:diamond"));
        assertEquals(7, restored.count(owner, "minecraft:bread"));
    }

    @Test
    void savedCodecStillReadsLegacyInventoryFormat() {
        UUID owner = UUID.randomUUID();
        NpcInventoryManager legacy = new NpcInventoryManager();
        legacy.transferIn(owner, "minecraft:emerald", 2);

        var legacyEncoded = com.mojang.serialization.Codec.unboundedMap(
                com.mojang.serialization.Codec.STRING.xmap(UUID::fromString, UUID::toString),
                NpcInventoryManager.INVENTORY_CODEC
        ).encodeStart(JsonOps.INSTANCE, java.util.Map.of(
                owner, legacy.getOrCreate(owner)
        )).resultOrPartial(message -> fail("Legacy encode failed: " + message)).orElseThrow();

        NpcInventoryManager restored = NpcInventoryManager.CODEC.parse(JsonOps.INSTANCE, legacyEncoded)
                .resultOrPartial(message -> fail("Legacy decode failed: " + message))
                .orElseThrow();

        assertEquals(2, restored.count(owner, "minecraft:emerald"));
    }
}
