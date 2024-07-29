package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.util.Logger;
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
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
        }
    }
    public static void fit(Chunk chunk, SchematicContainer schematicContainer) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                new FitUndergroundBuilding(chunk, schematicContainer, -53, 0, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                break;
            case NETHER:
                new FitUndergroundBuilding(chunk, schematicContainer, 5, 60, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                break;
            case THE_END:
                new FitUndergroundBuilding(chunk, schematicContainer, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
                //The nether has no "deep" underground area, it's floating islands
                break;
            default:
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
        }
    }
}
