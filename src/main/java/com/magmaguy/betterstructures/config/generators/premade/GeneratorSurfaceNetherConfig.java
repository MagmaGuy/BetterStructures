package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSurfaceNetherConfig extends GeneratorConfigFields {
    public GeneratorSurfaceNetherConfig() {
        super("generator_surface_nether", true, Arrays.asList(StructureType.SURFACE));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setChestEntries(DefaultChestContents.netherContents());
    }
}
