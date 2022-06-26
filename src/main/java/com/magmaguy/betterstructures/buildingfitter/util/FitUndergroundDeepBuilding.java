package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.buildingfitter.FitUndergroundBuilding;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import org.bukkit.Chunk;

public class FitUndergroundDeepBuilding extends FitUndergroundBuilding {
    public FitUndergroundDeepBuilding(Chunk chunk, SchematicContainer schematicContainer) {
        super(chunk, schematicContainer, -53, 0);
    }

    public FitUndergroundDeepBuilding(Chunk chunk) {
        super(chunk, -63, 0);
    }
}
