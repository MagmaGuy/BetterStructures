package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorLiquidOverworld extends GeneratorConfigFields {
    public GeneratorLiquidOverworld() {
        super("generator_liquid_overworld", true, Arrays.asList(StructureType.LIQUID_SURFACE));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NORMAL, World.Environment.CUSTOM));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
