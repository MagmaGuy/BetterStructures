package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Chunk;

public class FitUndergroundShallowBuilding {
    private FitUndergroundShallowBuilding() {
    }

    public static FitUndergroundBuilding fit(Chunk chunk) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                return new FitUndergroundBuilding(chunk, 0, 50, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            case NETHER:
                return new FitUndergroundBuilding(chunk, 60, 120, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            case THE_END:
                //The End has no "deep" underground area, it's floating islands
                return new FitUndergroundBuilding(chunk, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            default:
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
                return null;
        }
    }
    public static FitUndergroundBuilding fit(Chunk chunk, SchematicContainer schematicContainer) {
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                return new FitUndergroundBuilding(chunk, schematicContainer, 0, 50, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            case NETHER:
                return new FitUndergroundBuilding(chunk, schematicContainer, 60, 120, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            case THE_END:
                //The End has no "deep" underground area, it's floating islands
                return new FitUndergroundBuilding(chunk, schematicContainer, 0, 80, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
            default:
                Logger.warn("Unexpected environment type: " + chunk.getWorld().getEnvironment());
                return null;
        }
    }
}
