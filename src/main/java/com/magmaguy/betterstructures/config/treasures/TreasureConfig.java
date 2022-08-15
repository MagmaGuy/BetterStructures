package com.magmaguy.betterstructures.config.treasures;

import com.magmaguy.betterstructures.config.CustomConfig;
import lombok.Getter;

import java.util.HashMap;

public class TreasureConfig extends CustomConfig {
    @Getter
    private static HashMap<String, TreasureConfigFields> treasureConfigurations = new HashMap<>();

    public TreasureConfig() {
        super("treasures", "com.magmaguy.betterstructures.config.treasures.premade", TreasureConfigFields.class);
        for (String key : super.getCustomConfigFieldsHashMap().keySet()) {
            treasureConfigurations.put(key, (TreasureConfigFields) super.getCustomConfigFieldsHashMap().get(key));
        }
    }

    public static TreasureConfigFields getConfigFields(String configurationFilename) {
        return treasureConfigurations.get(configurationFilename);
    }
}
