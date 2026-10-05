package com.wizzadrds.theworldremembers.behavior;

import java.util.UUID;

public record NpcTravelState(
        UUID npcId,
        UUID targetPlayerId,
        int distanceFromHome,
        int travelTicks
) {
    public NpcTravelState {
        if (npcId == null || targetPlayerId == null) {
            throw new IllegalArgumentException("npcId and targetPlayerId are required");
        }
        distanceFromHome = Math.max(0, distanceFromHome);
        travelTicks = Math.max(0, travelTicks);
    }

    public boolean isLongJourney() {
        return distanceFromHome >= 128 || travelTicks >= 1200;
    }
}
