package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceBarrenConfig extends GeneratorConfigFields {
    public GeneratorSurfaceBarrenConfig(){
        super("generator_surface_barren", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomes(Arrays.asList(
                Biome.BADLANDS,
                Biome.SAVANNA,
                Biome.SAVANNA_PLATEAU,
                Biome.WINDSWEPT_SAVANNA,
                Biome.MUSHROOM_FIELDS
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
