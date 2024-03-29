package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundNetherDeep extends GeneratorConfigFields {
    public GeneratorUndergroundNetherDeep() {
        super("generator_underground_nether_deep", true, Arrays.asList(StructureType.UNDERGROUND_DEEP));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setTreasureFilename("treasure_nether.yml");
    }
}
