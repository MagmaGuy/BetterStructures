package com.magmaguy.betterstructures.config.modulegenerators.premade;

import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;

import java.util.List;

public class DungeoneeringModuleGeneratorConfig extends ModuleGeneratorsConfigFields {

    public DungeoneeringModuleGeneratorConfig() {
        super("dungeoneering_module_generator");
        radius = 5;
        edges = true;
        minChunkY = -2;
        maxChunkY = 2;
        moduleSizeXZ = 32;
        moduleSizeY = 16;
        debug = false;
        startModules = List.of(
                "Betterstructures_ModularDungeon_Free_Center_1.schem",
                "Betterstructures_ModularDungeon_Free_Center_2.schem",
                "Betterstructures_ModularDungeon_Premium_Center_1.schem",
                "Betterstructures_ModularDungeon_Premium_Center_2.schem",
                "Betterstructures_ModularDungeon_Premium_Center_3.schem");
        isWorldGeneration = false;
        treasureFile = "treasure_overworld_underground.yml";
    }
}
