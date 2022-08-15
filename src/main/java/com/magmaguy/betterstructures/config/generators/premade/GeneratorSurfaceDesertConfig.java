package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceDesertConfig extends GeneratorConfigFields {
    public GeneratorSurfaceDesertConfig() {
        super("generator_surface_desert", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomes(Arrays.asList(
                Biome.DESERT
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
