package com.wizzadrds.theworldremembers.voice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record VillagerNamePacket(UUID villager, String name) implements CustomPacketPayload {
    public static final Type<VillagerNamePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("the_world_remembers", "villager_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerNamePacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), VillagerNamePacket::villager,
                    ByteBufCodecs.STRING_UTF8, VillagerNamePacket::name,
                    VillagerNamePacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
