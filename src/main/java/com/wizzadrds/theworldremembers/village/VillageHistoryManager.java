package com.wizzadrds.theworldremembers.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

public final class VillageHistoryManager extends SavedData {
 private final Map<UUID,VillageHistory> histories=new HashMap<>();
 private static final Codec<VillageHistory> HISTORY=RecordCodecBuilder.create(i->i.group(
  Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("villageId").forGetter(VillageHistory::villageId),Codec.LONG.fieldOf("firstObservedTick").forGetter(VillageHistory::firstObservedTick),Codec.LONG.fieldOf("lastObservedTick").forGetter(VillageHistory::lastObservedTick),Codec.INT.fieldOf("peakPopulation").forGetter(VillageHistory::peakPopulation),Codec.INT.fieldOf("totalImportantEvents").forGetter(VillageHistory::totalImportantEvents)).apply(i,VillageHistory::new));
 private static final Codec<VillageHistoryManager> CODEC=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString,UUID::toString),HISTORY).xmap(m->{var x=new VillageHistoryManager();x.histories.putAll(m);return x;},x->x.histories);
 private static final SavedDataType<VillageHistoryManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","village_history"),VillageHistoryManager::new,CODEC,null);
 public static VillageHistoryManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new VillageHistoryManager():l.getDataStorage().computeIfAbsent(TYPE);}
 public VillageHistory observe(UUID id,int population,long tick){var old=histories.get(id);var h=old==null?new VillageHistory(id,tick,tick,population,0):old.observe(population,tick);if(!h.equals(old)){histories.put(id,h);setDirty();}return h;}
 public VillageHistory recordImportantEvent(UUID id){var h=histories.get(id);if(h==null)return null;h=h.event();histories.put(id,h);setDirty();return h;}
 public VillageHistory get(UUID id){return histories.get(id);}
 public Collection<VillageHistory> all(){return List.copyOf(histories.values());}
}
