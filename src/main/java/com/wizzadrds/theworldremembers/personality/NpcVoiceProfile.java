package com.wizzadrds.theworldremembers.personality;

import java.util.UUID;

public record NpcVoiceProfile(
        UUID npcId,
        VoiceTemperament temperament,
        int pitch,
        int speakingRate,
        int expressiveness
) {
    public NpcVoiceProfile {
        if (npcId == null) {
            throw new IllegalArgumentException("npcId cannot be null");
        }
        pitch = clamp(pitch);
        speakingRate = clamp(speakingRate);
        expressiveness = clamp(expressiveness);
    }

    private static int clamp(int value) {
        return Math.max(-100, Math.min(100, value));
    }
}
