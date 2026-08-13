package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.util.SchematicFileUtils;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.betterstructures.worldedit.SchematicConversionLog;
import com.magmaguy.magmacore.config.CustomConfig;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulesConfig extends CustomConfig {
    @Getter
    private static final HashMap<String, ModulesConfigFields> moduleConfigurations = new HashMap<>();
    //Module schematics are parsed on a single thread, so re-reading unchanged ones on every content
    //reload costs even more per file here than it does for structures.
    private static final SchematicClipboardCache clipboardCache = new SchematicClipboardCache();

    public ModulesConfig() {
        super("modules", ModulesConfigFields.class);
        moduleConfigurations.clear();

        ModulesContainer.initializeSpecialModules();

        File modulesFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath()+ File.separatorChar + "modules");
        if (!modulesFile.exists()) modulesFile.mkdir();

        HashMap<File, Clipboard> clipboards = new HashMap<>();
        //Initialize schematics
        File[] moduleFiles = modulesFile.listFiles();
        List<File> discoveredModuleFiles = new ArrayList<>();
        if (moduleFiles != null) {
            for (File file : moduleFiles) {
                SchematicFileUtils.scanDirectoryForSchematics(file, discoveredModuleFiles);
            }
        }
        discoveredModuleFiles.sort(
                Comparator.comparing(File::getAbsolutePath));
        Map<String, File> moduleSourcesByFilename = new HashMap<>();
        //Modules that are gone stop being cached before anything reads the cache, so a deleted
        //module schematic can never be served out of the previous load.
        clipboardCache.retainOnly(discoveredModuleFiles);
        try (SchematicConversionLog.Session conversionLog = SchematicConversionLog.capture()) {
            for (File file : discoveredModuleFiles) {
                File previous = moduleSourcesByFilename.putIfAbsent(
                        file.getName(),
                        file);
                if (previous != null && !previous.equals(file)) {
                    throw new IllegalStateException(
                            "Duplicate module schematic filename '"
                                    + file.getName() + "' exists at both "
                                    + previous.getPath() + " and "
                                    + file.getPath()
                                    + "; module configuration lookup would be ambiguous.");
                }
                Clipboard clipboard = clipboardCache.get(file);
                if (clipboard == null) {
                    clipboard = Schematic.load(file);
                    if (clipboard != null) clipboardCache.put(file, clipboard);
                }
                if (clipboard == null) {
                    throw new IllegalStateException(
                            "Failed to load module schematic " + file.getPath()
                                    + "; refusing to initialize a partial module registry.");
                }
                clipboards.put(file, clipboard);
            }
        }

        for (File file : discoveredModuleFiles) {
            String configurationName = SchematicFileUtils.convertFromSchematicFilename(file.getName());
            ModulesConfigFields moduleConfigField = new ModulesConfigFields(configurationName, true);
            new CustomConfig(file.getParent().replace(
                    MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar, ""),
                    ModulesConfigFields.class, moduleConfigField);
            moduleConfigurations.put(configurationName, moduleConfigField);
        }

        moduleConfigurations.values().forEach(ModulesConfigFields::validateClones);

        for (ModulesConfigFields modulesConfigFields : moduleConfigurations.values()) {
            if (!modulesConfigFields.isEnabled()) continue;
            String schematicFilename = SchematicFileUtils.convertFromConfigurationFilename(modulesConfigFields.getFilename());
            File source = moduleSourcesByFilename.get(schematicFilename);
            Clipboard clipboard = source == null ? null : clipboards.get(source);
            if (clipboard == null) {
                throw new IllegalStateException(
                        "Enabled module configuration "
                                + modulesConfigFields.getFilename()
                                + " has no readable schematic "
                                + schematicFilename
                                + "; refusing to initialize a partial module registry.");
            }
            ModulesContainer.initializeModulesContainer(
                    clipboard,
                    schematicFilename,
                    modulesConfigFields,
                    modulesConfigFields.getFilename());
        }

        ModulesContainer.postInitializeModulesContainer();

    }

    public static ModulesConfigFields getModuleConfiguration(String filename) {
        return moduleConfigurations.get(filename);
    }
}
