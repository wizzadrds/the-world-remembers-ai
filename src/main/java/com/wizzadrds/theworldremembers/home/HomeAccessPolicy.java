package com.wizzadrds.theworldremembers.home;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.relationship.Relationship;

public final class HomeAccessPolicy {
    private HomeAccessPolicy() {}

    public static HomeAccess evaluate(Relationship relationship, PersonalityProfile personality, boolean invited) {
        if (invited) return HomeAccess.ALLOWED;

        if (relationship.resentment() >= 40 || relationship.suspicion() >= 60) {
            return HomeAccess.DENIED;
        }

        if (relationship.trust() >= 50
                || relationship.affection() >= 45
                || (personality.has(PersonalityTrait.SOCIAL, 70) && relationship.trust() >= 30)) {
            return HomeAccess.ALLOWED;
        }

        if (relationship.trust() >= 20 && relationship.fear() < 40) {
            return HomeAccess.CONDITIONAL;
        }

        return HomeAccess.DENIED;
    }
}
