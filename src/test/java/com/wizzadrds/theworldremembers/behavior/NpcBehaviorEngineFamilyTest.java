package com.wizzadrds.theworldremembers.behavior;

import com.wizzadrds.theworldremembers.stress.NpcStress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcBehaviorEngineFamilyTest {
    @Test
    void familyProtectorCallsForHelpWhenDangerIsPresent() {
        NpcBehaviorEngine engine = new NpcBehaviorEngine();
        assertEquals(NpcDecision.CALL_FOR_HELP,
                engine.decideFamilyResponse(true, true, new NpcStress(20)));
    }

    @Test
    void familyProtectorReturnsHomeWhenCriticallyStressed() {
        NpcBehaviorEngine engine = new NpcBehaviorEngine();
        assertEquals(NpcDecision.RETURN_HOME,
                engine.decideFamilyResponse(true, false, new NpcStress(90)));
    }

    @Test
    void nonProtectorIgnoresFamilyResponse() {
        NpcBehaviorEngine engine = new NpcBehaviorEngine();
        assertEquals(NpcDecision.IGNORE_PLAYER,
                engine.decideFamilyResponse(false, true, new NpcStress(20)));
    }
}
