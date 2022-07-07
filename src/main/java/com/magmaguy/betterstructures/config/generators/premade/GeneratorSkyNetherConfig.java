package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSkyNetherConfig extends GeneratorConfigFields {
    public GeneratorSkyNetherConfig(){
        super("generator_sky_nether", true, Arrays.asList(StructureType.SKY));
        setValidWorldEnvironments(Arrays.asList(World.Environment.NETHER));
        setChestEntries(DefaultChestContents.netherContents());
    }
}
