package com.wizzadrds.theworldremembers.memory;

public final class MemoryPolicy {
    private MemoryPolicy() {}

    public static MemoryDecision decide(MemoryEvent event) {
        return switch (event.importance()) {
            case TRIVIAL -> MemoryDecision.IGNORE;
            case INTERESTING, IMPORTANT, HISTORICAL, LEGENDARY -> MemoryDecision.REMEMBER;
        };
    }
}
