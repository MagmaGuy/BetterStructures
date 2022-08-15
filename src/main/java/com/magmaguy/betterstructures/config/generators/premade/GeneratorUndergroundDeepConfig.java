package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorUndergroundDeepConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDeepConfig(){
        super("generator_underground_deep", true, Arrays.asList(StructureType.UNDERGROUND_DEEP));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
