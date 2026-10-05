package com.wizzadrds.theworldremembers.relationship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public record Relationship(UUID npcId, UUID playerId, int trust, int gratitude, int fear, int respect, int affection, int resentment, int suspicion) {
    public static final Codec<Relationship> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("npc").forGetter(Relationship::npcId),
            UUIDUtil.CODEC.fieldOf("player").forGetter(Relationship::playerId),
            Codec.INT.fieldOf("trust").forGetter(Relationship::trust),
            Codec.INT.fieldOf("gratitude").forGetter(Relationship::gratitude),
            Codec.INT.fieldOf("fear").forGetter(Relationship::fear),
            Codec.INT.fieldOf("respect").forGetter(Relationship::respect),
            Codec.INT.fieldOf("affection").forGetter(Relationship::affection),
            Codec.INT.fieldOf("resentment").forGetter(Relationship::resentment),
            Codec.INT.fieldOf("suspicion").forGetter(Relationship::suspicion)
    ).apply(i, Relationship::new));
    public Relationship clamp() { return new Relationship(npcId, playerId, clamp(trust), clamp(gratitude), clamp(fear), clamp(respect), clamp(affection), clamp(resentment), clamp(suspicion)); }
    public boolean isTrusted() { return trust >= 40; }
    public boolean isAfraid() { return fear >= 40; }
    public boolean isHostile() { return resentment >= 40 || suspicion >= 50; }
    public boolean hasProtectiveBond() { return affection >= 40 && gratitude >= 30 && fear < 50; }
    private static int clamp(int value) { return Math.max(-100, Math.min(100, value)); }
}
