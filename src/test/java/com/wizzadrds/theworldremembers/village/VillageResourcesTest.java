package com.wizzadrds.theworldremembers.village;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class VillageResourcesTest { @Test void pressureReflectsFoodShortage(){assertEquals(0.0,new VillageResources(20,1,2,20).pressure());assertEquals(0.75,new VillageResources(5,1,2,20).pressure());assertEquals(1.0,new VillageResources(0,0,0,0).pressure());} }
