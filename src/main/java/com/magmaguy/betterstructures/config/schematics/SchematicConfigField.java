package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.io.IOException;
import java.util.List;

public class SchematicConfigField extends CustomConfigFields {

    @Getter
    @Setter
    private double weight = 1;
    @Getter
    @Setter
    private String generatorConfigFilename = "";
    @Getter
    @Setter
    private GeneratorConfigFields generatorConfigFields;
    @Getter
    @Setter
    private Material pedestalMaterial = null;
    @Getter
    @Setter
    private String treasureFile = null;
    @Getter
    @Setter
    private ChestContents chestContents = null;
    @Getter
    @Setter
    private String barrelTreasureFilename = null;
    @Getter
    @Setter
    private ChestContents barrelContents = null;


    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public SchematicConfigField(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.weight = processDouble("weight", weight, 1, true);
        this.pedestalMaterial = processEnum("pedestalMaterial", pedestalMaterial, null, Material.class, false);
        this.generatorConfigFilename = processString("generatorConfigFilename", generatorConfigFilename, generatorConfigFilename, true);
        this.generatorConfigFields = GeneratorConfig.getConfigFields(generatorConfigFilename);
        this.treasureFile = processString("treasureFile", treasureFile, null, false);
        this.barrelTreasureFilename = processString("barrelTreasureFilename", barrelTreasureFilename, null, false);
        if (generatorConfigFields == null) {
            logInvalidGeneratorConfiguration();
            return;
        }
        // Inherit defaults from the generator
        this.chestContents = generatorConfigFields.getChestContents();
        this.barrelContents = generatorConfigFields.getBarrelContents();
        // Per-schematic chest treasure override
        if (treasureFile != null && !treasureFile.isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(treasureFile);
            if (treasureConfigFields == null) {
                Logger.warn("Failed to get treasure config file " + treasureFile + " for schematic configuration " + filename + " ! Defaulting to the generator treasure.");
            } else {
                this.chestContents = treasureConfigFields.getChestContents();
            }
        }
        // Per-schematic barrel treasure override
        if (barrelTreasureFilename != null && !barrelTreasureFilename.isEmpty()) {
            TreasureConfigFields barrelTreasureConfigFields = TreasureConfig.getConfigFields(barrelTreasureFilename);
            if (barrelTreasureConfigFields == null) {
                Logger.warn("Failed to get barrel treasure config file " + barrelTreasureFilename + " for schematic configuration " + filename + " ! Defaulting to the generator barrel treasure.");
            } else {
                this.barrelContents = barrelTreasureConfigFields.getChestContents();
            }
        }
    }

    private void logInvalidGeneratorConfiguration() {
        String configPath = file == null ? filename : file.getPath();
        String configuredGenerator = generatorConfigFilename == null ? "" : generatorConfigFilename.trim();
        List<String> generatorExamples = GeneratorConfig.getGeneratorConfigurations().keySet().stream()
                .sorted()
                .limit(8)
                .toList();

        Logger.warn("============================================================");
        Logger.warn("BetterStructures could not load schematic config " + filename + ".");
        Logger.warn("Config file: " + configPath);
        if (configuredGenerator.isEmpty()) {
            Logger.warn("Problem: generatorConfigFilename is empty.");
            Logger.warn("New schematic configs are generated with this field blank until an admin chooses a generator.");
        } else {
            Logger.warn("Problem: generatorConfigFilename '" + generatorConfigFilename + "' does not match any loaded generator config.");
        }
        Logger.warn("Fix: set generatorConfigFilename to a file from plugins/BetterStructures/generators, for example generator_surface_global.yml.");
        if (!generatorExamples.isEmpty()) {
            Logger.warn("Loaded generator examples: " + String.join(", ", generatorExamples));
        }
        Logger.warn("This schematic was scanned, but it will not spawn or appear in /bs place until the generator is valid. Run /bs reload after editing it.");
        Logger.warn("============================================================");
    }

    public void toggleEnabled(boolean enabled) {
        this.isEnabled = enabled;
        fileConfiguration.set("isEnabled", enabled);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
