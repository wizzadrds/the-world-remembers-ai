package com.wizzadrds.theworldremembers.village;
import org.junit.jupiter.api.Test; import java.util.UUID; import static org.junit.jupiter.api.Assertions.*;
class VillageHistoryManagerTest { @Test void historyTracksPeakAndEvents(){var h=new VillageHistory(UUID.randomUUID(),10,10,3,0);h=h.observe(5,20).event();assertEquals(5,h.peakPopulation());assertEquals(1,h.totalImportantEvents());assertEquals(10,h.firstObservedTick());assertEquals(20,h.lastObservedTick());} }
