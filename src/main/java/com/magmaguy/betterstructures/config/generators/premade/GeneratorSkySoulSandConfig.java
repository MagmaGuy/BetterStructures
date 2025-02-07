package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSkySoulSandConfig extends GeneratorConfigFields {
    public GeneratorSkySoulSandConfig() {
        super("generator_sky_soul_sand", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:soul_sand_valley",
                "minecraft:custom"));
        setTreasureFilename("treasure_nether.yml");
    }
}
