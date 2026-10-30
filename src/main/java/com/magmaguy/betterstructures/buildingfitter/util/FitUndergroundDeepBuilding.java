package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.buildingfitter.FitUndergroundBuilding;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Chunk;

public class FitUndergroundDeepBuilding {
    private FitUndergroundDeepBuilding() {
    }

    public static FitUndergroundBuilding fit(Chunk chunk) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                return new FitUndergroundBuilding(chunk, -53, 0, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            case NETHER:
                return new FitUndergroundBuilding(chunk, 5, 60, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            case THE_END:
                //The End has no "deep" underground area, it's floating islands
                return new FitUndergroundBuilding(chunk, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            default:
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
                return null;
        }
    }

    public static FitUndergroundBuilding fit(Chunk chunk, SchematicContainer schematicContainer) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                return new FitUndergroundBuilding(chunk, schematicContainer, -53, 0, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            case NETHER:
                return new FitUndergroundBuilding(chunk, schematicContainer, 5, 60, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            case THE_END:
                //The End has no "deep" underground area, it's floating islands
                return new FitUndergroundBuilding(chunk, schematicContainer, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP);
            default:
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
                return null;
        }
    }
}
