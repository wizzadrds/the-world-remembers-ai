package com.wizzadrds.theworldremembers.home;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record NpcHome(UUID npcId, BlockPos homePos, BlockPos bedPos, BlockPos entrancePos) {
    public NpcHome {
        if (npcId == null || homePos == null) {
            throw new IllegalArgumentException("npcId and homePos are required");
        }
    }
}
