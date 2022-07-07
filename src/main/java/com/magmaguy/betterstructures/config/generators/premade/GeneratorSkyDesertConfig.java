package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyDesertConfig extends GeneratorConfigFields {
    public GeneratorSkyDesertConfig(){
        super("generator_sky_desert", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(
                Biome.DESERT
        ));
        setChestEntries(DefaultChestContents.overworldContents());
    }
}
