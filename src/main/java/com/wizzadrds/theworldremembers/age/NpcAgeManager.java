package com.wizzadrds.theworldremembers.age;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcAgeManager extends SavedData {
    private static final long TICKS_PER_YEAR = 168_000L;
    private final Map<UUID, NpcAge> ages = new HashMap<>();
    private final Map<UUID, Long> lastAgeTick = new HashMap<>();
    private record AgeState(NpcAge age, long lastAgeTick) {}
    private static final Codec<AgeState> AGE_STATE_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("npc").forGetter(s -> s.age().npcId()),
        Codec.INT.fieldOf("years").forGetter(s -> s.age().years()),
        Codec.LONG.optionalFieldOf("lastAgeTick", 0L).forGetter(AgeState::lastAgeTick)
    ).apply(i, (npc, years, tick) -> new AgeState(new NpcAge(npc, years), tick)));
    static final Codec<NpcAgeManager> CODEC = Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), AGE_STATE_CODEC).xmap(map -> {
        NpcAgeManager m = new NpcAgeManager();
        map.forEach((id, state) -> { m.ages.put(id, state.age()); m.lastAgeTick.put(id, state.lastAgeTick()); });
        return m;
    }, m -> {
        Map<UUID, AgeState> states = new HashMap<>();
        m.ages.forEach((id, age) -> states.put(id, new AgeState(age, m.lastAgeTick.getOrDefault(id, 0L))));
        return states;
    });
    private static final SavedDataType<NpcAgeManager> TYPE = new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("the_world_remembers", "npc_ages"), NpcAgeManager::new, CODEC, null);
    public static NpcAgeManager get(MinecraftServer server) { ServerLevel level = server.getLevel(ServerLevel.OVERWORLD); return level == null ? new NpcAgeManager() : level.getDataStorage().computeIfAbsent(TYPE); }
    public NpcAge get(UUID npcId) { return ages.get(npcId); }
    public NpcAge assignIfAbsent(UUID npcId, int years) {
        NpcAge age = ages.get(npcId);
        boolean changed = false;
        if (age == null) {
            age = new NpcAge(npcId, years);
            ages.put(npcId, age);
            changed = true;
        }
        if (!lastAgeTick.containsKey(npcId)) {
            lastAgeTick.put(npcId, 0L);
            changed = true;
        }
        if (changed) setDirty();
        return age;
    }
    public boolean hasAge(UUID npcId) { return ages.containsKey(npcId); }
    public boolean advanceIfDue(UUID npcId, long gameTime) {
        NpcAge age = ages.get(npcId);
        if (age == null || age.years() >= 60) return false;
        long last = lastAgeTick.getOrDefault(npcId, gameTime);
        if (gameTime - last < TICKS_PER_YEAR) return false;
        int years = Math.min(60, age.years() + (int)((gameTime-last) / TICKS_PER_YEAR));
        ages.put(npcId, new NpcAge(npcId, years));
        lastAgeTick.put(npcId, last + ((gameTime-last) / TICKS_PER_YEAR) * TICKS_PER_YEAR);
        setDirty();
        return years != age.years();
    }
}