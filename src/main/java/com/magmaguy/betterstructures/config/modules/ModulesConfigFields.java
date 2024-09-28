package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModulesConfigFields extends CustomConfigFields {

    @Getter
    @Setter
    private String treasureFile = null;
    @Getter
    @Setter
    private ChestContents chestContents = null;
    @Getter
    private Map<String, Object> borderMap = new HashMap<>();
    @Getter
    private Integer minY = -4;
    @Getter
    private Integer maxY = 16;
    @Getter
    private boolean enforceVerticalRotation = false;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public ModulesConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
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
        this.minY = processInt("minY", minY, -4, true);
        this.maxY = processInt("maxY", maxY, 16, true);
        this.enforceVerticalRotation = processBoolean("enforceVerticalRotation", enforceVerticalRotation, enforceVerticalRotation, true);
    }
}
