package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.CustomConfig;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;

public class SchematicConfig extends CustomConfig {
    @Getter
    private static HashMap<String, SchematicConfigField> schematicConfigurations = new HashMap<>();

    public SchematicConfig() {
        super("schematics", "", SchematicConfigField.class);
        schematicConfigurations.clear();

        HashMap<String, Clipboard> clipboards = new HashMap();
        //Initialize schematics
        for (File file : new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "schematics").listFiles())
            if (file.getName().endsWith(".schem"))
                clipboards.put(file.getName(), Schematic.load(file.getName()));

        for (String key : super.getCustomConfigFieldsHashMap().keySet())
            schematicConfigurations.put(key, (SchematicConfigField) super.getCustomConfigFieldsHashMap().get(key));

        for (String string : clipboards.keySet()) {
            String configurationName = convertFromSchematicFilename(string);
            SchematicConfigField schematicConfigField = new SchematicConfigField(configurationName, true);
            new CustomConfig("schematics", SchematicConfigField.class, schematicConfigField);
            schematicConfigurations.put(configurationName, schematicConfigField);
        }

        for (SchematicConfigField schematicConfigField : schematicConfigurations.values()) {
            String schematicFilename = convertFromConfigurationFilename(schematicConfigField.getFilename());
            Clipboard clipboard = clipboards.get(schematicFilename);
            new SchematicContainer(
                    clipboard,
                    schematicFilename,
                    schematicConfigField,
                    schematicConfigField.getFilename());
        }

    }

    public static String convertFromSchematicFilename(String schematicFilename) {
        return schematicFilename.replace(".schem", ".yml");
    }

    public static String convertFromConfigurationFilename(String configurationFilename) {
        return configurationFilename.replace(".yml", ".schem");
    }

    public static SchematicConfigField getSchematicConfiguration(String filename) {
        return schematicConfigurations.get(filename);
    }
}
