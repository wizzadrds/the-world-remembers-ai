package com.wizzadrds.theworldremembers.personality;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PersonalityProfile {
    private final UUID npcId;
    private final Map<PersonalityTrait, Integer> traits = new EnumMap<>(PersonalityTrait.class);

    public PersonalityProfile(UUID npcId) {
        this.npcId = npcId;
        for (PersonalityTrait trait : PersonalityTrait.values()) {
            traits.put(trait, 0);
        }
    }

    public UUID npcId() {
        return npcId;
    }

    public int strength(PersonalityTrait trait) {
        return traits.getOrDefault(trait, 0);
    }

    public void set(PersonalityTrait trait, int value) {
        traits.put(trait, Math.max(-100, Math.min(100, value)));
    }

    public PersonalityProfile with(PersonalityTrait trait, int value) {
        set(trait, value);
        return this;
    }
}
