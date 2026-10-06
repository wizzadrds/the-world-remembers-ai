package com.wizzadrds.theworldremembers.inventory;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class NpcInventoryTest { @Test void hasEightBoundedSlots(){var i=new NpcInventory(); for(int n=0;n<8;n++) assertTrue(i.add("minecraft:item_"+n,64)); assertEquals(0,i.freeSlots()); assertFalse(i.add("minecraft:extra",1));} @Test void stacksToSixtyFour(){var i=new NpcInventory(); assertTrue(i.add("minecraft:bread",64)); assertTrue(i.add("minecraft:bread",64)); assertEquals(128,i.count("minecraft:bread")); assertEquals(2,i.usedSlots());} }
