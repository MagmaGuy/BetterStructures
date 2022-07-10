package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;

public class GeneratorUndergroundShallowConfig extends GeneratorConfigFields {
    public GeneratorUndergroundShallowConfig(){
        super("generator_underground_shallow", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW));
        setChestEntries(DefaultChestContents.overworldUndergroundContents());
        setValidWorldEnvironments(Arrays.asList(World.Environment.NORMAL, World.Environment.CUSTOM));
    }
}
