package com.magmaguy.betterstructures.config.generators.premade;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import org.bukkit.block.Biome;

import java.util.Arrays;

public class GeneratorSkySnowyConfig extends GeneratorConfigFields {
    public GeneratorSkySnowyConfig(){
        super("generator_sky_snowy", true, Arrays.asList(StructureType.SKY));
        setValidBiomesStrings(Arrays.asList(
                "minecraft:snowy_taiga",
                "minecraft:snowy_beach",
                "minecraft:snowy_plains",
                "minecraft:snowy_slopes",
                "minecraft:ice_spikes",
                "minecraft:custom"
        ));
        setTreasureFilename("treasure_overworld_surface.yml");
    }
}
