package com.wizzadrds.theworldremembers.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.wizzadrds.theworldremembers.memory.MemoryEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RelationshipManager extends SavedData {
    private final Map<String, Relationship> relationships = new HashMap<>();
    private static final Codec<Map<String, Relationship>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Relationship.CODEC);
    private static final Codec<RelationshipManager> CODEC = MAP_CODEC.xmap(m -> { RelationshipManager x=new RelationshipManager(); x.relationships.putAll(m); return x; }, x -> x.relationships);
    private static final SavedDataType<RelationshipManager> TYPE = new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "relationships"), RelationshipManager::new, CODEC, null);
    private static String key(UUID npc, UUID player) { return npc + ":" + player; }
    public static RelationshipManager get(MinecraftServer server) { ServerLevel l=server.getLevel(ServerLevel.OVERWORLD); return l==null?new RelationshipManager():l.getDataStorage().computeIfAbsent(TYPE); }
    public Collection<Relationship> all(){return List.copyOf(relationships.values());}
    public Relationship get(UUID npc, UUID player) { return relationships.get(key(npc, player)); }
    public Relationship getOrCreate(UUID npc, UUID player) { return relationships.computeIfAbsent(key(npc,player), k -> new Relationship(npc,player,0,0,0,0,0,0,0)); }
    public Relationship apply(MemoryEvent event) {
        Relationship current=getOrCreate(event.npcId(),event.playerId());
        RelationshipDelta d=RelationshipDelta.forEvent(event.type());
        Relationship next=new Relationship(current.npcId(),current.playerId(),
            current.trust()+d.trust(),current.gratitude()+d.gratitude(),current.fear()+d.fear(),
            current.respect()+d.respect(),current.affection()+d.affection(),current.resentment()+d.resentment(),current.suspicion()+d.suspicion()).clamp();
        relationships.put(key(event.npcId(),event.playerId()),next); setDirty(); return next;
    }
}
