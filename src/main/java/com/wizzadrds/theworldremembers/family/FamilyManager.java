package com.wizzadrds.theworldremembers.family;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

public final class FamilyManager extends SavedData {
    private final List<FamilyRelation> relations=new ArrayList<>();
    private static final Codec<FamilyRelation> RELATION_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("npc").forGetter(FamilyRelation::npcId),Codec.STRING.xmap(UUID::fromString,UUID::toString).fieldOf("related").forGetter(FamilyRelation::relatedNpcId),Codec.STRING.xmap(FamilyRelationType::valueOf,FamilyRelationType::name).fieldOf("type").forGetter(FamilyRelation::type)).apply(i,FamilyRelation::new));
    private static final Codec<FamilyManager> CODEC=RELATION_CODEC.listOf().xmap(list->{FamilyManager m=new FamilyManager();m.relations.addAll(list);return m;},m->m.relations);
    private static final SavedDataType<FamilyManager> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("the_world_remembers","families"),FamilyManager::new,CODEC,null);
    public static FamilyManager get(MinecraftServer server){ServerLevel l=server.getLevel(ServerLevel.OVERWORLD);return l==null?new FamilyManager():l.getDataStorage().computeIfAbsent(TYPE);}
    public boolean add(FamilyRelation r){if(relations.contains(r))return false;relations.add(r);setDirty();return true;}
    public List<FamilyRelation> getRelations(UUID id){return relations.stream().filter(r->r.npcId().equals(id)||r.relatedNpcId().equals(id)).toList();}
    public boolean areRelated(UUID a,UUID b){return relations.stream().anyMatch(r->(r.npcId().equals(a)&&r.relatedNpcId().equals(b))||(r.npcId().equals(b)&&r.relatedNpcId().equals(a)));}
}
