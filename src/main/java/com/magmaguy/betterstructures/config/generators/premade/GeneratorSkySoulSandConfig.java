package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkySoulSandConfig extends GeneratorConfigFields {
    public GeneratorSkySoulSandConfig() {
        super("generator_sky_soul_sand", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(
                Biome.SOUL_SAND_VALLEY,
                Biome.CUSTOM));
        setTreasureFilename("treasure_nether.yml");
    }
}
