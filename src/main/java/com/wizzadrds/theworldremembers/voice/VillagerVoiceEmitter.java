package com.wizzadrds.theworldremembers.voice;

import com.wizzadrds.theworldremembers.personality.PersonalityGenerator;
import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.rumor.GroundedDialogue;
import com.wizzadrds.theworldremembers.rumor.KnowledgeFact;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class VillagerVoiceEmitter {
    private static final long SPEECH_COOLDOWN_TICKS = 100;
    private static final float MAX_DISTANCE = 32.0f;
    private static final Map<ServerLevel, Map<UUID, Long>> LAST_SPEECH = new WeakHashMap<>();

    private VillagerVoiceEmitter() {}

    public static void speakKnowledge(ServerLevel world, Villager villager, KnowledgeFact fact, long gameTime) {
        if (fact == null) return;
        String line = naturalize(GroundedDialogue.render(fact));
        if (line.isBlank()) return;
        speak(world, villager, line, gameTime);
    }

    public static void speak(ServerLevel world, Villager villager, String line, long gameTime) {
        if (line == null || line.isBlank() || villager == null || !villager.isAlive()) return;
        Map<UUID, Long> levelState = LAST_SPEECH.computeIfAbsent(world, ignored -> new java.util.HashMap<>());
        long last = levelState.getOrDefault(villager.getUUID(), Long.MIN_VALUE);
        if (gameTime - last < SPEECH_COOLDOWN_TICKS) return;

        boolean recipientFound = false;
        for (ServerPlayer player : world.players()) {
            if (!player.isAlive() || player.distanceToSqr(villager) > MAX_DISTANCE * MAX_DISTANCE) continue;
            VoiceNetworking.send(player, packetFor(villager, line));
            recipientFound = true;
        }
        if (recipientFound) levelState.put(villager.getUUID(), gameTime);
    }

    private static VoicePacket packetFor(Villager villager, String line) {
        PersonalityProfile personality = PersonalityGenerator.generate(villager.getUUID());
        float social = personality.strength(com.wizzadrds.theworldremembers.personality.PersonalityTrait.SOCIAL) / 100.0f;
        float calm = personality.strength(com.wizzadrds.theworldremembers.personality.PersonalityTrait.CALM) / 100.0f;
        float rate = clamp(0.82f + social * 0.20f, 0.65f, 1.15f);
        float pitch = clamp(0.90f + (social - calm) * 0.14f, 0.78f, 1.20f);
        float expressiveness = clamp(0.35f + social * 0.45f, 0.20f, 0.90f);
        return new VoicePacket(line, villager.getX(), villager.getY() + 1.5, villager.getZ(),
                MAX_DISTANCE, 0.90f, rate, pitch, expressiveness, 1);
    }

    private static String naturalize(String line) {
        String normalized = line.trim();
        if (normalized.length() > 240) normalized = normalized.substring(0, 237) + "...";
        return normalized;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
