package com.wizzadrds.theworldremembers.voice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record VillagerConversationFocusPacket(UUID villager, boolean active) implements CustomPacketPayload {
    public static final Type<VillagerConversationFocusPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("the_world_remembers", "villager_conversation_focus"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerConversationFocusPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), VillagerConversationFocusPacket::villager,
                    ByteBufCodecs.BOOL, VillagerConversationFocusPacket::active,
                    VillagerConversationFocusPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
