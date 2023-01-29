package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSurfaceGlobalConfig extends GeneratorConfigFields {
    public GeneratorSurfaceGlobalConfig() {
        super("generator_surface_global", true, Arrays.asList(StructureType.SURFACE));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NORMAL, World.Environment.CUSTOM));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
