package com.magmaguy.betterstructures.modules;

import java.util.List;
import java.util.Map;

public class Choice {
    private final GridCell gridCell;
    private final ModulesContainer modulesContainer;
    private final Map<GridCell, List<ModulesContainer>> previousPossibleModules;

    public Choice(GridCell gridCell, ModulesContainer modulesContainer, Map<GridCell, List<ModulesContainer>> previousPossibleModules) {
        this.gridCell = gridCell;
        this.modulesContainer = modulesContainer;
        this.previousPossibleModules = previousPossibleModules;
    }

    public GridCell getChunkData() {
        return gridCell;
    }

    public ModulesContainer getModulesContainer() {
        return modulesContainer;
    }

    public Map<GridCell, List<ModulesContainer>> getPreviousPossibleModules() {
        return previousPossibleModules;
    }
}
