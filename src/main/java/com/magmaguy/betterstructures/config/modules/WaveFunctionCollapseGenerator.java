package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.modules.*;
import com.magmaguy.betterstructures.util.distributedload.WorkloadRunnable;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.Round;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class WaveFunctionCollapseGenerator {
    public static HashSet<WaveFunctionCollapseGenerator> waveFunctionCollapseGenerators = new HashSet<>();
    @Getter
    private final GenerationConfig config;
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
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;
    private int chunksReserved;
    private File worldFolder;

    /**
     * Creates a new WaveFunctionCollapseGenerator with slow generation.
     */
    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, int interval, Player player, String startingModule) {
        this(new GenerationConfig.Builder(worldName, radius)
                .debug(debug)
                .interval(interval)
                .slowGeneration(true)
                .player(player)
                .startingModule(startingModule)
                .build());
        waveFunctionCollapseGenerators.add(this);
    }

    /**
     * Creates a new WaveFunctionCollapseGenerator with fast generation.
     */
    public WaveFunctionCollapseGenerator(String worldName,
                                         int radius,
                                         boolean edgeModules,
                                         boolean debug,
                                         Player player,
                                         String startingModule) {
        this(new GenerationConfig.Builder(worldName, radius)
                .edgeModules(edgeModules)
                .debug(debug)
                .player(player)
                .startingModule(startingModule)
                .build());
    }

    public WaveFunctionCollapseGenerator(
            ModuleGeneratorsConfigFields generatorsConfigFields,
            Player player,
            String worldName,
            File worldFolder) {
        // Use the unique world name
        this(new GenerationConfig.Builder(worldName, generatorsConfigFields.getRadius())
                .edgeModules(generatorsConfigFields.isEdges())
                .debug(generatorsConfigFields.isDebug())
                .player(player)
                .startingModules(generatorsConfigFields.getStartModules())
                .spawnPoolSuffix(generatorsConfigFields.getSpawnPoolSuffix())
                .build());
        this.worldFolder = worldFolder;
    }

    /**
     * Main constructor using GenerationConfig.
     */
    public WaveFunctionCollapseGenerator(GenerationConfig config) {
        this.config = config;
        this.spatialGrid = new SpatialGrid(config.getRadius(), config.getChunkSize());
        this.stats = new GenerationStats(config.getRadius(), getSpatialGrid());
        this.chunkRollbackCounter = new HashMap<>();
        this.modularGenerationStatus = new ModularGenerationStatus(config.getPlayer());
        modularGenerationStatus.startInitializing();

        reserveChunks();
    }

    public static CompletableFuture<ModularWorld> generateFromConfigAsync(ModuleGeneratorsConfigFields configFields, Player player) {
        CompletableFuture<ModularWorld> future = new CompletableFuture<>();

        // Register an event listener for when the world generation is finished.
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onWorldGenerationFinish(WorldGenerationFinishEvent event) {
                // Optionally, check that this event corresponds to the world you generated
                ModularWorld world = event.getModularWorld();
                if (world != null) {
                    future.complete(world);
                    // Optionally unregister this listener
                    HandlerList.unregisterAll(this);
                }
            }
        }, MetadataHandler.PLUGIN);

        // Start generation. This call will eventually lead to a WorldGenerationFinishEvent.
        WaveFunctionCollapseGenerator.generateFromConfig(configFields, player);

        return future;
    }

    public static CompletableFuture<ModularWorld> generateFromConfigAsync(String configFieldsString, Player player) {
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = ModuleGeneratorsConfig.getConfigFields(configFieldsString);
        if (moduleGeneratorsConfigFields == null) return null;
        return generateFromConfigAsync(moduleGeneratorsConfigFields, player);
    }

    public static void generateFromConfig(ModuleGeneratorsConfigFields generatorsConfigFields, Player player) {
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

        new WaveFunctionCollapseGenerator(generatorsConfigFields, player, worldName, worldFolder);
    }

    public static void shutdown() {
        ModularGenerationStatus.shutdown();
        waveFunctionCollapseGenerators.forEach(WaveFunctionCollapseGenerator::cancel);
        waveFunctionCollapseGenerators.clear();
    }

    private void reserveChunks() {
        modularGenerationStatus.finishedInitializing();
        modularGenerationStatus.startReservingChunks();
        this.world = WorldInitializer.generateWorld(config.getWorldName(), config.getPlayer());
        if (config.isSlowGeneration()) config.getPlayer().teleport(world.getSpawnLocation());

        WorkloadRunnable reserveChunksTask = new WorkloadRunnable(.2, () -> {
            modularGenerationStatus.finishedReservingChunks(chunksReserved);
        });
        int realChunkRadius = (int) Math.ceil(spatialGrid.getChunkSize() / 16d) * spatialGrid.getGridRadius();
        int chunkCounter = realChunkRadius * realChunkRadius;
        for (int x = -realChunkRadius; x < realChunkRadius; x++) {
            for (int z = -realChunkRadius; z < realChunkRadius; z++) {
                int finalX = x;
                int finalZ = z;
                reserveChunksTask.addWorkload(() -> {
                    world.loadChunk(finalX, finalZ);
                    chunksReserved++;
                    modularGenerationStatus.updateProgressReservingChunks( chunksReserved / (double) chunkCounter);
                });
            }
        }
        reserveChunksTask.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        startArrangingModules();
    }

    private void startArrangingModules() {
        modularGenerationStatus.startArrangingModules();
        new InitializeGenerationTask().runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        try {
            modularGenerationStatus.finishedArrangingModules();

            GridCell startChunk = createStartChunk(startingModule);
            if (startChunk == null) {
                return;
            }

            if (config.isSlowGeneration()) {
                new GenerateSlowlyTask().runTaskTimer(MetadataHandler.PLUGIN, 0L, config.getInterval());
            } else {
                generateFast();
            }
        } catch (Exception e) {
            Logger.warn("Error during generation: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    private GridCell createStartChunk(String startingModule) {
        GridCell startChunk = new GridCell(new Vector3i(), world, spatialGrid, spatialGrid.getCellMap(), this);
        spatialGrid.getCellMap().put(new Vector3i(), startChunk);
        spatialGrid.initializeCellNeighbors(startChunk, this);

        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.sendMessage(config.getPlayer(), "Starting module was null! Canceling!");
            return null;
        }

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

        if (config.isSlowGeneration()) {
            scheduleActualPaste(gridCell);
        }
    }

    private void updateNeighbors(GridCell gridCell) {
        gridCell.getOrientedNeighbors().values().stream()
                .filter(neighbor -> neighbor != null && !neighbor.isGenerated())
                .forEach(spatialGrid::updateCellPriority);
    }

    private void scheduleActualPaste(GridCell gridCell) {
        new ActualPasteTask() {
            @Override
            public void run() {
                actualPaste(gridCell);
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    private void actualPaste(GridCell gridCell) {
        ModulesContainer modulesContainer = gridCell.getModulesContainer();
        if (modulesContainer == null || modulesContainer.getClipboard() == null) {
            return;
        }

        ModulePasting.paste(modulesContainer.getClipboard(),
                gridCell.getRealLocation().add(0, 0, 0),
                modulesContainer.getRotation());

        if (config.isDebug()) {
            gridCell.showDebugTextDisplays();
        }
    }

    private void generateNextChunk(GridCell gridCell) {
        gridCell.updateValidOptions();

        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
//            Logger.debug("No valid options for cell at " + gridCell.getCellLocation() + " CANCELLING");
            cancel();
            rollbackChunk(gridCell);
            return;
        }

        ModulesContainer modulesContainer = ModulesContainer.pickRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
//            Logger.debug("Failed to pick a module for cell at " + gridCell.getCellLocation());
            rollbackChunk(gridCell);
            return;
        }

//        Logger.debug("picked module " + modulesContainer.getClipboardFilename() + " for coords " + gridCell.getCellLocation());

        if (!modulesContainer.isNothing())
            spatialGrid.initializeCellNeighbors(gridCell, this);

        paste(gridCell.getCellLocation(), modulesContainer);
    }

    private void rollbackChunk(GridCell gridCell) {
        int rollbacks = stats.rollbackCounter.incrementAndGet();
//        Logger.debug("Rolling back " + gridCell.getCellLocation());
        if (rollbacks % 1000 == 0) {
            logRollbackStatus(gridCell, rollbacks);
        }

        boolean resolved = tryAdjustingNeighbors(gridCell);
        if (!resolved) {
            performHardReset(gridCell);
        }
    }

    private void logRollbackStatus(GridCell gridCell, int rollbacks) {
        Vector3i location = gridCell.getCellLocation();
        String message = String.format("Current rollback status: %d chunks rolled back. Latest rollback location: %d, %d, %d",
                rollbacks, location.x, location.y, location.z);
        Logger.warn(message);
        if (config.getPlayer() != null) {
            config.getPlayer().sendMessage(message);
        }
    }

    private void performHardReset(GridCell gridCell) {
        int currentCount = chunkRollbackCounter.merge(gridCell, 1, Integer::sum);
//        int rollBackRadius = Math.min(currentCount / 10, 2);
        int rollBackRadius = 1;

        int cellsReset = gridCell.hardReset(spatialGrid, config.isSlowGeneration(), rollBackRadius);
        stats.decrementGeneratedChunks(cellsReset);
    }

    private boolean tryAdjustingNeighbors(GridCell gridCell) {
        Map<Direction, GridCell> neighbors = gridCell.getOrientedNeighbors();
        Map<GridCell, ModulesContainer> originalModules = new HashMap<>();
        List<GridCell> generatedNeighbors = new ArrayList<>();

        collectGeneratedNeighbors(neighbors, originalModules, generatedNeighbors);

        boolean resolved = tryNeighborConfigurations(generatedNeighbors, gridCell, originalModules);
        if (!resolved) {
            revertNeighbors(originalModules);
        }

        return resolved;
    }

    private void collectGeneratedNeighbors(Map<Direction, GridCell> neighbors,
                                           Map<GridCell, ModulesContainer> originalModules,
                                           List<GridCell> generatedNeighbors) {
        neighbors.values().stream()
                .filter(neighbor -> neighbor != null && neighbor.isGenerated())
                .forEach(neighbor -> {
                    generatedNeighbors.add(neighbor);
                    originalModules.put(neighbor, neighbor.getModulesContainer());
                });
    }

    private boolean tryNeighborConfigurations(List<GridCell> neighbors, GridCell failedCell,
                                              Map<GridCell, ModulesContainer> originalModules) {
        if (neighbors.isEmpty()) {
            return false;
        }

        List<List<ModulesContainer>> neighborOptions = new ArrayList<>();
        List<GridCell> adjustableNeighbors = new ArrayList<>();

        // Collect valid options for each neighbor
        for (GridCell neighbor : neighbors) {
            neighbor.updateValidOptions();
            List<ModulesContainer> options = new ArrayList<>(neighbor.getValidOptions());

            if (!options.isEmpty()) {
                neighborOptions.add(options);
                adjustableNeighbors.add(neighbor);
            }
        }

        if (adjustableNeighbors.isEmpty()) {
            return false;
        }

        // Try different combinations of neighbor modules
        return tryAllCombinations(adjustableNeighbors, neighborOptions, failedCell);
    }

    private boolean tryAllCombinations(List<GridCell> neighbors,
                                       List<List<ModulesContainer>> neighborOptions,
                                       GridCell failedCell) {
        int[] indices = new int[neighborOptions.size()];

        do {
            // Apply current combination
            for (int i = 0; i < neighbors.size(); i++) {
                GridCell neighbor = neighbors.get(i);
                ModulesContainer module = neighborOptions.get(i).get(indices[i]);
                neighbor.setModulesContainer(module);
            }

            // Check if this combination works
            failedCell.updateValidOptions();
            if (failedCell.getValidOptionCount() > 0) {
                return true;
            }

        } while (incrementIndices(indices, neighborOptions));

        return false;
    }

    private boolean incrementIndices(int[] indices, List<List<ModulesContainer>> options) {
        for (int i = 0; i < indices.length; i++) {
            indices[i]++;
            if (indices[i] < options.get(i).size()) {
                return true;
            }
            indices[i] = 0;
        }
        return false;
    }

    private void revertNeighbors(Map<GridCell, ModulesContainer> originalModules) {
        originalModules.forEach((cell, module) -> {
            cell.setModulesContainer(module);
            cell.updateValidOptions();
        });
    }

    private void done() {
        modularGenerationStatus.startPreparingPlacement();
        isGenerating = false;

        if (!config.isSlowGeneration()) {
            Logger.warn("Starting mass paste");
            spatialGrid.clearGridGenerationData();
            instantPaste();
        } else {
            cleanup();
        }
    }

    private void cleanup() {
//        if (!getConfig().isSlowGeneration()) teleportToSpawn(config.getPlayer());

//        reportUnusedModules(); this is just for debugging
        spatialGrid.clearAllData();
        waveFunctionCollapseGenerators.remove(this);
    }

    private void reportUnusedModules() {
        Set<String> usedModules = new HashSet<>();

        // Collect all used modules
        spatialGrid.getCellMap().values().stream()
                .filter(cell -> cell.getModulesContainer() != null)
                .forEach(cell -> usedModules.add(cell.getModulesContainer().getClipboardFilename()));

        // Report unused modules
        List<String> unusedModules = ModulesContainer.getModulesContainers().values().stream()
                .map(ModulesContainer::getClipboardFilename)
                .filter(filename -> !usedModules.contains(filename))
                .toList();

        if (!unusedModules.isEmpty()) {
            Logger.sendMessage(config.getPlayer(), "Failed to use the following modules:");
            Logger.warn("Failed to use the following modules:");
            unusedModules.forEach(module -> {
                Logger.sendMessage(config.getPlayer(), module);
                Logger.warn(module);
            });
        }
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

        if (config.isDebug()) {
            orderedPasteDeque.forEach(chunkData -> {
                if (chunkData != null) {
                    chunkData.showDebugTextDisplays();
                }
            });
        }

        new ModulePasting(world, worldFolder, orderedPasteDeque, modularGenerationStatus, config.getSpawnPoolSuffix());

        cleanup();
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
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

    private static class ActualPasteTask extends BukkitRunnable {
        @Override
        public void run() {
        }
    }

    private class InitializeGenerationTask extends BukkitRunnable {
        @Override
        public void run() {
            start(config.getStartingModule());
        }
    }

    private class GenerateSlowlyTask extends BukkitRunnable {
        @Override
        public void run() {
            if (isCancelled) {
                this.cancel();
                return;
            }

            GridCell nextCell = spatialGrid.getNextGridCell();
            if (nextCell == null) {
                modularGenerationStatus.finishedArrangingModules();
                done();
                this.cancel();
                return;
            }

            generateNextChunk(nextCell);
            updateProgress();
        }
    }
}