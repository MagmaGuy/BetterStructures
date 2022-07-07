package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.buildingfitter.FitUndergroundBuilding;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.WarningMessage;
import org.bukkit.Chunk;

public class FitUndergroundDeepBuilding {
    private FitUndergroundDeepBuilding() {
    }

    public static void fit(Chunk chunk) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                new FitUndergroundBuilding(chunk, -53, 0, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
                break;
            case NETHER:
                new FitUndergroundBuilding(chunk, 5, 60, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
                break;
            case THE_END:
                new FitUndergroundBuilding(chunk, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
                //The nether has no "deep" underground area, it's floating islands
                break;
            default:
                new WarningMessage("Unexpected environment type: " + chunk.getWorld().getEnvironment());
        }
    }
}
