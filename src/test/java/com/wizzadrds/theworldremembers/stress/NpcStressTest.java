package com.wizzadrds.theworldremembers.stress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcStressTest {
    @Test
    void stressIsClamped() {
        assertEquals(0, new NpcStress(-20).value());
        assertEquals(100, new NpcStress(140).value());
    }

    @Test
    void thresholdsAreStable() {
        assertTrue(new NpcStress(75).isHighlyStressed());
        assertTrue(new NpcStress(90).isCritical());
        assertFalse(new NpcStress(49).isStressed());
    }

    @Test
    void increaseAndDecreaseStayBounded() {
        NpcStress stress = new NpcStress(90).increase(30).decrease(150);
        assertEquals(0, stress.value());
    }
}
