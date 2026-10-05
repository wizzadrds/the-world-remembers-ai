package com.wizzadrds.theworldremembers.personality;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersonalityGeneratorTest {
    @Test
    void sameNpcGetsSamePersonality() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PersonalityProfile a = PersonalityGenerator.generate(id);
        PersonalityProfile b = PersonalityGenerator.generate(id);

        for (PersonalityTrait trait : PersonalityTrait.values()) {
            assertEquals(a.strength(trait), b.strength(trait), trait.name());
            assertTrue(a.strength(trait) >= -100 && a.strength(trait) <= 100);
        }
    }

    @Test
    void differentNpcsCanHaveDifferentProfiles() {
        PersonalityProfile a = PersonalityGenerator.generate(
                UUID.fromString("11111111-1111-1111-1111-111111111111"));
        PersonalityProfile b = PersonalityGenerator.generate(
                UUID.fromString("22222222-2222-2222-2222-222222222222"));

        assertTrue(PersonalityTrait.values().length > 1);
        assertTrue(java.util.Arrays.stream(PersonalityTrait.values())
                .anyMatch(trait -> a.strength(trait) != b.strength(trait)));
    }
}
