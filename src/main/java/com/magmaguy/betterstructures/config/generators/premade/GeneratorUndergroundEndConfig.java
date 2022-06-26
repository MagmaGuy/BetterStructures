package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundEndConfig extends GeneratorConfigFields {
    public GeneratorUndergroundEndConfig() {
        super("generator_underground_end", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidWorldEnvironments(Arrays.asList(World.Environment.THE_END));
    }
}
