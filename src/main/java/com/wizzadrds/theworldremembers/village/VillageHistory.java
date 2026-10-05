package com.wizzadrds.theworldremembers.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.world.level.saveddata.SavedData;

public record VillageHistory(UUID villageId, long firstObservedTick, long lastObservedTick, int peakPopulation, int totalImportantEvents) {
 public VillageHistory { if(firstObservedTick<0||lastObservedTick<firstObservedTick||peakPopulation<0||totalImportantEvents<0) throw new IllegalArgumentException(); }
 public VillageHistory observe(int population,long tick){return new VillageHistory(villageId,firstObservedTick,tick,Math.max(peakPopulation,population),totalImportantEvents);}
 public VillageHistory event(){return new VillageHistory(villageId,firstObservedTick,lastObservedTick,peakPopulation,totalImportantEvents+1);}
}
