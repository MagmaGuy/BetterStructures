package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorUndergroundDripstoneConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDripstoneConfig(){
        super("generator_underground_dripstone", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:dripstone_caves",
                "minecraft:custom"));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
