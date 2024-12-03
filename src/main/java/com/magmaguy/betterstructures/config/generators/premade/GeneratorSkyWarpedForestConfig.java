package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyWarpedForestConfig extends GeneratorConfigFields {
    public GeneratorSkyWarpedForestConfig() {
        super("generator_sky_warped_forest", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:warped_forest",
                "minecraft:custom"));
        setTreasureFilename("treasure_nether.yml");
    }
}
