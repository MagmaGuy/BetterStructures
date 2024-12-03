package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkyNetherWastesConfig extends GeneratorConfigFields {
    public GeneratorSkyNetherWastesConfig(){
        super("generator_sky_nether_wastes", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:nether_wastes",
                "minecraft:custom"
                ));
        setTreasureFilename("treasure_nether.yml");
    }
}
