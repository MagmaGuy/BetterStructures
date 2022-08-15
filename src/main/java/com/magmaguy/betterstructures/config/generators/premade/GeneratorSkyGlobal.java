package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

import java.util.Arrays;

public class GeneratorSkyGlobal extends GeneratorConfigFields {
    public GeneratorSkyGlobal() {
        super("generator_sky_global", true, Arrays.asList(StructureType.SKY));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
