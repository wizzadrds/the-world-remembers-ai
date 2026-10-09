package com.wizzadrds.theworldremembers.voice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record VillagerMemoryContextPacket(UUID villager, String context) implements CustomPacketPayload {
    public static final Type<VillagerMemoryContextPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("the_world_remembers", "villager_memory_context"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerMemoryContextPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), VillagerMemoryContextPacket::villager,
                    ByteBufCodecs.STRING_UTF8, VillagerMemoryContextPacket::context,
                    VillagerMemoryContextPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
