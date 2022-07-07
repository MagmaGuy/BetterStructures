package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.WarningMessage;
import org.bukkit.Chunk;

public class FitUndergroundShallowBuilding {
    private FitUndergroundShallowBuilding() {
    }

    public static void fit(Chunk chunk) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                new FitUndergroundBuilding(chunk, -0, 50, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                break;
            case NETHER:
                new FitUndergroundBuilding(chunk, 60, 120, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                break;
            case THE_END:
                //The nether has no "deep" underground area, it's floating islands
                new FitUndergroundBuilding(chunk, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                break;
            default:
                new WarningMessage("Unexpected environment type: " + chunk.getWorld().getEnvironment());
        }
    }
}
