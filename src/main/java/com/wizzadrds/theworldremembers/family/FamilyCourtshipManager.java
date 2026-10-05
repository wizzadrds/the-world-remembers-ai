package com.wizzadrds.theworldremembers.family;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FamilyCourtshipManager extends SavedData {
    private static final int MAX_PROGRESS = 1200;
    private final Map<String, Integer> progress = new HashMap<>();
    private static final Codec<FamilyCourtshipManager> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(map -> {
        FamilyCourtshipManager manager = new FamilyCourtshipManager();
        manager.progress.putAll(map);
        return manager;
    }, manager -> manager.progress);
    private static final SavedDataType<FamilyCourtshipManager> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("the_world_remembers", "family_courtship"),
        FamilyCourtshipManager::new,
        CODEC,
        null
    );

    public static FamilyCourtshipManager get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        return level == null ? new FamilyCourtshipManager() : level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int advance(UUID first, UUID second, int ticks) {
        String key = key(first, second);
        int next = Math.min(MAX_PROGRESS, progress.getOrDefault(key, 0) + Math.max(0, ticks));
        progress.put(key, next);
        setDirty();
        return next;
    }

    public void clear(UUID first, UUID second) {
        if (progress.remove(key(first, second)) != null) setDirty();
    }

    public int progress(UUID first, UUID second) {
        return progress.getOrDefault(key(first, second), 0);
    }

    public boolean ready(UUID first, UUID second) {
        return progress(first, second) >= MAX_PROGRESS;
    }

    private static String key(UUID first, UUID second) {
        return first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
    }
}
