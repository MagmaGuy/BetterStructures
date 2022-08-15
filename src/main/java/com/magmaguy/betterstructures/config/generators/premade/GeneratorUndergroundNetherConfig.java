package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundNetherConfig extends GeneratorConfigFields {
    public GeneratorUndergroundNetherConfig(){
        super("generator_underground_nether", true, Arrays.asList(StructureType.UNDERGROUND_DEEP, StructureType.UNDERGROUND_SHALLOW));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setTreasureFilename("treasure_nether.yml");
    }
}
