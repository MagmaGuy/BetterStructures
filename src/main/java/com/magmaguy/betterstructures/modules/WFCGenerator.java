package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.magmaguy.betterstructures.modules.ModulesContainer.pickWeightedRandomModule;

public class WFCGenerator {
    public static HashSet<WFCGenerator> wfcGenerators = new HashSet<>();
    @Getter
    private ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;

    @Getter
    private WFCLattice spatialGrid;
    private Player player = null;
    private String startingModule;
    @Getter
    private World world;
    @Getter
    private Location startLocation = null;
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;
    private File worldFolder;
    private String worldName;
    private int rollbackCounter = 0;
    private BossBar progressBar;
    private int totalNodes = 0;
    private int completedNodes = 0;

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Player player) {
        this.player = player;
        this.startLocation = player.getLocation();
        initialize(moduleGeneratorsConfigFields);
    }

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Location startLocation) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.startLocation = startLocation;
        initialize(moduleGeneratorsConfigFields);
    }

    public static void generateFromConfig(ModuleGeneratorsConfigFields generatorsConfigFields, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (generatorsConfigFields.isWorldGeneration()) {
                    String baseWorldName = generatorsConfigFields.getFilename().replace(".yml", "");
                    File worldContainer = Bukkit.getWorldContainer();
                    int i = 0;

                    // Increment until a unique world name is found
                    String worldName;
                    worldName = baseWorldName + "_" + i;
                    File worldFolder = new File(worldContainer, worldName);
                    while (worldFolder.exists()) {
                        worldName = baseWorldName + "_" + i;
                        i++;
                        worldFolder = new File(worldContainer, worldName);
                    }

                } else {
                    new WFCGenerator(generatorsConfigFields, player);
                }
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        wfcGenerators.forEach(WFCGenerator::cancel);
        wfcGenerators.clear();
    }

    private void initializeProgressBar() {
        if (player != null) {
            progressBar = Bukkit.createBossBar("Generating Structure...", BarColor.BLUE, BarStyle.SOLID);
            progressBar.addPlayer(player);
            progressBar.setProgress(0.0);
        }
    }

    private void updateProgressBar(String message) {
        if (progressBar != null && totalNodes > 0) {
            double progress = (double) completedNodes / totalNodes;
            progressBar.setProgress(Math.min(progress, 1.0));
            progressBar.setTitle(message);
        }
    }

    private void removeProgressBar() {
        if (progressBar != null) {
            progressBar.removeAll();
            progressBar = null;
        }
    }

    private void initialize(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.spatialGrid = new WFCLattice(moduleGeneratorsConfigFields.getRadius(), moduleGeneratorsConfigFields.getModuleSizeXZ(), moduleGeneratorsConfigFields.getModuleSizeY(), moduleGeneratorsConfigFields.getMinChunkY(), moduleGeneratorsConfigFields.getMaxChunkY());
        wfcGenerators.add(this);

        // Calculate total nodes for progress tracking
        int radius = moduleGeneratorsConfigFields.getRadius();
        int minY = moduleGeneratorsConfigFields.getMinChunkY();
        int maxY = moduleGeneratorsConfigFields.getMaxChunkY();
        totalNodes = (radius * 2 + 1) * (radius * 2 + 1) * (maxY - minY + 1);

        List<String> startModules = moduleGeneratorsConfigFields.getStartModules();
        if (startModules.isEmpty()) {
            if (player != null) player.sendMessage("No start modules exist, you need to install or make modules first!");
            Logger.warn("No start modules exist, you need to install or make modules first!");
            cancel();
            return;
        }
        this.startingModule = startModules.get(ThreadLocalRandom.current().nextInt(moduleGeneratorsConfigFields.getStartModules().size())) + "_rotation_0";

        if (moduleGeneratorsConfigFields.isWorldGeneration()) {
            String baseWorldName = moduleGeneratorsConfigFields.getFilename().replace(".yml", "");
            File worldContainer = Bukkit.getWorldContainer();
            int i = 0;

            // Increment until a unique world name is found
            String worldName;
            worldName = baseWorldName + "_" + i;
            File worldFolder = new File(worldContainer, worldName);
            while (worldFolder.exists()) {
                worldName = baseWorldName + "_" + i;
                i++;
                worldFolder = new File(worldContainer, worldName);
            }
        }

        initializeProgressBar();
        reserveChunks();
    }

    private void reserveChunks() {
        updateProgressBar("Initializing lattice...");
        if (moduleGeneratorsConfigFields.isWorldGeneration()) {
            this.world = WorldInitializer.generateWorld(worldName, player);
        } else {
            this.world = startLocation.getWorld();
        }

        spatialGrid.initializeLattice(world, this);
        startArrangingModules();
    }

    private void startArrangingModules() {
        updateProgressBar("Starting generation...");
        new InitializeGenerationTask().runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        updateProgressBar("Collapsing initial node...");

        try {
            WFCNode startChunk = createStartChunk(startingModule);
            if (startChunk == null) {
                return;
            }

            generateFast();

        } catch (Exception e) {
            Logger.warn("Error during generation: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    private WFCNode createStartChunk(String startingModule) {
        WFCNode startCell = spatialGrid.getNodeMap().get(new Vector3i());

        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.warn("Starting module was null! Cancelling!");
            return null;
        }

        paste(startCell, modulesContainer);
        completedNodes++;
        return startCell;
    }

    private void generateFast() {
        updateProgressBar("Propagating constraints...");
        while (!isCancelled) {
            WFCNode nextCell = spatialGrid.getLowestEntropyNode();
            if (nextCell == null) {
                done();
                break;
            }

            generateNextChunk(nextCell);
        }
    }

    private void paste(WFCNode gridCell, ModulesContainer modulesContainer) {
        // Record the decision for backtracking
        spatialGrid.recordCollapseDecision(gridCell, modulesContainer);

        gridCell.setModulesContainer(modulesContainer);
        gridCell.getOrientedNeighbors().values().forEach(spatialGrid::updateNodeEntropy);
    }

    private void generateNextChunk(WFCNode gridCell) {
        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            updateProgressBar("Backtracking...");
            org.bukkit.Location targetLocation = gridCell.getRealCenterLocation();
//            if (player != null)
//                player.spigot().sendMessage(Logger.commandHoverMessage("Rolling back cell at " + gridCell.getCellLocation(), "Click to teleport", "tp " + targetLocation.getX() + " " + targetLocation.getY() + " " + targetLocation.getZ()));
            rollbackChunk();
            return;
        }

        ModulesContainer modulesContainer = pickWeightedRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
            updateProgressBar("Backtracking...");
            rollbackChunk();
            return;
        }

        paste(gridCell, modulesContainer);
        completedNodes++;
        updateProgressBar("Generating... (" + completedNodes + "/" + totalNodes + ")");
    }

    private void rollbackChunk() {
        // Use proper backtracking instead of just resetting
        if (spatialGrid.backtrack()) {
            updateProgressBar("Backtracking... (" + spatialGrid.getBacktrackDepth() + " decisions remaining)");
        } else {
            updateProgressBar("Generation failed - no decisions to backtrack");
            cancel();
            return;
        }

        rollbackCounter++;
        if (rollbackCounter > 1000) {
            updateProgressBar("Generation failed - exceeded backtrack limit");
            Logger.warn("Exceeded backtrack limit!");
            cancel();
            //retry
            if (player != null) new WFCGenerator(moduleGeneratorsConfigFields, player);
            else new WFCGenerator(moduleGeneratorsConfigFields, startLocation);
        }
    }

    private void done() {
        updateProgressBar("Generation complete!");
        if (player != null) {
            player.sendMessage("Done assembling!");
            player.sendMessage("It will take a moment to paste the structure, and will require relogging.");
        }
        isGenerating = false;
        instantPaste();
        spatialGrid.clearGenerationData();
        removeProgressBar();
    }

    private void cleanup() {
        spatialGrid.clearAllData();
        wfcGenerators.remove(this);
        removeProgressBar();
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
        removeProgressBar();
    }

    private void instantPaste() {
        // This guarantees that the paste order is grouped by chunk, making pasting faster down the line.
        Deque<WFCNode> orderedPasteDeque = new ArrayDeque<>();
        for (int x = -spatialGrid.getLatticeRadius(); x < spatialGrid.getLatticeRadius(); x++) {
            for (int z = -spatialGrid.getLatticeRadius(); z < spatialGrid.getLatticeRadius(); z++) {
                for (int y = spatialGrid.getMinYLevel(); y <= spatialGrid.getMaxYLevel(); y++) {
                    // Remove the cell from the map and get it in one go.
                    WFCNode cell = spatialGrid.getNodeMap().remove(new Vector3i(x, y, z));
                    if (cell != null) {
                        orderedPasteDeque.add(cell);
                    }
                }
            }
        }

        new ModulePasting(world, worldFolder, orderedPasteDeque, moduleGeneratorsConfigFields.getSpawnPoolSuffix(), startLocation, moduleGeneratorsConfigFields);

        cleanup();
    }

    private class InitializeGenerationTask extends BukkitRunnable {
        @Override
        public void run() {
            start(startingModule);
        }
    }
}