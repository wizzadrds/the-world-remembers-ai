package com.wizzadrds.theworldremembers.voice;

import java.util.UUID;

/**
 * Stable voice identity for an NPC.
 *
 * Voice is derived from personality and state rather than being one generic
 * narrator. The actual TTS backend is selected later by the voice adapter.
 */
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

    public boolean isTimid() {
        return temperament == VoiceTemperament.TIMID;
    }

    public boolean isAssertive() {
        return temperament == VoiceTemperament.ASSERTIVE;
    }

    private static int clamp(int value) {
        return Math.max(-100, Math.min(100, value));
    }
}
