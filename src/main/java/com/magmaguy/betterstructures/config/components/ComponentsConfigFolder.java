package com.magmaguy.betterstructures.config.components;

import com.magmaguy.betterstructures.MetadataHandler;

import java.io.File;

public class ComponentsConfigFolder {
    public static void initialize() {
        File file = new File(MetadataHandler.PLUGIN.getDataFolder().getPath() + File.separatorChar + "components");
        if (!file.exists()) {
            file.mkdir();
        }
    }
}
