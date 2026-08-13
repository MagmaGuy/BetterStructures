package com.magmaguy.betterstructures.util;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeighedProbability {

    /**
     * Picks a key from the map with a probability proportional to its weight.
     *
     * @return the picked key, or null when nothing could be picked (empty map or non-positive
     * weights) - callers must handle null
     */
    public static Integer pickWeightedProbability(Map<Integer, Double> weighedValues) {

        double totalWeight = 0;

        for (Map.Entry<Integer, Double> entry : weighedValues.entrySet())
            totalWeight += entry.getValue();

        Integer selectedInteger = null;
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (Map.Entry<Integer, Double> entry : weighedValues.entrySet()) {
            random -= entry.getValue();
            if (random <= 0) {
                selectedInteger = entry.getKey();
                break;
            }
        }

        return selectedInteger;
    }

}
