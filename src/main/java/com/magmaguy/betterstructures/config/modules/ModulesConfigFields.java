package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Biome;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private String moduleBiome = "default";
    private String minecraftBiomeString = "null";
    @Getter
    private Biome minecraftBiome = null;
    @Getter
    private String cloneConfig = "";
    private ModulesConfigFields clonedConfig = null;
    private boolean northIsPassable = true;
    private boolean southIsPassable = true;
    private boolean eastIsPassable = true;
    private boolean westIsPassable = true;
    private boolean upIsPassable = true;
    private boolean downIsPassable = true;
    private UUID uuid = UUID.randomUUID();
    private String compoundModule = null;

    //used to check if a config is either cloned or the same between two modules
    public UUID getUuid() {
        return clonedConfig == null ? uuid : clonedConfig.getUuid();
    }

    public String getCompoundModule() {
        return clonedConfig == null ? compoundModule : clonedConfig.getCompoundModule();
    }

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

    public boolean isNorthIsPassable() {
        return clonedConfig == null ? northIsPassable : clonedConfig.isNorthIsPassable();
    }

    public boolean isSouthIsPassable() {
        return clonedConfig == null ? southIsPassable : clonedConfig.isSouthIsPassable();
    }

    public boolean isEastIsPassable() {
        return clonedConfig == null ? eastIsPassable : clonedConfig.isEastIsPassable();
    }

    public boolean isWestIsPassable() {
        return clonedConfig == null ? westIsPassable : clonedConfig.isWestIsPassable();
    }

    public boolean isUpIsPassable() {
        return clonedConfig == null ? upIsPassable : clonedConfig.isUpIsPassable();
    }

    public boolean isDownIsPassable() {
        return clonedConfig == null ? downIsPassable : clonedConfig.isDownIsPassable();
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
        this.moduleBiome = processString("biome", moduleBiome, moduleBiome, true);
        this.minecraftBiomeString = processString("minecraftBiome", minecraftBiomeString, minecraftBiomeString, true);
        if (!minecraftBiomeString.equalsIgnoreCase("null"))
            try {
                this.minecraftBiome = Biome.valueOf(minecraftBiomeString.toUpperCase());
            } catch (Exception e) {
                Logger.warn("Biome " + minecraftBiomeString + " is not a valid biome! Fix it in " + filename);
            }
        this.cloneConfig = processString("cloneConfig", cloneConfig, cloneConfig, true);
//        this.northIsPassable = processBoolean("northIsPassable", northIsPassable, northIsPassable, true);
//        this.southIsPassable = processBoolean("southIsPassable", southIsPassable, southIsPassable, true);
//        this.eastIsPassable = processBoolean("eastIsPassable", eastIsPassable, eastIsPassable, true);
//        this.westIsPassable = processBoolean("westIsPassable", westIsPassable, westIsPassable, true);
//        this.upIsPassable = processBoolean("upIsPassable", upIsPassable, upIsPassable, true);
//        this.downIsPassable = processBoolean("downIsPassable", downIsPassable, downIsPassable, true);
        this.compoundModule = processString("compoundModule", compoundModule, compoundModule, true);
    }

    public void validateClones() {
        if (cloneConfig.isEmpty()) return;
        clonedConfig = ModulesConfig.getModuleConfiguration(cloneConfig);
        if (clonedConfig == null) {
            Logger.warn("Configuration " + filename + " is supposed to clone " + clonedConfig + " but that is not a valid configuration file! The cloning setting will be ignored.");
            return;
        } else
            Logger.info("Cloned " + filename + " into " + clonedConfig.getFilename());
        fileConfiguration.set("treasureFile", null);
        fileConfiguration.set("borders", null);
        fileConfiguration.set("minY", null);
        fileConfiguration.set("maxY", null);
        fileConfiguration.set("enforceVerticalRotation", null);
        fileConfiguration.set("noRepeat", null);
        fileConfiguration.set("weight", null);
        fileConfiguration.set("repetitionPenalty", null);
        fileConfiguration.set("enforceHorizontalRotation", null);
        fileConfiguration.set("northIsPassable", null);
        fileConfiguration.set("southIsPassable", null);
        fileConfiguration.set("eastIsPassable", null);
        fileConfiguration.set("westIsPassable", null);
        fileConfiguration.set("upIsPassable", null);
        fileConfiguration.set("downIsPassable", null);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
