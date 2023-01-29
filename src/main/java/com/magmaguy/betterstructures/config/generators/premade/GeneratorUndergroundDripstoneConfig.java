package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorUndergroundDripstoneConfig extends GeneratorConfigFields {
    public GeneratorUndergroundDripstoneConfig(){
        super("generator_underground_dripstone", true, Arrays.asList(StructureType.UNDERGROUND_SHALLOW, StructureType.UNDERGROUND_DEEP));
        setValidBiomes(Arrays.asList(Biome.DRIPSTONE_CAVES));
        setTreasureFilename("treasure_overworld_underground.yml");
    }
}
