package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;

import java.util.Arrays;

public class GeneratorUndergroundDeepConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDeepConfig(){
        super("generator_underground_deep", true, Arrays.asList(StructureType.UNDERGROUND_DEEP));
    }
}
