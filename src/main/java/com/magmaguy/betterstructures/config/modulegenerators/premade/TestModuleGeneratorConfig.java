package com.magmaguy.betterstructures.config.modulegenerators.premade;

import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;

import java.util.List;

public class TestModuleGeneratorConfig extends ModuleGeneratorsConfigFields {

    public TestModuleGeneratorConfig() {
        super("test_module_generator");
        radius = 2;
        edges = true;
        minChunkY = 0;
        maxChunkY = 0;
        debug = false;
        startModules = List.of(
                "MegaModules_MineshaftPack_Middle1.schem",
                "MegaModules_MineshaftPack_Middle2.schem",
                "MegaModules_MineshaftPack_Middle3.schem");
        generateInstantly = true;
        pasteBatchSize = 1;
    }
}
