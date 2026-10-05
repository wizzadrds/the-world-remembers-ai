package com.wizzadrds.theworldremembers.personality;

import java.util.UUID;

/**
 * Creates a stable personality from the NPC UUID.
 * The same villager always receives the same profile after reload.
 */
public final class PersonalityGenerator {
    private PersonalityGenerator() {}

    public static PersonalityProfile generate(UUID npcId) {
        long state = npcId.getMostSignificantBits() ^ Long.rotateLeft(npcId.getLeastSignificantBits(), 21);
        PersonalityProfile profile = new PersonalityProfile(npcId);

        PersonalityTrait[] traits = PersonalityTrait.values();
        for (int i = 0; i < traits.length; i++) {
            state = mix(state + 0x9E3779B97F4A7C15L + i);
            int value = (int) Math.floorMod(state, 81) + 20; // 20..100
            profile.set(traits[i], value);
        }

        // Keep personalities readable: three dominant traits, the rest moderate.
        for (int i = 3; i < traits.length; i++) {
            state = mix(state + i);
            if ((state & 1L) == 0L) {
                profile.set(traits[i], 0);
            }
        }

        return profile;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
