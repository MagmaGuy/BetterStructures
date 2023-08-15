package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSurfaceGrasslandConfig extends GeneratorConfigFields {
    public GeneratorSurfaceGrasslandConfig(){
        super("generator_surface_grassland", true, Arrays.asList(StructureType.SURFACE));
        setValidBiomes(Arrays.asList(
                Biome.PLAINS,
                Biome.FOREST,
                Biome.BIRCH_FOREST,
                Biome.DARK_FOREST,
                Biome.FLOWER_FOREST,
                Biome.JUNGLE,
                Biome.SWAMP,
                Biome.TAIGA,
                Biome.OLD_GROWTH_PINE_TAIGA,
                Biome.OLD_GROWTH_SPRUCE_TAIGA,
                Biome.OLD_GROWTH_BIRCH_FOREST,
                Biome.SUNFLOWER_PLAINS,
                Biome.WINDSWEPT_FOREST,
                Biome.WINDSWEPT_HILLS,
                Biome.CUSTOM
                ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
