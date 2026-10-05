package com.wizzadrds.theworldremembers.family;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class FamilyManager extends SavedData {
    private final List<FamilyRelation> relations=new ArrayList<>();
    private static final SavedDataType<FamilyManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","families"),FamilyManager::new,null);
    public static FamilyManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new FamilyManager():l.getDataStorage().computeIfAbsent(TYPE);}
    public boolean add(FamilyRelation r){if(relations.contains(r))return false;relations.add(r);setDirty();return true;}
    public List<FamilyRelation> getRelations(UUID id){return relations.stream().filter(r->r.npcId().equals(id)||r.relatedNpcId().equals(id)).toList();}
    public boolean areRelated(UUID a,UUID b){return relations.stream().anyMatch(r->(r.npcId().equals(a)&&r.relatedNpcId().equals(b))||(r.npcId().equals(b)&&r.relatedNpcId().equals(a)));}
}
