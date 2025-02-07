package com.magmaguy.betterstructures.config.modulegenerators;

import com.magmaguy.magmacore.config.CustomConfig;
import lombok.Getter;

import java.util.HashMap;

public class ModuleGeneratorsConfig extends CustomConfig {
    @Getter
    private static final HashMap<String, ModuleGeneratorsConfigFields> moduleGenerators = new HashMap<>();

    public ModuleGeneratorsConfig() {
        super("module_generators", "com.magmaguy.betterstructures.config.modulegenerators.premade", ModuleGeneratorsConfigFields.class);
        moduleGenerators.clear();
        for (String key : super.getCustomConfigFieldsHashMap().keySet()) {
            moduleGenerators.put(key, (ModuleGeneratorsConfigFields) super.getCustomConfigFieldsHashMap().get(key));
        }
    }

    public static ModuleGeneratorsConfigFields getConfigFields(String configurationFilename) {
        return moduleGenerators.get(configurationFilename);
    }
}
