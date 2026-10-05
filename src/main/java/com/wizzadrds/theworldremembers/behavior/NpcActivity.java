package com.wizzadrds.theworldremembers.behavior;

public enum NpcActivity {
    IDLE(true),
    WORKING(true),
    EATING(true),
    SLEEPING(false),
    TRADING(true),
    TALKING(false),
    SOCIALIZING(false),
    WALKING(true),
    TRAVELLING(true),
    FOLLOWING_PLAYER(true),
    FLEEING(false),
    FAMILY(false);

    private final boolean interruptible;

    NpcActivity(boolean interruptible) {
        this.interruptible = interruptible;
    }

    public boolean interruptible() {
        return interruptible;
    }
}
