package com.wizzadrds.theworldremembers.age;

import java.util.UUID;

public record NpcAge(UUID npcId, int years) {
    public NpcAge {
        if (years < 4 || years > 60) {
            throw new IllegalArgumentException("NPC age must be between 4 and 60");
        }
    }

    public boolean isChild() {
        return years <= 13;
    }

    public boolean isAdult() {
        return years >= 18;
    }

    public String spokenAge() {
        return "Tengo " + years + " años.";
    }
}
