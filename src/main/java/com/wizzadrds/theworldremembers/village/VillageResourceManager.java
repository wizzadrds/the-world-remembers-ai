package com.wizzadrds.theworldremembers.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

public final class VillageResourceManager extends SavedData {
 private final Map<UUID,VillageResources> resources=new HashMap<>();
 private static final Codec<VillageResources> RES=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("food").forGetter(VillageResources::food),Codec.INT.fieldOf("valuableItems").forGetter(VillageResources::valuableItems),Codec.INT.fieldOf("occupiedStorage").forGetter(VillageResources::occupiedStorage),Codec.INT.fieldOf("storageCapacity").forGetter(VillageResources::storageCapacity)).apply(i,VillageResources::new));
 private static final Codec<VillageResourceManager> CODEC=Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString,UUID::toString),RES).xmap(m->{var x=new VillageResourceManager();x.resources.putAll(m);return x;},x->x.resources);
 private static final SavedDataType<VillageResourceManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","village_resources"),VillageResourceManager::new,CODEC,null);
 public static VillageResourceManager get(MinecraftServer s){ServerLevel l=s.getLevel(ServerLevel.OVERWORLD);return l==null?new VillageResourceManager():l.getDataStorage().computeIfAbsent(TYPE);}
 public void observe(UUID id,VillageResources value){resources.put(id,value);setDirty();}
 public VillageResources get(UUID id){return resources.get(id);}
 public Collection<VillageResources> all(){return List.copyOf(resources.values());}
}
