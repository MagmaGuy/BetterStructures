package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorLiquidNether extends GeneratorConfigFields {
    public GeneratorLiquidNether() {
        super("generator_liquid_nether", true, Arrays.asList(StructureType.LIQUID_SURFACE));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setTreasureFilename("treasure_nether.yml");
    }
}
