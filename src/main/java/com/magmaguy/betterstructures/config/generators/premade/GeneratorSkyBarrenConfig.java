package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyBarrenConfig extends GeneratorConfigFields {
    public GeneratorSkyBarrenConfig(){
        super("generator_sky_barren", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(
                Biome.BADLANDS,
                Biome.SAVANNA,
                Biome.SAVANNA_PLATEAU,
                Biome.WINDSWEPT_SAVANNA,
                Biome.MUSHROOM_FIELDS
        ));
        setChestEntries(DefaultChestContents.overworldContents());
    }
}
