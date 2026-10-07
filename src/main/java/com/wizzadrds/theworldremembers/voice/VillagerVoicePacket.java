package com.wizzadrds.theworldremembers.voice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record VillagerVoicePacket(
        UUID speaker,
        String profession,
        String text,
        double x,
        double y,
        double z,
        float maxDistance,
        float volume,
        float rate,
        float pitch,
        float expressiveness,
        int priority
) implements CustomPacketPayload {
    public static final Type<VillagerVoicePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("the_world_remembers", "villager_voice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerVoicePacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.UUID, VillagerVoicePacket::speaker,
                    ByteBufCodecs.STRING_UTF8, VillagerVoicePacket::profession,
                    ByteBufCodecs.STRING_UTF8, VillagerVoicePacket::text,
                    ByteBufCodecs.DOUBLE, VillagerVoicePacket::x,
                    ByteBufCodecs.DOUBLE, VillagerVoicePacket::y,
                    ByteBufCodecs.DOUBLE, VillagerVoicePacket::z,
                    ByteBufCodecs.FLOAT, VillagerVoicePacket::maxDistance,
                    ByteBufCodecs.FLOAT, VillagerVoicePacket::volume,
                    ByteBufCodecs.FLOAT, VillagerVoicePacket::rate,
                    ByteBufCodecs.FLOAT, VillagerVoicePacket::pitch,
                    ByteBufCodecs.FLOAT, VillagerVoicePacket::expressiveness,
                    ByteBufCodecs.VAR_INT, VillagerVoicePacket::priority,
                    VillagerVoicePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
