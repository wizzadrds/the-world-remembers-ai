package com.wizzadrds.theworldremembers.behavior;
public record NpcActivityState(NpcActivity activity,long sinceTick){public NpcActivityState{if(sinceTick<0)throw new IllegalArgumentException("sinceTick");}}
