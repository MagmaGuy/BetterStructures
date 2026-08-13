package com.magmaguy.betterstructures.util;

import java.io.File;
import java.util.List;

public final class SchematicFileUtils {
    private SchematicFileUtils() {
    }

    public static void scanDirectoryForSchematics(File file, List<File> schematicFiles) {
        if (file.getName().endsWith(".schem")) {
            schematicFiles.add(file);
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null)
                for (File iteratedFile : children) scanDirectoryForSchematics(iteratedFile, schematicFiles);
        }
    }

    public static String convertFromSchematicFilename(String schematicFilename) {
        return schematicFilename.replace(".schem", ".yml");
    }

    public static String convertFromConfigurationFilename(String configurationFilename) {
        return configurationFilename.replace(".yml", ".schem");
    }
}
