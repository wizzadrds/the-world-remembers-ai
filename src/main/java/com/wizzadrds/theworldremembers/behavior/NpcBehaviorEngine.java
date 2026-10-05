package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.relationship.Relationship;
import com.wizzadrds.theworldremembers.stress.NpcStress;
import com.wizzadrds.theworldremembers.family.FamilyProtectionManager;

public final class NpcBehaviorEngine {
    public NpcDecision decideFamilyResponse(boolean protectingFamily, boolean dangerPresent, NpcStress stress) {
        if (!protectingFamily) return NpcDecision.IGNORE_PLAYER;
        if (dangerPresent) return NpcDecision.CALL_FOR_HELP;
        if (stress.isCritical()) return NpcDecision.RETURN_HOME;
        return NpcDecision.FOLLOW;
    }

    public NpcDecision decide(NpcActivity activity,
                              Relationship relationship,
                              PersonalityProfile personality) {
        return decide(activity, relationship, personality, new NpcStress(0));
    }

    public NpcDecision decide(NpcActivity activity,
                              Relationship relationship,
                              PersonalityProfile personality,
                              NpcStress stress) {
        if (activity == NpcActivity.SLEEPING) {
            return NpcDecision.IGNORE_PLAYER;
        }

        int resentment = relationship.resentment();
        int fear = relationship.fear();
        int affection = relationship.affection();
        int social = personality.strength(PersonalityTrait.SOCIAL);
        int hardWorking = personality.strength(PersonalityTrait.HARD_WORKING);
        int brave = personality.strength(PersonalityTrait.BRAVE);
        int cowardly = personality.strength(PersonalityTrait.COWARDLY);
        int hotTempered = personality.strength(PersonalityTrait.HOT_TEMPERED);
        int suspicious = personality.strength(PersonalityTrait.SUSPICIOUS);
        int protective = personality.strength(PersonalityTrait.PROTECTIVE);

        if (stress.isCritical()) {
            return NpcDecision.LEAVE;
        }

        if (activity == NpcActivity.FLEEING) {
            return NpcDecision.CALL_FOR_HELP;
        }

        if (resentment >= 40 || fear >= 70) {
            return NpcDecision.LEAVE;
        }

        if (hotTempered >= 70 && resentment >= 20) {
            return NpcDecision.LEAVE;
        }

        if (cowardly >= 70 && fear >= 30) {
            return NpcDecision.LEAVE;
        }

        if (stress.isHighlyStressed() && relationship.trust() < 50) {
            return NpcDecision.LEAVE;
        }

        if (protective >= 70 && fear < 50 && affection >= 20 && relationship.trust() >= 30) {
            return NpcDecision.FOLLOW;
        }

        if (suspicious >= 70 && relationship.suspicion() >= 20) {
            return NpcDecision.ACKNOWLEDGE_PLAYER;
        }

        if (activity == NpcActivity.WORKING && hardWorking >= 50) {
            return relationship.gratitude() >= 20
                    ? NpcDecision.ACKNOWLEDGE_PLAYER
                    : NpcDecision.IGNORE_PLAYER;
        }

        if (activity == NpcActivity.IDLE || activity == NpcActivity.SOCIALIZING) {
            if (brave >= 70 && fear < 30 && relationship.respect() >= 20 && relationship.trust() >= 30) {
                return NpcDecision.FOLLOW;
            }
            if (affection >= 20 || social >= 50 || relationship.gratitude() >= 20) {
                return NpcDecision.TALK;
            }
            return NpcDecision.ACKNOWLEDGE_PLAYER;
        }

        return NpcDecision.ACKNOWLEDGE_PLAYER;
    }
}
