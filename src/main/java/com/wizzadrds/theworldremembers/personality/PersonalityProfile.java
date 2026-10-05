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

    public boolean has(PersonalityTrait trait, int threshold) {
        return strength(trait) >= threshold;
    }

    public boolean isStrongly(PersonalityTrait trait) {
        return Math.abs(strength(trait)) >= 60;
    }

    public PersonalityProfile copy() {
        PersonalityProfile copy = new PersonalityProfile(npcId);
        for (PersonalityTrait trait : PersonalityTrait.values()) {
            copy.set(trait, strength(trait));
        }
        return copy;
    }
}
