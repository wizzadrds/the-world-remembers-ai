package com.wizzadrds.theworldremembers.home;

import com.wizzadrds.theworldremembers.personality.PersonalityProfile;
import com.wizzadrds.theworldremembers.personality.PersonalityTrait;
import com.wizzadrds.theworldremembers.relationship.Relationship;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeAccessPolicyTest {
    private final UUID npc = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void hostileRelationshipDeniesEntry() {
        Relationship relationship = new Relationship(npc, player, 80, 0, 0, 20, 20, 50, 70);
        assertEquals(HomeAccess.DENIED,
                HomeAccessPolicy.evaluate(relationship, new PersonalityProfile(npc), false));
    }

    @Test
    void trustedRelationshipAllowsEntry() {
        Relationship relationship = new Relationship(npc, player, 60, 0, 0, 20, 50, 0, 0);
        assertEquals(HomeAccess.ALLOWED,
                HomeAccessPolicy.evaluate(relationship, new PersonalityProfile(npc), false));
    }

    @Test
    void explicitInvitationAllowsEntry() {
        Relationship relationship = new Relationship(npc, player, -50, 0, 20, 0, 0, 60, 60);
        assertEquals(HomeAccess.ALLOWED,
                HomeAccessPolicy.evaluate(relationship, new PersonalityProfile(npc), true));
    }

    @Test
    void socialNpcCanAllowTrustedFriend() {
        PersonalityProfile personality = new PersonalityProfile(npc)
                .with(PersonalityTrait.SOCIAL, 80);
        Relationship relationship = new Relationship(npc, player, 35, 0, 0, 0, 0, 0, 0);
        assertEquals(HomeAccess.ALLOWED,
                HomeAccessPolicy.evaluate(relationship, personality, false));
    }
}
