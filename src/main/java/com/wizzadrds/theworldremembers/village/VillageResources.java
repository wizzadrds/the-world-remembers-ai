package com.wizzadrds.theworldremembers.village;

public record VillageResources(int food, int valuableItems, int occupiedStorage, int storageCapacity) {
 public VillageResources { if(food<0||valuableItems<0||occupiedStorage<0||storageCapacity<0) throw new IllegalArgumentException(); }
 public double pressure(){ if(storageCapacity==0)return 1.0; return Math.max(0.0,1.0-(double)food/Math.max(1,storageCapacity)); }
}
