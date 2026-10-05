package com.wizzadrds.theworldremembers.village;
import org.junit.jupiter.api.Test; import java.util.UUID; import net.minecraft.core.BlockPos; import static org.junit.jupiter.api.Assertions.*;
class VillageManagerTest { @Test void observationPersistsPopulationAndTime(){var m=new VillageManager();var id=UUID.randomUUID();m.observe(id,new BlockPos(1,2,3),7,10);var s=m.observe(id,new BlockPos(2,2,3),9,20);assertEquals(9,s.population());assertEquals(10,s.firstObservedTick());assertEquals(20,s.lastObservedTick());} }
