package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TravelDialogueContextTest {
    private final UUID npc = UUID.randomUUID();

    @Test
    void shortTravelProducesNoTravelComplaint() {
        PersonalityProfile personality = new PersonalityProfile(npc);
        assertNull(new TravelDialogueContext(personality, 32, 200, 0).line());
    }

    @Test
    void longTravelProducesGenericQuestion() {
        PersonalityProfile personality = new PersonalityProfile(npc);
        assertEquals("Are we there yet?",
                new TravelDialogueContext(personality, 128, 1200, 0).line());
    }

    @Test
    void stressOverridesNormalTravelLine() {
        PersonalityProfile personality = new PersonalityProfile(npc);
        assertEquals("Can we go back soon? I don't like being this far from home.",
                new TravelDialogueContext(personality, 64, 500, 80).line());
    }

    @Test
    void adventurousNpcGetsDifferentLongTravelLine() {
        PersonalityProfile personality = new PersonalityProfile(npc)
                .with(PersonalityTrait.ADVENTUROUS, 80);
        assertEquals("This is far from home... but I want to see what's ahead.",
                new TravelDialogueContext(personality, 256, 2400, 0).line());
    }
}
