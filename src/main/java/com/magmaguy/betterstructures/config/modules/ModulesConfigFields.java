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
        this.treasureFile = processString("treasureFile", treasureFile, null, false);
        if (treasureFile != null && !treasureFile.isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(treasureFile);
            if (treasureConfigFields == null) {
                Logger.warn("Failed to get treasure config file " + treasureFile + " for schematic configuration " + filename + " ! Defaulting to the generator treasure.");
                return;
            }
            this.chestContents = treasureConfigFields.getChestContents();
        }
        this.borderMap = processMap("borders", new HashMap<>());
        this.minY = processInt("minY", minY, -4, false);
        this.maxY = processInt("maxY", maxY, 16, false);
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

    public enum BuildBorder {
        NORTH, SOUTH, EAST, WEST, UP, DOWN;

        @Nullable
        public static BuildBorder fromString(String s) {
            for (BuildBorder border : BuildBorder.values()) {
                if (border.name().equalsIgnoreCase(s)) {
                    return border;
                }
            }
            return null;
        }

        public BuildBorder getOpposite() {
            switch (this) {
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                default:
                    throw new IllegalArgumentException("Invalid BuildBorder");
            }
        }
    }
}
