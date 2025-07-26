package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.magmacore.config.CustomConfig;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;

public class ModulesConfig extends CustomConfig {
    @Getter
    private static final HashMap<String, ModulesConfigFields> moduleConfigurations = new HashMap<>();

    public ModulesConfig() {
        super("modules", ModulesConfigFields.class);
        moduleConfigurations.clear();

        ModulesContainer.initializeSpecialModules();

        File modulesFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath()+ File.separatorChar + "modules");
        if (!modulesFile.exists()) modulesFile.mkdir();

        HashMap<File, Clipboard> clipboards = new HashMap();
        //Initialize schematics
        for (File file : new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "modules").listFiles())
            scanDirectoryForSchematics(file, clipboards);

        for (String key : super.getCustomConfigFieldsHashMap().keySet())
            moduleConfigurations.put(key, (ModulesConfigFields) super.getCustomConfigFieldsHashMap().get(key));

        for (File file : clipboards.keySet()) {
            String configurationName = convertFromSchematicFilename(file.getName());
            ModulesConfigFields moduleConfigField = new ModulesConfigFields(configurationName, true);
            new CustomConfig(file.getParent().replace(
                    MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar, ""),
                    ModulesConfigFields.class, moduleConfigField);
            moduleConfigurations.put(configurationName, moduleConfigField);
        }

        moduleConfigurations.values().forEach(ModulesConfigFields::validateClones);

        for (ModulesConfigFields modulesConfigFields : moduleConfigurations.values()) {
            if (!modulesConfigFields.isEnabled()) continue;
            String schematicFilename = convertFromConfigurationFilename(modulesConfigFields.getFilename());
            Clipboard clipboard = null;
            for (File file : clipboards.keySet())
                if (file.getName().equals(schematicFilename)) {
                    clipboard = clipboards.get(file);
                    break;
                }
            ModulesContainer.initializeModulesContainer(
                    clipboard,
                    schematicFilename,
                    modulesConfigFields,
                    modulesConfigFields.getFilename());
        }

        ModulesContainer.postInitializeModulesContainer();

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

    public static ModulesConfigFields getModuleConfiguration(String filename) {
        return moduleConfigurations.get(filename);
    }
}
