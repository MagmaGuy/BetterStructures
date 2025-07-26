package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.magmacore.config.CustomConfig;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;

public class SchematicConfig extends CustomConfig {
    @Getter
    private static final HashMap<String, SchematicConfigField> schematicConfigurations = new HashMap<>();

    public SchematicConfig() {
        super("schematics", SchematicConfigField.class);
        schematicConfigurations.clear();

        File readMeFile = new File(MetadataHandler.PLUGIN.getDataFolder(), "schematics" + File.separatorChar + "ReadMe.txt");
        if (!readMeFile.exists()) {
            readMeFile.getParentFile().mkdirs();
            MetadataHandler.PLUGIN.saveResource("schematics" + File.separatorChar + "ReadMe.txt", false);
        }

        HashMap<File, Clipboard> clipboards = new HashMap();
        //Initialize schematics
        for (File file : new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "schematics").listFiles())
            scanDirectoryForSchematics(file, clipboards);

        for (String key : super.getCustomConfigFieldsHashMap().keySet())
            schematicConfigurations.put(key, (SchematicConfigField) super.getCustomConfigFieldsHashMap().get(key));

        for (File file : clipboards.keySet()) {
            String configurationName = convertFromSchematicFilename(file.getName());
            SchematicConfigField schematicConfigField = new SchematicConfigField(configurationName, true);
            new CustomConfig(file.getParent().replace(
                    MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar, ""),
                    SchematicConfigField.class, schematicConfigField);
            schematicConfigurations.put(configurationName, schematicConfigField);
        }

        for (SchematicConfigField schematicConfigField : schematicConfigurations.values()) {
            if (!schematicConfigField.isEnabled()) continue;
            String schematicFilename = convertFromConfigurationFilename(schematicConfigField.getFilename());
            Clipboard clipboard = null;
            for (File file : clipboards.keySet())
                if (file.getName().equals(schematicFilename)) {
                    clipboard = clipboards.get(file);
                    break;
                }
            new SchematicContainer(
                    clipboard,
                    schematicFilename,
                    schematicConfigField,
                    schematicConfigField.getFilename());
        }

    }

    private static void scanDirectoryForSchematics(File file, HashMap<File, Clipboard> clipboards) {
        if (file.getName().endsWith(".schem")) {
            Clipboard clipboard = Schematic.load(file);
            if (clipboard == null) return;
            clipboards.put(file, clipboard);
        }
        else if (file.isDirectory())
            for (File iteratedFile : file.listFiles())
                scanDirectoryForSchematics(iteratedFile, clipboards);
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
