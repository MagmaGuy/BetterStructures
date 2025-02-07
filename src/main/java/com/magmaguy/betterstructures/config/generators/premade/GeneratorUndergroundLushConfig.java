package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorUndergroundLushConfig extends GeneratorConfigFields {
    public GeneratorUndergroundLushConfig() {
        super("generator_underground_lush", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:lush_caves",
                "minecraft:custom"));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}