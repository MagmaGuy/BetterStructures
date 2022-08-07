package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorUndergroundDripstoneConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDripstoneConfig(){
        super("generator_underground_dripstone", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidBiomes(Arrays.asList(Biome.DRIPSTONE_CAVES));
        setChestEntries(DefaultChestContents.overworldUndergroundContents());
    }
}
