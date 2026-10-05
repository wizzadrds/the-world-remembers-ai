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

public final class FamilyProtectionManager extends SavedData {
    private final Map<UUID, UUID> protectors = new HashMap<>();
    private static final Codec<FamilyProtectionManager> CODEC = Codec.unboundedMap(
        Codec.STRING.xmap(UUID::fromString, UUID::toString),
        Codec.STRING.xmap(UUID::fromString, UUID::toString)
    ).xmap(map -> {
        FamilyProtectionManager manager = new FamilyProtectionManager();
        manager.protectors.putAll(map);
        return manager;
    }, manager -> manager.protectors);
    private static final SavedDataType<FamilyProtectionManager> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("the_world_remembers", "family_protection"),
        FamilyProtectionManager::new,
        CODEC,
        null
    );

    public static FamilyProtectionManager get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        return level == null ? new FamilyProtectionManager() : level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void protect(UUID protector, UUID familyMember) {
        protectors.put(familyMember, protector);
        setDirty();
    }

    public UUID protectorOf(UUID familyMember) {
        return protectors.get(familyMember);
    }

    public void clear(UUID familyMember) {
        if (protectors.remove(familyMember) != null) setDirty();
    }
}
