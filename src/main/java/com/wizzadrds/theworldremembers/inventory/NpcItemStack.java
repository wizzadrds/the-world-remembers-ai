package com.wizzadrds.theworldremembers.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NpcItemStack(String itemId, int count) {
    public static final Codec<NpcItemStack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("item").forGetter(NpcItemStack::itemId),
            Codec.INT.fieldOf("count").forGetter(NpcItemStack::count)
    ).apply(i, NpcItemStack::new));
    public NpcItemStack { if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId cannot be blank"); if (count < 1) throw new IllegalArgumentException("count must be positive"); }
}
