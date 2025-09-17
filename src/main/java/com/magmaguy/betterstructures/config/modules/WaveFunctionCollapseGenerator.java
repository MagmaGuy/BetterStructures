package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.modules.*;
import com.magmaguy.betterstructures.util.distributedload.WorkloadRunnable;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.Round;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.magmaguy.betterstructures.modules.ModulesContainer.pickWeightedRandomModule;

public class WaveFunctionCollapseGenerator {
    public static HashSet<WaveFunctionCollapseGenerator> waveFunctionCollapseGenerators = new HashSet<>();
    @Getter
    private final ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;
    @Getter
    private final GenerationStats stats;
    @Getter
    private final SpatialGrid spatialGrid;
    private final Map<GridCell, Integer> chunkRollbackCounter;
    @Getter
    private final ModularGenerationStatus modularGenerationStatus;
    private final ModularWorld modularWorld = null;
    @Getter
    private World world;
    @Getter
    private Location startLocation = null;
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;
    private int chunksReserved;
    private File worldFolder;
    private final Player player;
    private final String startingModule;
    private String worldName;
    private  HashMap consecutiveFailureCounter = new HashMap<>();

    //todo: later make this take a player and a location and handle it statically
    public WaveFunctionCollapseGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Player player) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.player = player;
        this.startLocation = player.getLocation();
        this.spatialGrid = new SpatialGrid(moduleGeneratorsConfigFields.getRadius(), moduleGeneratorsConfigFields.getModuleSizeXZ(), moduleGeneratorsConfigFields.getModuleSizeY(), moduleGeneratorsConfigFields.getMinChunkY(), moduleGeneratorsConfigFields.getMaxChunkY());
        this.stats = new GenerationStats(moduleGeneratorsConfigFields.getRadius(), getSpatialGrid());
        this.chunkRollbackCounter = new HashMap<>();
        this.modularGenerationStatus = new ModularGenerationStatus(player);
        waveFunctionCollapseGenerators.add(this);
        modularGenerationStatus.startInitializing();

        this.startingModule = moduleGeneratorsConfigFields.getStartModules().get(ThreadLocalRandom.current().nextInt(moduleGeneratorsConfigFields.getStartModules().size())) + "_rotation_0";

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

        reserveChunks();
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
                    new WaveFunctionCollapseGenerator(generatorsConfigFields, player);
                }
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        ModularGenerationStatus.shutdown();
        waveFunctionCollapseGenerators.forEach(WaveFunctionCollapseGenerator::cancel);
        waveFunctionCollapseGenerators.clear();
    }

    private void reserveChunks() {
        Logger.debug("Starting chunk reservation");
        modularGenerationStatus.finishedInitializing();
        modularGenerationStatus.startReservingChunks();
        if (moduleGeneratorsConfigFields.isWorldGeneration()) {
            this.world = WorldInitializer.generateWorld(worldName, player);
        } else {
            this.world = startLocation.getWorld();
        }

        WorkloadRunnable reserveChunksTask = new WorkloadRunnable(.2, () -> {
            modularGenerationStatus.finishedReservingChunks(chunksReserved);
        });
        int realChunkRadius = (int) Math.ceil(spatialGrid.getChunkSizeXZ() / 16d) * spatialGrid.getGridRadius();
        int chunkCounter = realChunkRadius * realChunkRadius;
        for (int x = -realChunkRadius; x < realChunkRadius; x++) {
            for (int z = -realChunkRadius; z < realChunkRadius; z++) {
                int finalX = x;
                int finalZ = z;
                reserveChunksTask.addWorkload(() -> {
                    world.loadChunk(finalX, finalZ);
                    chunksReserved++;
                    modularGenerationStatus.updateProgressReservingChunks(chunksReserved / (double) chunkCounter);
                });
            }
        }
        reserveChunksTask.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        startArrangingModules();
    }

    private void startArrangingModules() {
        Logger.debug("Starting module arrangement");
        modularGenerationStatus.startArrangingModules();
        new InitializeGenerationTask().runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        Logger.debug("Starting generation with starting module " + startingModule);

        try {
            modularGenerationStatus.finishedArrangingModules();

            GridCell startChunk = createStartChunk(startingModule);
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

    private GridCell createStartChunk(String startingModule) {
        Logger.debug("Creating start chunk at " + startLocation + " with radius " + moduleGeneratorsConfigFields.getRadius() +
                " and chunk size " + moduleGeneratorsConfigFields.getModuleSizeXZ() + " and starting module " + startingModule);

        Logger.debug("Initializing start grid cell at origin (0,0,0)");
        GridCell startChunk = new GridCell(new Vector3i(), world, spatialGrid, spatialGrid.getCellMap(), this);

        Logger.debug("Adding start cell to spatial grid map");
        spatialGrid.getCellMap().put(new Vector3i(), startChunk);

        Logger.debug("Looking up modules container for " + startingModule);
        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.sendMessage(player, "Starting module was null! Canceling!");
            return null;
        }

        Logger.debug("Pasting starting module at origin");
        paste(new Vector3i(), modulesContainer);
        return startChunk;
    }

    private void generateFast() {
        while (!isCancelled) {
            GridCell nextCell = spatialGrid.getNextGridCell();
            if (nextCell == null) {
                done();
                break;
            }

            generateNextChunk(nextCell);
            if (stats.getGeneratedChunks() % 100 == 0) {
                updateProgress();
            }
        }
    }

    private void updateProgress() {
        double progress = stats.getProgress() * 100;
        String formattedProgress = Round.twoDecimalPlaces(progress) + "";
//        Logger.debug("[" + formattedProgress + "%] Generated " + stats.getGeneratedChunks() +
//                " out of " + stats.totalChunks + " chunks");
    }

    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer) {
        Logger.debug("Pasting chunk at " + chunkLocation + " with modules " + modulesContainer.getClipboardFilename());
        GridCell gridCell = spatialGrid.getCellMap().get(chunkLocation);
        if (gridCell == null) {
            Logger.warn("ChunkData not found at location: " + chunkLocation);
            return;
        }

        if (!gridCell.isGenerated()) {
            stats.incrementGeneratedChunks();
        }

        gridCell.processPaste(modulesContainer);
        updateNeighbors(gridCell);

        chunkRollbackCounter.remove(gridCell);
    }

    private void updateNeighbors(GridCell gridCell) {
        gridCell.getOrientedNeighbors().values().stream()
                .filter(neighbor -> neighbor != null && !neighbor.isGenerated())
                .forEach(spatialGrid::updateCellPriority);
    }

    private void generateNextChunk(GridCell gridCell) {
        Logger.debug("Generating next chunk at location: " + gridCell.getCellLocation());
        gridCell.updateValidOptions();

        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            Logger.debug("No valid options for cell at " + gridCell.getCellLocation() + ", initiating rollback");
            org.bukkit.Location targetLocation = gridCell.getRealCenterLocation();
            player.spigot().sendMessage(Logger.commandHoverMessage("Rolling back cell at " + gridCell.getCellLocation(), "Click to teleport", "tp "+ targetLocation.getX() + " " + targetLocation.getY() + " " + targetLocation.getZ()));
            rollbackChunk(gridCell);
            return;
        }

        ModulesContainer modulesContainer = pickWeightedRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
            Logger.debug("Failed to pick a module for cell at " + gridCell.getCellLocation());
            rollbackChunk(gridCell);
            return;
        }

        Logger.debug("picked module " + modulesContainer.getClipboardFilename() + " for coords " + gridCell.getCellLocation());

        paste(gridCell.getCellLocation(), modulesContainer);
    }

    private void rollbackChunk(GridCell gridCell) {
        Logger.debug("Starting rollback for chunk at: " + gridCell.getCellLocation());
        int rollbacks = stats.rollbackCounter.incrementAndGet();
        Logger.debug("Total rollbacks so far: " + rollbacks);

        if (rollbacks % 1000 == 0) {
            logRollbackStatus(gridCell, rollbacks);
        }

        // Hard reset all neighbors except the starting module
        Map<Direction, GridCell> neighbors = gridCell.getOrientedNeighbors();
        Logger.debug("Processing " + neighbors.size() + " neighbors for rollback");

        spatialGrid.rollbackCell(gridCell);
    }

    private void logRollbackStatus(GridCell gridCell, int rollbacks) {
        Vector3i location = gridCell.getCellLocation();
        String message = String.format("Current rollback status: %d chunks rolled back. Latest rollback location: %d, %d, %d",
                rollbacks, location.x, location.y, location.z);
        Logger.warn(message);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    private void done() {
        Logger.debug("Done with generation");
        modularGenerationStatus.startPreparingPlacement();
        isGenerating = false;
        instantPaste();
        spatialGrid.clearGridGenerationData();
    }

    private void cleanup() {
        spatialGrid.clearAllData();
        waveFunctionCollapseGenerators.remove(this);
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
    }

    private void instantPaste() {
        // This guarantees that the paste order is grouped by chunk, making pasting faster down the line.
        Deque<GridCell> orderedPasteDeque = new ArrayDeque<>();
        for (int x = -spatialGrid.getGridRadius(); x < spatialGrid.getGridRadius(); x++) {
            for (int z = -spatialGrid.getGridRadius(); z < spatialGrid.getGridRadius(); z++) {
                for (int y = spatialGrid.getMinYLevel(); y <= spatialGrid.getMaxYLevel(); y++) {
                    // Remove the cell from the map and get it in one go.
                    GridCell cell = spatialGrid.getCellMap().remove(new Vector3i(x, y, z));
                    if (cell != null) {
                        orderedPasteDeque.add(cell);
                    }
                }
            }
        }

        new ModulePasting(world, worldFolder, orderedPasteDeque, modularGenerationStatus, moduleGeneratorsConfigFields.getSpawnPoolSuffix(), startLocation);

        cleanup();
    }

    /**
     * Statistics tracking for generation progress.
     */
    public static class GenerationStats {
        @Getter
        private final int totalChunks;
        private final AtomicInteger generatedChunks = new AtomicInteger(0);
        @Getter
        private final AtomicInteger massPasteCount = new AtomicInteger(0);
        @Getter
        private final AtomicInteger rollbackCounter = new AtomicInteger(0);

        public GenerationStats(int radius, SpatialGrid spatialGrid) {
            int xzRange = 2 * radius + 1;
            int yRange = spatialGrid.getMaxYLevel() - spatialGrid.getMinYLevel() + 1;
            this.totalChunks = xzRange * xzRange * yRange;
        }

        public int getGeneratedChunks() {
            return generatedChunks.get();
        }

        public void incrementGeneratedChunks() {
            generatedChunks.incrementAndGet();
        }

        public void decrementGeneratedChunks(int count) {
            generatedChunks.addAndGet(-count);
        }

        public double getProgress() {
            return (double) generatedChunks.get() / totalChunks;
        }
    }

    private class InitializeGenerationTask extends BukkitRunnable {
        @Override
        public void run() {
            start(startingModule);
        }
    }
}