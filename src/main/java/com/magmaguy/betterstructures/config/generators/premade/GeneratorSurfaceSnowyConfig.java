package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceSnowyConfig extends GeneratorConfigFields {
    public GeneratorSurfaceSnowyConfig() {
        super("generator_surface_snowy", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomes(Arrays.asList(
                Biome.SNOWY_TAIGA,
                Biome.SNOWY_BEACH,
                Biome.SNOWY_PLAINS,
                Biome.SNOWY_SLOPES,
                Biome.ICE_SPIKES
        ));
    }
}
