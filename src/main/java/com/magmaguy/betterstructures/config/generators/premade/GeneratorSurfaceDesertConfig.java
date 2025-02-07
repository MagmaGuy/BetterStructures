package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSurfaceDesertConfig extends GeneratorConfigFields {
    public GeneratorSurfaceDesertConfig() {
        super("generator_surface_desert", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomesStrings(Arrays.asList(
               "minecraft:desert",
               "minecraft:custom"
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
