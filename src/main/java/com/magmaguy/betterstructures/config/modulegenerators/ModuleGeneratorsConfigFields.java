package com.magmaguy.betterstructures.config.modulegenerators;

import com.magmaguy.betterstructures.config.modules.ModulesConfig;
import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ModuleGeneratorsConfigFields extends CustomConfigFields {
    @Getter
    protected int radius;
    @Getter
    protected boolean edges;
    protected List<String> startModules;
    @Getter
    protected int minChunkY;
    @Getter
    protected int maxChunkY;
    @Getter
    protected int moduleSizeXZ;
    @Getter
    protected int moduleSizeY;
    @Getter
    protected boolean debug;
    @Getter
    protected boolean useGradientLevels;
    @Getter
    protected String spawnPoolSuffix;
    @Getter
    protected boolean isWorldGeneration;
    @Getter
    protected String treasureFile;
    @Getter
    @Setter
    private List<String> validWorlds = null;
    @Getter
    @Setter
    private List<World.Environment> validWorldEnvironments = null;

    public List<String> getStartModules() {
        List<String> existingModules = new ArrayList<>();
        for (ModulesConfigFields value : ModulesConfig.getModuleConfigurations().values())
            if (startModules.contains(value.getFilename().replace(".yml", ".schem")))
                existingModules.add(value.getFilename().replace(".yml", ".schem"));
        return existingModules;
    }

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public ModuleGeneratorsConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    public ModuleGeneratorsConfigFields(String filename) {
        super(filename, true);
    }

    @Override
    public void processConfigFields() {
        this.radius = processInt("radius", radius, 1, true);
        this.edges = processBoolean("edges", edges, false, true);
        this.startModules = processStringList("startModule", startModules, null, true);
        this.minChunkY = processInt("minChunkY", minChunkY, 0, true);
        this.maxChunkY = processInt("maxChunkY", maxChunkY, 0, true);
        this.moduleSizeXZ = processInt("moduleSizeXZ", moduleSizeXZ, 16, true);
        this.moduleSizeY = processInt("moduleSizeY", moduleSizeY, 16, true);
        this.debug = processBoolean("debug", debug, false, true);
        this.useGradientLevels = processBoolean("useGradientLevels", useGradientLevels, useGradientLevels, true);
        this.spawnPoolSuffix = processString("spawnPoolSuffix", spawnPoolSuffix, spawnPoolSuffix, true);
        this.isWorldGeneration = processBoolean("isWorldGeneration", isWorldGeneration, isWorldGeneration, true);
        this.treasureFile = processString("treasureFile", treasureFile, null, false);
        this.validWorlds = processStringList("validWorlds", validWorlds, new ArrayList<>(), false);
        this.validWorldEnvironments = processEnumList("validWorldEnvironments", validWorldEnvironments, null, World.Environment.class, false);
    }
}
