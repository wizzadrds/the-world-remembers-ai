package com.wizzadrds.theworldremembers.voice;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VoiceNetworking {
    private static final float MAX_DISTANCE = 64.0f;
    private static final int MAX_FRAMES_PER_WINDOW = 25;
    private static final long RATE_WINDOW_TICKS = 10;
    private static final Map<UUID, RateState> RATE_LIMITS = new HashMap<>();

    private VoiceNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(VoicePacket.TYPE, VoicePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VillagerVoicePacket.TYPE, VillagerVoicePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoiceAudioPacket.TYPE, VoiceAudioPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VoiceAudioPacket.TYPE, VoiceAudioPacket.CODEC);

        // A disconnected player must not leave rate-limit state behind forever.
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) ->
                RATE_LIMITS.remove(listener.getPlayer().getUUID()));

        ServerPlayNetworking.registerGlobalReceiver(VoiceAudioPacket.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            byte[] pcm = payload.pcm();
            if (pcm.length == 0 || pcm.length > 4096 || (pcm.length & 1) != 0) return;
            if (payload.sequence() < 0) return;
            if (!Float.isFinite(payload.maxDistance()) || !Float.isFinite(payload.volume())) return;

            long now = sender.level().getGameTime();
            RateState state = RATE_LIMITS.computeIfAbsent(sender.getUUID(), ignored -> new RateState(now));
            if (now - state.windowStart >= RATE_WINDOW_TICKS) {
                state.windowStart = now;
                state.frames = 0;
            }
            if (++state.frames > MAX_FRAMES_PER_WINDOW) return;

            float distance = Math.max(1.0f, Math.min(MAX_DISTANCE, payload.maxDistance()));
            float volume = Math.max(0.0f, Math.min(2.0f, payload.volume()));
            VoiceAudioPacket relay = new VoiceAudioPacket(
                    sender.getUUID(), sender.getX(), sender.getY() + sender.getEyeHeight(),
                    sender.getZ(), volume, distance, payload.sequence(), pcm);

            double radiusSquared = distance * distance;
            for (ServerPlayer recipient : sender.level().players()) {
                if (recipient == sender || !recipient.isAlive()) continue;
                if (recipient.distanceToSqr(sender) > radiusSquared) continue;
                if (ServerPlayNetworking.canSend(recipient, VoiceAudioPacket.TYPE)) {
                    ServerPlayNetworking.send(recipient, relay);
                }
            }
        });
    }

    public static void send(ServerPlayer player, VillagerVoicePacket packet) {
        if (ServerPlayNetworking.canSend(player, VillagerVoicePacket.TYPE)) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public static void send(ServerPlayer player, VoicePacket packet) {
        if (ServerPlayNetworking.canSend(player, VoicePacket.TYPE)) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    private static final class RateState {
        private long windowStart;
        private int frames;
        private RateState(long windowStart) { this.windowStart = windowStart; }
    }
}
