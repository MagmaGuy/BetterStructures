package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.CustomConfigFields;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.WarningMessage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

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
        this.generatorConfigFilename = processString("generatorConfigFilename", generatorConfigFilename, null, true);
        this.generatorConfigFields = GeneratorConfig.getConfigFields(generatorConfigFilename);
        this.treasureFile = processString("treasureFile", treasureFile, null, false);
        if (generatorConfigFields == null) {
            new WarningMessage("Failed to assign a valid generator to " + filename + "! This will not spawn.");
            return;
        }
        this.chestContents = generatorConfigFields.getChestContents();
        if (treasureFile != null && !treasureFile.isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(treasureFile);
            if (treasureConfigFields == null) {
                new WarningMessage("Failed to get treasure config file " + treasureFile + " for schematic configuration " + filename + " ! Defaulting to the generator treasure.");
                return;
            }
            this.chestContents = treasureConfigFields.getChestContents();
        }
    }
}
