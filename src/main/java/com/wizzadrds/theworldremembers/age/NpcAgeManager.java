package com.wizzadrds.theworldremembers.age;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcAgeManager extends SavedData {
    private final Map<UUID, NpcAge> ages = new HashMap<>();
    private static final Codec<NpcAge> AGE_CODEC = RecordCodecBuilder.create(i -> i.group(UUIDUtil.CODEC.fieldOf("npc").forGetter(NpcAge::npcId), Codec.INT.fieldOf("years").forGetter(NpcAge::years)).apply(i, NpcAge::new));
    private static final Codec<NpcAgeManager> CODEC = Codec.unboundedMap(UUIDUtil.CODEC, AGE_CODEC).xmap(map -> { NpcAgeManager m = new NpcAgeManager(); m.ages.putAll(map); return m; }, m -> m.ages);
    private static final SavedDataType<NpcAgeManager> TYPE = new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "npc_ages"), NpcAgeManager::new, CODEC, null);
    public static NpcAgeManager get(MinecraftServer server) { ServerLevel level = server.getLevel(ServerLevel.OVERWORLD); return level == null ? new NpcAgeManager() : level.getDataStorage().computeIfAbsent(TYPE); }
    public NpcAge get(UUID npcId) { return ages.get(npcId); }
    public NpcAge assignIfAbsent(UUID npcId, int years) { NpcAge age = ages.computeIfAbsent(npcId, id -> new NpcAge(id, years)); setDirty(); return age; }
    public boolean hasAge(UUID npcId) { return ages.containsKey(npcId); }
}
