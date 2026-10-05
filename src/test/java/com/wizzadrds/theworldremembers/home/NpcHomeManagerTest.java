package com.wizzadrds.theworldremembers.home;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcHomeManagerTest {
    @Test
    void familyMemberReceivesSameHomeState() {
        NpcHomeManager manager = new NpcHomeManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        BlockPos home = new BlockPos(10, 64, 10);
        BlockPos bed = new BlockPos(11, 64, 10);
        BlockPos entrance = new BlockPos(10, 64, 11);

        manager.assignIfAbsent(parent, home, bed, entrance);
        manager.assignFamilyHome(child, parent);

        assertEquals(home, manager.get(child).homePos());
        assertEquals(bed, manager.get(child).bedPos());
        assertEquals(entrance, manager.get(child).entrancePos());
    }

    @Test
    void missingFamilyHomeDoesNotInventOne() {
        NpcHomeManager manager = new NpcHomeManager();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        manager.assignFamilyHome(child, parent);

        assertFalse(manager.hasHome(child));
    }
}
