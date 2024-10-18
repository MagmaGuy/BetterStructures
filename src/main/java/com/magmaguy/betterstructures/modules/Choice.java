package com.magmaguy.betterstructures.modules;

import java.util.List;
import java.util.Map;

public class Choice {
    private final ChunkData chunkData;
    private final ModulesContainer modulesContainer;
    private final Map<ChunkData, List<ModulesContainer>> previousPossibleModules;

    public Choice(ChunkData chunkData, ModulesContainer modulesContainer, Map<ChunkData, List<ModulesContainer>> previousPossibleModules) {
        this.chunkData = chunkData;
        this.modulesContainer = modulesContainer;
        this.previousPossibleModules = previousPossibleModules;
    }

    public ChunkData getChunkData() {
        return chunkData;
    }

    public ModulesContainer getModulesContainer() {
        return modulesContainer;
    }

    public Map<ChunkData, List<ModulesContainer>> getPreviousPossibleModules() {
        return previousPossibleModules;
    }
}
