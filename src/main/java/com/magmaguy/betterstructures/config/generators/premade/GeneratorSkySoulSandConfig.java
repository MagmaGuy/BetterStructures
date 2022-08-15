package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkySoulSandConfig extends GeneratorConfigFields {
    public GeneratorSkySoulSandConfig() {
        super("generator_sky_soul_sand", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(Biome.SOUL_SAND_VALLEY));
        setTreasureFilename("treasure_nether.yml");
    }
}
