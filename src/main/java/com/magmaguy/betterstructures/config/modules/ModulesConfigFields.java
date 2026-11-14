package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Setter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModulesConfigFields extends CustomConfigFields {

    @Setter
    private String barrelTreasureFilename = "treasure_barrel_food.yml";
    @Setter
    private boolean generateLootInBarrels = true;
    private Map<String, Object> borderMap = new HashMap<>();
    private Integer minY = -4;
    private Integer maxY = 20;
    private boolean enforceVerticalRotation = false;
    private boolean enforceHorizontalRotation = false;
    private boolean noRepeat = false;
    private double weight = 100D;
    private double repetitionPenalty = 0;
    private String cloneConfig = "";
    private ModulesConfigFields clonedConfig = null;
    private UUID uuid = UUID.randomUUID();
    private boolean isAutomaticallyPlaced = true;

    //used to check if a config is either cloned or the same between two modules
    public UUID getUuid() {
        return clonedConfig == null ? uuid : clonedConfig.getUuid();
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

    public String getBarrelTreasureFilename() {
        return clonedConfig == null ? barrelTreasureFilename : clonedConfig.getBarrelTreasureFilename();
    }

    public boolean isGenerateLootInBarrels() {
        return clonedConfig == null ? generateLootInBarrels : clonedConfig.isGenerateLootInBarrels();
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

    public boolean isAutomaticallyPlaced() {
        return clonedConfig == null ? isAutomaticallyPlaced : clonedConfig.isAutomaticallyPlaced();
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.generateLootInBarrels = processBoolean("generateLootInBarrels", generateLootInBarrels, true, true);
        this.barrelTreasureFilename = processString("barrelTreasureFilename", barrelTreasureFilename, "treasure_barrel_food.yml", true);
        this.borderMap = processMap("borders", new HashMap<>());
        this.minY = processInt("minY", minY, minY, true);
        this.maxY = processInt("maxY", maxY, maxY, true);
        this.enforceVerticalRotation = processBoolean("enforceVerticalRotation", enforceVerticalRotation, enforceVerticalRotation, true);
        this.noRepeat = processBoolean("noRepeat", noRepeat, noRepeat, true);
        this.weight = processDouble("weight", weight, weight, true);
        if (!Double.isFinite(weight) || weight < 0) {
            Logger.warn("Invalid weight in " + filename + "; expected a finite nonnegative number. Disabling selection.");
            weight = 0;
        }
        this.repetitionPenalty = processDouble("repetitionPenalty", repetitionPenalty, repetitionPenalty, true);
        this.enforceHorizontalRotation = processBoolean("enforceHorizontalRotation", enforceHorizontalRotation, enforceHorizontalRotation, true);
        this.cloneConfig = processString("cloneConfig", cloneConfig, cloneConfig, true);
        this.isAutomaticallyPlaced = processBoolean("isAutomaticallyPlaced", isAutomaticallyPlaced, isAutomaticallyPlaced, true);
    }

    static void validateCloneGraph(java.util.Collection<ModulesConfigFields> configurations) {
        java.util.Set<ModulesConfigFields> resolved = new java.util.HashSet<>();
        for (ModulesConfigFields configuration : configurations) {
            java.util.Set<ModulesConfigFields> active = new java.util.LinkedHashSet<>();
            ModulesConfigFields cursor = configuration;
            while (cursor != null && !resolved.contains(cursor)) {
                if (!active.add(cursor)) {
                    String chain = active.stream().map(ModulesConfigFields::getFilename)
                            .collect(java.util.stream.Collectors.joining(" -> "));
                    throw new IllegalStateException("Cyclic module clone configuration: " + chain
                            + " -> " + cursor.getFilename());
                }
                cursor = cursor.cloneConfig.isEmpty() ? null
                        : ModulesConfig.getModuleConfiguration(cursor.cloneConfig);
            }
            resolved.addAll(active);
        }
    }

    public void validateClones() {
        if (cloneConfig.isEmpty()) return;
        clonedConfig = ModulesConfig.getModuleConfiguration(cloneConfig);
        if (clonedConfig == null) {
            Logger.warn("Configuration " + filename + " is supposed to clone " + cloneConfig + " but that is not a valid configuration file! The cloning setting will be ignored.");
            return;
        }
        fileConfiguration.set("barrelTreasureFilename", null);
        fileConfiguration.set("borders", null);
        fileConfiguration.set("minY", null);
        fileConfiguration.set("maxY", null);
        fileConfiguration.set("enforceVerticalRotation", null);
        fileConfiguration.set("noRepeat", null);
        fileConfiguration.set("weight", null);
        fileConfiguration.set("repetitionPenalty", null);
        fileConfiguration.set("enforceHorizontalRotation", null);
        fileConfiguration.set("generateLootInBarrels", null);
        fileConfiguration.set("isAutomaticallyPlaced", null);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
