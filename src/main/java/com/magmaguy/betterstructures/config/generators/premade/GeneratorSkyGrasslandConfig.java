package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSkyGrasslandConfig extends GeneratorConfigFields {
    public GeneratorSkyGrasslandConfig(){
        super("generator_sky_grassland", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:plains",
                "minecraft:forest",
                "minecraft:birch_forest",
                "minecraft:dark_forest",
                "minecraft:flower_forest",
                "minecraft:jungle",
                "minecraft:swamp",
                "minecraft:taiga",
                "minecraft:old_growth_pine_taiga",
                "minecraft:old_growth_spruce_taiga",
                "minecraft:old_growth_birch_forest",
                "minecraft:sunflower_plains",
                "minecraft:windswept_forest",
                "minecraft:windswept_hills",
                "minecraft:custom"
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
