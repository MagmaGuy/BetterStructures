package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundDeepConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDeepConfig(){
        super("generator_underground_deep", true, Arrays.asList(StructureType.UNDERGROUND_DEEP));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NORMAL, World.Environment.CUSTOM));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
