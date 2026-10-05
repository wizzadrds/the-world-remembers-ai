package com.wizzadrds.theworldremembers.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

public final class VillageManager extends SavedData {
 private final Map<UUID,VillageState> villages=new HashMap<>();
 private static final Codec<VillageState> STATE=RecordCodecBuilder.create(i->i.group(
  Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("villageId").forGetter(VillageState::villageId),
  BlockPos.CODEC.fieldOf("center").forGetter(VillageState::center), Codec.INT.fieldOf("population").forGetter(VillageState::population),
  Codec.LONG.fieldOf("firstObservedTick").forGetter(VillageState::firstObservedTick), Codec.LONG.fieldOf("lastObservedTick").forGetter(VillageState::lastObservedTick)).apply(i,VillageState::new));
 private static final Codec<VillageManager> CODEC=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString,UUID::toString),STATE).xmap(m->{var v=new VillageManager();v.villages.putAll(m);return v;},v->v.villages);
 private static final SavedDataType<VillageManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","villages"),VillageManager::new,CODEC,null);
 public static VillageManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new VillageManager():l.getDataStorage().computeIfAbsent(TYPE);}
 public VillageState observe(UUID id,BlockPos center,int population,long tick){var old=villages.get(id);var s=old==null?new VillageState(id,center,population,tick,tick):old.observe(center,population,tick);villages.put(id,s);setDirty();return s;}\n public VillageState observeNearest(BlockPos center,int population,long tick,Set<UUID> claimed){VillageState nearest=null;double best=64D*64D;for(var state:villages.values()){if(claimed.contains(state.villageId()))continue;double d=state.center().distSqr(center);if(d<=best){best=d;nearest=state;}}UUID id=nearest==null?UUID.randomUUID():nearest.villageId();claimed.add(id);return observe(id,center,population,tick);}
 public VillageState get(UUID id){return villages.get(id);}
 public Collection<VillageState> all(){return List.copyOf(villages.values());}
}
