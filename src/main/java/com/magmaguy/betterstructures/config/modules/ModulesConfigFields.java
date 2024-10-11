package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModulesConfigFields extends CustomConfigFields {

    @Setter
    private String treasureFile = null;
    @Setter
    private ChestContents chestContents = null;
    private Map<String, Object> borderMap = new HashMap<>();
    private Integer minY = -4;
    private Integer maxY = 20;
    private boolean enforceVerticalRotation = false;
    private boolean enforceHorizontalRotation = false;
    private boolean noRepeat = false;
    private double weight = 100D;
    private double repetitionPenalty = 0;
    @Getter
    private String biome = "default";
    @Getter
    private String cloneConfig = "";
    private ModulesConfigFields clonedConfig = null;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public ModulesConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    public String getTreasureFile() {
        return clonedConfig == null ? treasureFile : clonedConfig.getTreasureFile();
    }

    public ChestContents getChestContents() {
        return clonedConfig == null ? chestContents : clonedConfig.getChestContents();
    }

    public Map<String, Object> getBorderMap() {
        return clonedConfig == null ? borderMap : clonedConfig.getBorderMap();
    }

    public Integer getMinY() {
        return clonedConfig == null ? minY : clonedConfig.getMinY();
    }

    public Integer getMaxY() {
        return clonedConfig == null ? maxY : clonedConfig.getMaxY();
    }

    public boolean isEnforceVerticalRotation() {
        return clonedConfig == null ? enforceVerticalRotation : clonedConfig.isEnforceVerticalRotation();
    }

    public boolean isEnforceHorizontalRotation() {
        return clonedConfig == null ? enforceHorizontalRotation : clonedConfig.isEnforceHorizontalRotation();
    }

    public boolean isNoRepeat() {
        return clonedConfig == null ? noRepeat : clonedConfig.isNoRepeat();
    }

    public double getWeight() {
        return clonedConfig == null ? weight : clonedConfig.getWeight();
    }

    public double getRepetitionPenalty() {
        return clonedConfig == null ? repetitionPenalty : clonedConfig.getRepetitionPenalty();
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.treasureFile = processString("treasureFile", treasureFile, null, true);
        if (treasureFile != null && !treasureFile.isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(treasureFile);
            if (treasureConfigFields == null) {
                Logger.warn("Failed to get treasure config file " + treasureFile + " for schematic configuration " + filename + " ! Defaulting to the generator treasure.");
                return;
            }
            this.chestContents = treasureConfigFields.getChestContents();
        }
        this.borderMap = processMap("borders", new HashMap<>());
        this.minY = processInt("minY", minY, minY, true);
        this.maxY = processInt("maxY", maxY, maxY, true);
        this.enforceVerticalRotation = processBoolean("enforceVerticalRotation", enforceVerticalRotation, enforceVerticalRotation, true);
        this.noRepeat = processBoolean("noRepeat", noRepeat, noRepeat, true);
        this.weight = processDouble("weight", weight, weight, true);
        this.repetitionPenalty = processDouble("repetitionPenalty", repetitionPenalty, repetitionPenalty, true);
        this.enforceHorizontalRotation = processBoolean("enforceHorizontalRotation", enforceHorizontalRotation, enforceHorizontalRotation, true);
        this.biome = processString("biome", biome, biome, true);
        this.cloneConfig = processString("cloneConfig", cloneConfig, cloneConfig, true);
    }

    public void validateClones() {
        if (cloneConfig.isEmpty()) return;
        clonedConfig = ModulesConfig.getModuleConfiguration(cloneConfig);
        if (clonedConfig == null) {
            Logger.warn("Configuration " + filename + " is supposed to clone " + clonedConfig + " but that is not a valid configuration file! The cloning setting will be ignored.");
            return;
        } else
            Logger.info("Cloned " + filename + " into " + clonedConfig.getFilename());
//        fileConfiguration.set("treasureFile", null);
//        fileConfiguration.set("borders", null);
//        fileConfiguration.set("minY", null);
//        fileConfiguration.set("maxY", null);
//        fileConfiguration.set("enforceVerticalRotation", null);
//        fileConfiguration.set("noRepeat", null);
//        fileConfiguration.set("weight", null);
//        fileConfiguration.set("repetitionPenalty", null);
//        fileConfiguration.set("enforceHorizontalRotation", null);
//        try {
//            fileConfiguration.save(file);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
}
