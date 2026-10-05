package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;

public record TravelDialogueContext(
        PersonalityProfile personality,
        int distanceFromHome,
        int travelTicks,
        int stress
) {
    public String line() {
        if (stress >= 75) return "Can we go back soon? I don't like being this far from home.";

        if (distanceFromHome >= 256 || travelTicks >= 2400) {
            if (personality.has(PersonalityTrait.ADVENTUROUS, 60)) {
                return "This is far from home... but I want to see what's ahead.";
            }
            if (personality.has(PersonalityTrait.COWARDLY, 60)) {
                return "Are we there yet? We're really far from home.";
            }
            if (personality.has(PersonalityTrait.HARD_WORKING, 60)) {
                return "Are we much longer? I should be getting back to work.";
            }
            if (personality.has(PersonalityTrait.FAMILY_ORIENTED, 60)) {
                return "We should head back soon. They'll wonder where I am.";
            }
            return "We're still far from home, aren't we?";
        }

        if (distanceFromHome >= 128 || travelTicks >= 1200) {
            return "Are we there yet?";
        }

        return null;
    }
}
