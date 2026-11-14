package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.util.SchematicFileUtils;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.betterstructures.worldedit.SchematicConversionLog;
import com.magmaguy.betterstructures.worldedit.SchematicDiskCache;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.config.CustomConfig;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

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
        if (shutdownRequested()) return;
        File modulesFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath()+ File.separatorChar + "modules");
        if (!modulesFile.exists()) modulesFile.mkdir();

        HashMap<File, Clipboard> clipboards = new HashMap<>();
        //Initialize schematics
        File[] moduleFiles = modulesFile.listFiles();
        List<File> discoveredModuleFiles = new ArrayList<>();
        if (moduleFiles == null) throw new java.io.UncheckedIOException(
                new java.io.IOException("Could not enumerate module root: " + modulesFile));
        {
            for (File file : moduleFiles) {
                SchematicFileUtils.scanDirectoryForSchematics(file, discoveredModuleFiles);
            }
        }
        discoveredModuleFiles.sort(
                Comparator.comparing(File::getAbsolutePath));
        Map<String, File> moduleSourcesByFilename = new HashMap<>();
        //Modules that are gone stop being cached before anything reads the cache, so a deleted
        //module schematic can never be served out of the previous load.
        int forgotten = clipboardCache.retainOnly(discoveredModuleFiles);
        if (!discoveredModuleFiles.isEmpty()) {
            //Module schematics ship at an older DataVersion just like structure schematics do, so
            //without this every cold start re-ran Mojang's data converter over every module file —
            //the last remaining source of legacy-conversion cost and log spam at startup.
            SchematicDiskCache diskCache =
                    new SchematicDiskCache(SchematicDiskCache.moduleCacheFolder());
            try (SchematicConversionLog.Session conversionLog = SchematicConversionLog.capture()) {
                for (File file : discoveredModuleFiles) {
                    if (shutdownRequested()) return;
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
                    Clipboard clipboard = clipboardCache.get(file, diskCache);
                    if (clipboard == null) {
                        SchematicDiskCache.LoadedClipboard loaded = diskCache.loadWithIdentity(file);
                        clipboard = loaded == null ? null : loaded.clipboard();
                        if (loaded != null) clipboardCache.put(file, loaded);
                    }
                    if (clipboard == null) {
                        throw new IllegalStateException(
                                "Failed to load module schematic " + file.getPath()
                                        + "; refusing to initialize a partial module registry.");
                    }
                    clipboards.put(file, clipboard);
                }
            }
            if (diskCache.filesRead() > 0 || forgotten > 0)
                diskCache.pruneStaleEntries(discoveredModuleFiles);
        }

        if (shutdownRequested()) return;
        moduleConfigurations.clear();
        ModulesContainer.initializeSpecialModules();
        for (File file : discoveredModuleFiles) {
            if (shutdownRequested()) return;
            String configurationName = SchematicFileUtils.convertFromSchematicFilename(file.getName());
            ModulesConfigFields moduleConfigField = new ModulesConfigFields(configurationName, true);
            new CustomConfig(file.getParent().replace(
                    MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar, ""),
                    ModulesConfigFields.class, moduleConfigField);
            moduleConfigurations.put(configurationName, moduleConfigField);
        }

        ModulesConfigFields.validateCloneGraph(moduleConfigurations.values());
        moduleConfigurations.values().forEach(ModulesConfigFields::validateClones);

        for (ModulesConfigFields modulesConfigFields : moduleConfigurations.values()) {
            if (shutdownRequested()) return;
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

        if (!shutdownRequested()) ModulesContainer.postInitializeModulesContainer();

    }

    private static boolean shutdownRequested() {
        return MagmaCore.isShutdownRequested((JavaPlugin) MetadataHandler.PLUGIN);
    }

    public static ModulesConfigFields getModuleConfiguration(String filename) {
        return moduleConfigurations.get(filename);
    }
}
