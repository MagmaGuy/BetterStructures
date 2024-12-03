package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceBarrenConfig extends GeneratorConfigFields {
    public GeneratorSurfaceBarrenConfig(){
        super("generator_surface_barren", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:badlands",
                "minecraft:savanna",
                "minecraft:savanna_plateau",
                "minecraft:windswept_savanna",
                "minecraft:mushroom_fields",
                "minecraft:custom"
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
