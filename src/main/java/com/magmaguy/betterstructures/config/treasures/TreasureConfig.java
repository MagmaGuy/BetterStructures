package com.magmaguy.betterstructures.config.treasures;

import com.magmaguy.magmacore.config.CustomConfig;
import lombok.Getter;

import java.util.HashMap;

public class TreasureConfig extends CustomConfig {
    @Getter
    private static HashMap<String, TreasureConfigFields> treasureConfigurations = new HashMap<>();

    public TreasureConfig() {
        super("treasures", "com.magmaguy.betterstructures.config.treasures.premade", TreasureConfigFields.class);
        treasureConfigurations.clear();
        for (String key : super.getCustomConfigFieldsHashMap().keySet()) {
            treasureConfigurations.put(key, (TreasureConfigFields) super.getCustomConfigFieldsHashMap().get(key));
        }
    }

    public static TreasureConfigFields getConfigFields(String configurationFilename) {
        if (configurationFilename == null) return null;
        String key = configurationFilename.endsWith(".yml") ? configurationFilename : configurationFilename + ".yml";
        return treasureConfigurations.get(key);
    }
}
