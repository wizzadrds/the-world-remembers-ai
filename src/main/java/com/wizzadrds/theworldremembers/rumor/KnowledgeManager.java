package com.wizzadrds.theworldremembers.rumor;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

public final class KnowledgeManager extends SavedData {
    private final Map<UUID, List<KnowledgeFact>> data = new HashMap<>();
    private transient Map<UUID,List<KnowledgeFact>> factsIndex;
    private static final Codec<KnowledgeManager> C = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            KnowledgeFact.CODEC.listOf())
            .xmap(m -> {
                var x = new KnowledgeManager();
                x.data.putAll(m);
                return x;
            }, x -> x.data);

    private static final SavedDataType<KnowledgeManager> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("the_world_remembers", "knowledge"),
            KnowledgeManager::new, C, null);

    public static KnowledgeManager get(MinecraftServer s) {
        ServerLevel l = s.getLevel(ServerLevel.OVERWORLD);
        return l == null ? new KnowledgeManager() : l.getDataStorage().computeIfAbsent(TYPE);
    }

    public void learn(UUID npc, KnowledgeFact fact) {
        var list = data.computeIfAbsent(npc, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            KnowledgeFact existing = list.get(i);
            if (!existing.subject().equals(fact.subject()) || existing.eventType() != fact.eventType()) continue;
            if (existing.equals(fact)) return;
            list.set(i, fact);
            factsIndex = null;
            setDirty();
            return;
        }
        list.add(fact);
        if (list.size() > 32) list.remove(0);
        factsIndex = null;
        setDirty();
    }

    public List<KnowledgeFact> facts(UUID npc) {
        if (factsIndex == null) {
            factsIndex = new HashMap<>();
            data.forEach((id, list) -> factsIndex.put(id, List.copyOf(list)));
        }
        return factsIndex.getOrDefault(npc, List.of());
    }

    public void decay(long tick) {
        boolean dirty = false;
        for (var list : data.values()) {
            for (int i = 0; i < list.size(); i++) {
                var old = list.get(i);
                var next = old.degrade(tick);
                if (next.confidence() != old.confidence()) {
                    list.set(i, next);
                    dirty = true;
                    factsIndex = null;
                }
            }
        }
        if (dirty) setDirty();
    }
}
