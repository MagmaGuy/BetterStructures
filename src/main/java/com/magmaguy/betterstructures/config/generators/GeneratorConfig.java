package com.magmaguy.betterstructures.config.generators;

import com.magmaguy.betterstructures.config.CustomConfig;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.HashMap;

public class GeneratorConfig extends CustomConfig {
    @Getter
    private static HashMap<String, GeneratorConfigFields> generatorConfigurations = new HashMap<>();

    public GeneratorConfig() {
        super("generators", "com.magmaguy.betterstructures.config.generators.premade", GeneratorConfigFields.class);
        generatorConfigurations.clear();
        for (String key : super.getCustomConfigFieldsHashMap().keySet()){
            generatorConfigurations.put(key, (GeneratorConfigFields) super.getCustomConfigFieldsHashMap().get(key));
        }
    }

    public static GeneratorConfigFields getConfigFields(String configurationFilename) {
        return generatorConfigurations.get(configurationFilename);
    }
}
