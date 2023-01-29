package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSkyNetherConfig extends GeneratorConfigFields {
    public GeneratorSkyNetherConfig(){
        super("generator_sky_nether", true, Arrays.asList(StructureType.SKY));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setTreasureFilename("treasure_nether.yml");
    }
}
