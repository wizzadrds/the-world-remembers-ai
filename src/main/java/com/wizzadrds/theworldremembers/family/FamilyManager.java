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
    public boolean add(FamilyRelation r){
        if (relations.contains(r)) return false;
        if (hasConflictingPair(r.npcId(), r.relatedNpcId(), r.type())) return false;
        relations.add(r); setDirty(); return true;
    }
    public boolean addParentChild(UUID parent, UUID child) {
        return add(new FamilyRelation(parent, child, FamilyRelationType.PARENT))
            && add(new FamilyRelation(child, parent, FamilyRelationType.CHILD));
    }
    public boolean addSpouses(UUID first, UUID second) {
        return add(new FamilyRelation(first, second, FamilyRelationType.SPOUSE))
            && add(new FamilyRelation(second, first, FamilyRelationType.SPOUSE));
    }
    public boolean addSiblings(UUID first, UUID second) {
        return add(new FamilyRelation(first, second, FamilyRelationType.SIBLING))
            && add(new FamilyRelation(second, first, FamilyRelationType.SIBLING));
    }
    private boolean hasConflictingPair(UUID a, UUID b, FamilyRelationType type) {
        return relations.stream().anyMatch(existing ->
            ((existing.npcId().equals(a) && existing.relatedNpcId().equals(b)) ||
             (existing.npcId().equals(b) && existing.relatedNpcId().equals(a))) && !compatible(existing.type(), type));
    }
    private boolean compatible(FamilyRelationType existing, FamilyRelationType incoming) {
        return existing == incoming ||
            (existing == FamilyRelationType.PARENT && incoming == FamilyRelationType.CHILD) ||
            (existing == FamilyRelationType.CHILD && incoming == FamilyRelationType.PARENT);
    }
    public List<FamilyRelation> getRelations(UUID id){return relations.stream().filter(r->r.npcId().equals(id)||r.relatedNpcId().equals(id)).toList();}
    public List<UUID> parentsOf(UUID child) { return relations.stream().filter(r -> r.relatedNpcId().equals(child) && r.type() == FamilyRelationType.PARENT).map(FamilyRelation::npcId).toList(); }
    public List<UUID> childrenOf(UUID parent) { return relations.stream().filter(r -> r.npcId().equals(parent) && r.type() == FamilyRelationType.PARENT).map(FamilyRelation::relatedNpcId).toList(); }
    public boolean hasParents(UUID child) { return !parentsOf(child).isEmpty(); }
    public boolean hasSpouse(UUID npcId) { return relations.stream().anyMatch(r -> r.npcId().equals(npcId) && r.type() == FamilyRelationType.SPOUSE); }
    public boolean areRelated(UUID a,UUID b){return relations.stream().anyMatch(r->(r.npcId().equals(a)&&r.relatedNpcId().equals(b))||(r.npcId().equals(b)&&r.relatedNpcId().equals(a)));}
}
