package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSkyBarrenConfig extends GeneratorConfigFields {
    public GeneratorSkyBarrenConfig(){
        super("generator_sky_barren", true, Arrays.asList(StructureType.SKY));
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
