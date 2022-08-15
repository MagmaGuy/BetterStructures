package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorUndergroundLushConfig extends GeneratorConfigFields {
    public GeneratorUndergroundLushConfig() {
        super("generator_underground_lush", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidBiomes(Arrays.asList(Biome.LUSH_CAVES));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
