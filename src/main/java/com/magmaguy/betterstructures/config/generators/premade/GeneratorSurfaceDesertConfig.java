package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceDesertConfig extends GeneratorConfigFields {
    public GeneratorSurfaceDesertConfig() {
        super("generator_surface_desert", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomes(Arrays.asList(
                Biome.DESERT,
                Biome.CUSTOM
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
