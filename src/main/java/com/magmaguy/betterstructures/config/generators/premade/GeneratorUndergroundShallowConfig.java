package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

import java.util.Arrays;

public class GeneratorUndergroundShallowConfig extends GeneratorConfigFields {
    public GeneratorUndergroundShallowConfig(){
        super("generator_underground_shallow", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW));
        setChestEntries(DefaultChestContents.overworldUndergroundContents());
    }
}
