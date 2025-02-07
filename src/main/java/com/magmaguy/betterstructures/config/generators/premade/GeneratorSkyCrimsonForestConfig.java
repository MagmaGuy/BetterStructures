package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSkyCrimsonForestConfig extends GeneratorConfigFields {
    public GeneratorSkyCrimsonForestConfig(){
        super("generator_sky_crimson_forest", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:crimson_forest",
                "minecraft:custom"));
        setTreasureFilename("treasure_nether.yml");
    }
}
