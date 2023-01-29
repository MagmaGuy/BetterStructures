package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;

import java.util.Arrays;

public class GeneratorSkyEndConfig  extends GeneratorConfigFields  {
    public GeneratorSkyEndConfig(){
        super("generator_sky_end", true, Arrays.asList(StructureType.SKY));
        setValidWorldEnvironments(Arrays.asList(World.Environment.THE_END));
        setTreasureFilename("treasure_end.yml");
    }
}
