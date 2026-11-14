package com.magmaguy.betterstructures.config.spawnpools;

import com.magmaguy.magmacore.config.CustomConfigFields;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SpawnPoolsConfigFields extends CustomConfigFields {
    @Getter
    private List<String> poolStrings = new ArrayList<>();
    @Getter
    private int maxLevel = -1;
    public SpawnPoolsConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        poolStrings = processStringList("poolStrings", poolStrings, poolStrings, true);
        maxLevel = processInt("maxLevel", maxLevel, maxLevel, false);
    }
}
