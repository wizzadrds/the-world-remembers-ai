package com.wizzadrds.theworldremembers.behavior;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class NpcActivityManagerTest{@Test void stateStoresActivity(){var s=new NpcActivityState(NpcActivity.WORKING,10);assertEquals(NpcActivity.WORKING,s.activity());assertEquals(10,s.sinceTick());}}
