package com.wizzadrds.theworldremembers.village;

import net.minecraft.core.BlockPos;
import java.util.UUID;

public record VillageState(UUID villageId, BlockPos center, int population, long firstObservedTick, long lastObservedTick) {
    public VillageState { if (population < 0) throw new IllegalArgumentException("population"); }
    public VillageState observe(BlockPos newCenter, int newPopulation, long tick) {
        return new VillageState(villageId, newCenter, Math.max(0,newPopulation), firstObservedTick, tick);
    }
}
