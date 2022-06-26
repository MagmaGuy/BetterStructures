package com.magmaguy.betterstructures.util;

import java.util.HashMap;
import java.util.Map;

public class WeighedProbability {

    public static Integer pickWeighedProbability(Map<Integer, Double> weighedValues) {

        double totalWeight = 0;

        for (Integer integer : weighedValues.keySet())
            totalWeight += weighedValues.get(integer);

        Integer selectedInteger = null;
        double random = Math.random() * totalWeight;

        for (Integer integer : weighedValues.keySet()) {
            random -= weighedValues.get(integer);
            if (random <= 0) {
                selectedInteger = integer;
                break;
            }
        }

        return selectedInteger;
    }


}
