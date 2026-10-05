package com.wizzadrds.theworldremembers.age;

import java.util.Random;

public final class NpcAgeGenerator {
    private NpcAgeGenerator() {}

    public static int generateChildAge(Random random) {
        return 4 + random.nextInt(10);
    }

    public static int generateAdultAge(Random random) {
        return 18 + random.nextInt(43);
    }
}
