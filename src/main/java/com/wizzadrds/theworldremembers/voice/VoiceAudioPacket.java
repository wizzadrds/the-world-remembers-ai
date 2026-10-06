package com.wizzadrds.theworldremembers.voice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record VoiceAudioPacket(
        UUID speaker, double x, double y, double z, float volume,
        float maxDistance, int sequence, byte[] pcm) implements CustomPacketPayload {
    public static final Type<VoiceAudioPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("the_world_remembers", "voice_audio"));

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC =
            StreamCodec.of(
                    (buf, value) -> { buf.writeLong(value.getMostSignificantBits()); buf.writeLong(value.getLeastSignificantBits()); },
                    buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceAudioPacket> CODEC =
            StreamCodec.composite(
                    UUID_CODEC, VoiceAudioPacket::speaker,
                    ByteBufCodecs.DOUBLE, VoiceAudioPacket::x,
                    ByteBufCodecs.DOUBLE, VoiceAudioPacket::y,
                    ByteBufCodecs.DOUBLE, VoiceAudioPacket::z,
                    ByteBufCodecs.FLOAT, VoiceAudioPacket::volume,
                    ByteBufCodecs.FLOAT, VoiceAudioPacket::maxDistance,
                    ByteBufCodecs.VAR_INT, VoiceAudioPacket::sequence,
                    ByteBufCodecs.byteArray(4096), VoiceAudioPacket::pcm,
                    VoiceAudioPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
