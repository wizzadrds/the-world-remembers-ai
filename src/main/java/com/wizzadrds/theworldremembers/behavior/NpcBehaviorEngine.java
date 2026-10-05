package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.relationship.Relationship;

public final class NpcBehaviorEngine {
    public NpcDecision decide(NpcActivity activity,
                              Relationship relationship,
                              PersonalityProfile personality) {
        if (activity == NpcActivity.FLEEING || activity == NpcActivity.SLEEPING) {
            return NpcDecision.IGNORE_PLAYER;
        }

        int resentment = relationship.resentment();
        int fear = relationship.fear();
        int affection = relationship.affection();
        int social = personality.strength(PersonalityTrait.SOCIAL);
        int hardWorking = personality.strength(PersonalityTrait.HARD_WORKING);

        if (resentment >= 40 || fear >= 70) {
            return NpcDecision.LEAVE;
        }

        if (activity == NpcActivity.WORKING && hardWorking >= 50) {
            return relationship.gratitude() >= 20
                    ? NpcDecision.ACKNOWLEDGE_PLAYER
                    : NpcDecision.IGNORE_PLAYER;
        }

        if (activity == NpcActivity.IDLE || activity == NpcActivity.SOCIALIZING) {
            if (affection >= 20 || social >= 50 || relationship.gratitude() >= 20) {
                return NpcDecision.TALK;
            }
            return NpcDecision.ACKNOWLEDGE_PLAYER;
        }

        return NpcDecision.ACKNOWLEDGE_PLAYER;
    }
}
