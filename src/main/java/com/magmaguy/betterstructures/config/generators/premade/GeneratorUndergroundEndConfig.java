package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundEndConfig extends GeneratorConfigFields {
    public GeneratorUndergroundEndConfig() {
        super("generator_underground_end", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW));
        setValidWorldEnvironments(Arrays.asList(World.Environment.THE_END));
        setTreasureFilename("treasure_end.yml");
    }
}
