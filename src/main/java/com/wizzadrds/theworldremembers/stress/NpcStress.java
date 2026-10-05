package com.wizzadrds.theworldremembers.stress;

public record NpcStress(int value) {
    public NpcStress {
        value = clamp(value);
    }

    public NpcStress increase(int amount) {
        return new NpcStress(value + Math.max(0, amount));
    }

    public NpcStress decrease(int amount) {
        return new NpcStress(value - Math.max(0, amount));
    }

    public boolean isStressed() {
        return value >= 50;
    }

    public boolean isHighlyStressed() {
        return value >= 75;
    }

    public boolean isCritical() {
        return value >= 90;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
