package com.magmaguy.betterstructures.config.spawnpools;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SpawnPoolsConfigFields extends CustomConfigFields {
    @Getter
    private List<String> poolStrings = new ArrayList<>();
    public SpawnPoolsConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        poolStrings = processStringList("poolStrings", poolStrings, poolStrings, true);
        Logger.debug("list: " + poolStrings.toString());
    }
}
