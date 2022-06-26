package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.schematics.SchematicContainer;
import org.bukkit.Chunk;

public class FitUndergroundShallowBuilding extends FitUndergroundBuilding {

    public FitUndergroundShallowBuilding(Chunk chunk, SchematicContainer schematicContainer) {
        super(chunk, schematicContainer, 0, 50);
    }

    public FitUndergroundShallowBuilding(Chunk chunk) {
        super(chunk, 0, 50);
    }

}
