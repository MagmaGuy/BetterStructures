package com.magmaguy.betterstructures.config.spawnpools;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.magmacore.config.CustomConfig;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

public class SpawnPoolsConfig extends CustomConfig {
    @Getter
    public static final HashMap<String, SpawnPoolsConfigFields> spawnPoolConfigFields = new HashMap<>();

    public SpawnPoolsConfig() {
        super("spawn_pools", "com.magmaguy.betterstructures.config.spawnpools.premade", SpawnPoolsConfigFields.class);
        String directory = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "spawn_pools";
        Logger.debug(directory);
        File file = Path.of(directory).toFile();
        if (!file.exists()) file.mkdir();
        spawnPoolConfigFields.clear();
        for (String key : super.getCustomConfigFieldsHashMap().keySet())
            spawnPoolConfigFields.put(key, (SpawnPoolsConfigFields) super.getCustomConfigFieldsHashMap().get(key));
    }

    public static SpawnPoolsConfigFields getConfigFields(String configurationFilename) {
        return spawnPoolConfigFields.get(configurationFilename);
    }
}