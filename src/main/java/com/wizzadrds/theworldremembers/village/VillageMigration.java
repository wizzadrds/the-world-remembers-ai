package com.wizzadrds.theworldremembers.village;
import net.minecraft.core.BlockPos; import java.util.UUID;
public record VillageMigration(UUID villageId, BlockPos origin, BlockPos destination, long detectedTick, int populationMoved) { public VillageMigration { if(populationMoved<0) throw new IllegalArgumentException(); } }
