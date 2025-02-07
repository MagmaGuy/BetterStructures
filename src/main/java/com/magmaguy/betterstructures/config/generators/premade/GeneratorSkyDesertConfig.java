package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorSkyDesertConfig extends GeneratorConfigFields {
    public GeneratorSkyDesertConfig(){
        super("generator_sky_desert", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:desert",
                "minecraft:custom"
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
