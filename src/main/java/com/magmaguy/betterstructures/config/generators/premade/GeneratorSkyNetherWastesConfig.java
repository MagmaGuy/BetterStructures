package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyNetherWastesConfig extends GeneratorConfigFields {
    public GeneratorSkyNetherWastesConfig(){
        super("generator_sky_nether_wastes", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(Biome.NETHER_WASTES));
        setChestEntries(DefaultChestContents.netherContents());
    }
}
