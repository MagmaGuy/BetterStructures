package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyCrimsonForestConfig extends GeneratorConfigFields {
    public GeneratorSkyCrimsonForestConfig(){
        super("generator_sky_crimson_forest", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(
                Biome.CRIMSON_FOREST,
                Biome.CUSTOM));
        setTreasureFilename("treasure_nether.yml");
    }
}
