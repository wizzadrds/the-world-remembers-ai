package com.wizzadrds.theworldremembers.memory;
import org.junit.jupiter.api.Test; import java.util.UUID; import static org.junit.jupiter.api.Assertions.*;
class MemoryItemTest { @Test void itemIdentityPersistsInMemory(){var m=new Memory(UUID.randomUUID(),UUID.randomUUID(),MemoryEventType.PLAYER_GAVE_ITEM,10,MemoryImportance.IMPORTANT,MemoryOrigin.DIRECT,java.util.Optional.of("minecraft:diamond"));assertEquals("minecraft:diamond",m.itemId().orElseThrow());assertTrue(m.summary().contains("minecraft:diamond"));} }
