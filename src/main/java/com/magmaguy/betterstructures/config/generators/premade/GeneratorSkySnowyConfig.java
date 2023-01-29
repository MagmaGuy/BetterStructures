package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkySnowyConfig extends GeneratorConfigFields {
    public GeneratorSkySnowyConfig(){
        super("generator_sky_snowy", true, Arrays.asList(StructureType.SKY));
        setValidBiomes(Arrays.asList(
                Biome.SNOWY_TAIGA,
                Biome.SNOWY_BEACH,
                Biome.SNOWY_PLAINS,
                Biome.SNOWY_SLOPES,
                Biome.ICE_SPIKES
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
