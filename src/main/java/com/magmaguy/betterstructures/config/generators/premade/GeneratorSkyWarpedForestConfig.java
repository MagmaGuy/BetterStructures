package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyWarpedForestConfig extends GeneratorConfigFields {
    public GeneratorSkyWarpedForestConfig() {
        super("generator_sky_warped_forest", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(Biome.WARPED_FOREST));
        setChestEntries(DefaultChestContents.netherContents());
    }
}
