package com.wizzadrds.theworldremembers.home;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import java.util.Optional;
import java.util.UUID;

public record NpcHome(UUID npcId, BlockPos homePos, BlockPos bedPos, BlockPos entrancePos) {
    public static final Codec<NpcHome> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("npc").forGetter(NpcHome::npcId),
            BlockPos.CODEC.fieldOf("home").forGetter(NpcHome::homePos),
            BlockPos.CODEC.optionalFieldOf("bed").forGetter(h -> Optional.ofNullable(h.bedPos())),
            BlockPos.CODEC.optionalFieldOf("entrance").forGetter(h -> Optional.ofNullable(h.entrancePos()))
    ).apply(i, (npc, home, bed, entrance) -> new NpcHome(npc, home, bed.orElse(null), entrance.orElse(null))));
    public NpcHome { if (npcId == null || homePos == null) throw new IllegalArgumentException("npcId and homePos are required"); }
}
