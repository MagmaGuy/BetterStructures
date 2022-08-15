package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class UndergroundGeneratorGlobalConfig extends GeneratorConfigFields {
    public UndergroundGeneratorGlobalConfig() {
        super("generator_underground_global", true, Arrays.asList(StructureType.UNDERGROUND_DEEP, StructureType.UNDERGROUND_SHALLOW));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NORMAL, World.Environment.CUSTOM));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
