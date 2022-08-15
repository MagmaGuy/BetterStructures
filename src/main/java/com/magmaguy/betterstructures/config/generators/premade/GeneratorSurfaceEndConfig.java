package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSurfaceEndConfig extends GeneratorConfigFields {
    public GeneratorSurfaceEndConfig() {
        super("generator_surface_end", true, Arrays.asList(StructureType.SURFACE));
        setValidWorldEnvironments(Arrays.asList(World.Environment.THE_END));
        setTreasureFilename("treasure_nether.yml");
    }
}
