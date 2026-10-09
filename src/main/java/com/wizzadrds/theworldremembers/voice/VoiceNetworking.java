package com.wizzadrds.theworldremembers.voice;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VoiceNetworking {
    private static final float MAX_DISTANCE = 64.0f;
    private static final int MAX_FRAMES_PER_WINDOW = 25;
    private static final long RATE_WINDOW_TICKS = 10;
    /** Keep the nearest villager attentive while the player is actively speaking. */
    private static final long VOICE_FOCUS_HOLD_TICKS = 20;
    private static final long VOICE_FOCUS_REFRESH_TICKS = 2;
    private static final double VOICE_FOCUS_RADIUS = 8.0;
    private static final Map<UUID, RateState> RATE_LIMITS = new HashMap<>();
    private static final Map<UUID, FocusState> VOICE_FOCUS = new HashMap<>();

    private VoiceNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(VoicePacket.TYPE, VoicePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VillagerVoicePacket.TYPE, VillagerVoicePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoiceAudioPacket.TYPE, VoiceAudioPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VoiceAudioPacket.TYPE, VoiceAudioPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VillagerConversationFocusPacket.TYPE, VillagerConversationFocusPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VillagerNamePacket.TYPE, VillagerNamePacket.CODEC);

        // Keep an explicitly focused villager stopped and facing the player every server tick.
        // The client sends the focus-on packet before STT/AI/TTS and the focus-off packet only
        // after playback finishes, so the villager cannot resume its normal goals mid-reply.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var entry : VOICE_FOCUS.entrySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                FocusState focus = entry.getValue();
                if (player == null || focus.villager == null || server.isStopped()) continue;
                Villager villager = player.level().getEntity(focus.villager) instanceof Villager v ? v : null;
                if (villager != null && villager.isAlive() && villager.distanceToSqr(player) <= 12.0 * 12.0
                        && focus.untilTick > player.level().getGameTime()) {
                    holdVillagerFocus(player, villager);
                } else if (focus.untilTick <= player.level().getGameTime()) {
                    focus.villager = null;
                }
            }
        });

        // A disconnected player must not leave rate-limit state behind forever.
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            UUID playerId = listener.getPlayer().getUUID();
            RATE_LIMITS.remove(playerId);
            VOICE_FOCUS.remove(playerId);
        });

        ServerPlayNetworking.registerGlobalReceiver(VillagerNamePacket.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            String rawName = payload.name() == null ? "" : payload.name().trim();
            String name = rawName.replaceAll("[^\\p{L}\\p{M}'’ -]", "").replaceAll("\\s+", " ");
            if (name.isBlank() || name.length() > 24) return;
            context.server().execute(() -> {
                Villager villager = sender.level().getEntity(payload.villager()) instanceof Villager v ? v : null;
                if (villager == null || !villager.isAlive() || villager.distanceToSqr(sender) > 12.0 * 12.0) return;
                if (villager.hasCustomName() && villager.getCustomName() != null
                        && !villager.getCustomName().getString().equalsIgnoreCase(name)) return;
                villager.setCustomName(net.minecraft.network.chat.Component.literal(name));
                villager.setCustomNameVisible(true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(VillagerConversationFocusPacket.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            context.server().execute(() -> {
                Villager villager = sender.level().getEntity(payload.villager()) instanceof Villager v ? v : null;
                if (villager == null || !villager.isAlive() || villager.distanceToSqr(sender) > 12.0 * 12.0) return;
                FocusState focus = VOICE_FOCUS.computeIfAbsent(sender.getUUID(), ignored -> new FocusState());
                if (payload.active()) {
                    focus.villager = villager.getUUID();
                    focus.untilTick = Long.MAX_VALUE;
                    focus.nextRefreshTick = 0;
                    holdVillagerFocus(sender, villager);
                } else if (villager.getUUID().equals(focus.villager)) {
                    focus.untilTick = sender.level().getGameTime();
                }
            });
        });

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
            keepNearestVillagerAttentive(sender, now);

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

    private static void keepNearestVillagerAttentive(ServerPlayer player, long now) {
        UUID playerId = player.getUUID();
        FocusState focus = VOICE_FOCUS.computeIfAbsent(playerId, ignored -> new FocusState());
        if (focus.villager != null && now < focus.untilTick) {
            Villager focused = player.level().getEntity(focus.villager) instanceof Villager v ? v : null;
            if (focused != null && focused.isAlive()) {
                holdVillagerFocus(player, focused);
                return;
            }
        }
        if (now < focus.nextRefreshTick) return;
        focus.nextRefreshTick = now + VOICE_FOCUS_REFRESH_TICKS;

        Villager nearest = null;
        double nearestDistance = VOICE_FOCUS_RADIUS * VOICE_FOCUS_RADIUS;
        for (Villager villager : player.level().getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(VOICE_FOCUS_RADIUS),
                candidate -> candidate.isAlive() && !candidate.isRemoved())) {
            double distance = villager.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = villager;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return;

        // Interrupt the current path and make the villager look at the speaking player.
        // Explicit conversation focus is refreshed every server tick until TTS playback ends.
        nearest.getNavigation().stop();
        nearest.getLookControl().setLookAt(player, 30.0f, 30.0f);
        focus.villager = nearest.getUUID();
        focus.untilTick = now + VOICE_FOCUS_HOLD_TICKS;
    }

    private static final class FocusState {
        private UUID villager;
        private long nextRefreshTick;
        private long untilTick;
    }

    private static void holdVillagerFocus(ServerPlayer player, Villager villager) {
        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(player, 30.0f, 30.0f);
    }

    private static final class RateState {
        private long windowStart;
        private int frames;
        private RateState(long windowStart) { this.windowStart = windowStart; }
    }
}
