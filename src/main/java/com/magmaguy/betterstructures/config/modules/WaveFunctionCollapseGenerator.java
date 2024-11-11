package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.modules.*;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.Round;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

public class WaveFunctionCollapseGenerator {
    private static final int DEFAULT_CHUNK_SIZE = 128;

    @Getter private final GenerationConfig config;
    @Getter private final GenerationStats stats;
    @Getter private final SpatialGrid spatialGrid;
    private final Map<GridCell, Integer> chunkRollbackCounter;
    @Getter private final Messaging messaging;

    @Getter private World world;
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;

    /**
     * Configuration class for the generator.
     */
    public static class GenerationConfig {
        @Getter private final String worldName;
        @Getter private final int radius;
        @Getter private final int chunkSize;
        @Getter private final boolean debug;
        @Getter private final int interval;
        @Getter private final boolean slowGeneration;
        @Getter private final String startingModule;
        @Getter private final Player player;
        @Getter private final int massPasteSize;

        private GenerationConfig(Builder builder) {
            this.worldName = builder.worldName;
            this.radius = builder.radius;
            this.chunkSize = builder.chunkSize;
            this.debug = builder.debug;
            this.interval = builder.interval;
            this.slowGeneration = builder.slowGeneration;
            this.startingModule = builder.startingModule;
            this.player = builder.player;
            this.massPasteSize = builder.massPasteSize;
        }

        /**
         * Builder for GenerationConfig.
         */
        public static class Builder {
            private final String worldName;
            private final int radius;
            private int chunkSize = DEFAULT_CHUNK_SIZE;
            private boolean debug = false;
            private int interval = 1;
            private boolean slowGeneration = false;
            private String startingModule;
            private Player player;
            private int massPasteSize = DefaultConfig.getModularChunkPastingSpeed();

            public Builder(String worldName, int radius) {
                this.worldName = worldName;
                this.radius = radius;
            }

            public Builder chunkSize(int chunkSize) {
                this.chunkSize = chunkSize;
                return this;
            }

            public Builder debug(boolean debug) {
                this.debug = debug;
                return this;
            }

            public Builder interval(int interval) {
                this.interval = interval;
                return this;
            }

            public Builder slowGeneration(boolean slowGeneration) {
                this.slowGeneration = slowGeneration;
                return this;
            }

            public Builder startingModule(String startingModule) {
                this.startingModule = startingModule;
                return this;
            }

            public Builder player(Player player) {
                this.player = player;
                return this;
            }

            public Builder massPasteSize(int massPasteSize) {
                this.massPasteSize = massPasteSize;
                return this;
            }

            public GenerationConfig build() {
                validate();
                return new GenerationConfig(this);
            }

            private void validate() {
                if (worldName == null || worldName.isEmpty()) {
                    throw new IllegalArgumentException("World name must be specified");
                }
                if (radius <= 0) {
                    throw new IllegalArgumentException("Radius must be positive");
                }
                if (chunkSize <= 0) {
                    throw new IllegalArgumentException("Chunk size must be positive");
                }
                if (interval <= 0) {
                    throw new IllegalArgumentException("Interval must be positive");
                }
                if (massPasteSize <= 0) {
                    throw new IllegalArgumentException("Mass paste size must be positive");
                }
            }
        }
    }

    /**
     * Statistics tracking for generation progress.
     */
    public static class GenerationStats {
        @Getter private final int totalChunks;
        private final AtomicInteger generatedChunks = new AtomicInteger(0);
        @Getter private final AtomicInteger massPasteCount = new AtomicInteger(0);
        @Getter private final AtomicInteger rollbackCounter = new AtomicInteger(0);

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
    }

    /**
     * Creates a new WaveFunctionCollapseGenerator with fast generation.
     */
    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, Player player, String startingModule) {
        this(new GenerationConfig.Builder(worldName, radius)
                .debug(debug)
                .player(player)
                .startingModule(startingModule)
                .build());
    }

    /**
     * Main constructor using GenerationConfig.
     */
    public WaveFunctionCollapseGenerator(GenerationConfig config) {
        this.config = config;
        this.spatialGrid = new SpatialGrid(config.radius, config.chunkSize);
        this.stats = new GenerationStats(config.radius, getSpatialGrid());
        this.chunkRollbackCounter = new HashMap<>();
        this.messaging = new Messaging(config.player);

        initialize();
    }

    private void initialize() {
        this.world = WorldInitializer.generateWorld(config.worldName, config.player);
        this.messaging.updateProgress(0, "Initializing...");

        new BukkitRunnable() {
            @Override
            public void run() {
                start(config.startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        Messaging.shutdown();
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        try {
            GridCell startChunk = createStartChunk(startingModule);
            if (startChunk == null) {
                return;
            }

            if (config.slowGeneration) {
                generateSlowly();
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
            Logger.sendMessage(config.player, "Starting module was null! Canceling!");
            return null;
        }

        paste(new Vector3i(), modulesContainer);
        return startChunk;
    }

    private void generateSlowly() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isCancelled) {
                    this.cancel();
                    return;
                }

                GridCell nextCell = spatialGrid.getNextGridCell();
                if (nextCell == null) {
                    done();
                    this.cancel();
                    return;
                }

                generateNextChunk(nextCell);
                updateProgress();
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 0L, config.interval);
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
        Logger.debug("[" + formattedProgress + "%] Generated " + stats.getGeneratedChunks() +
                " out of " + stats.totalChunks + " chunks");
        messaging.updateProgress(progress / 100.0, "Assembling modules - " + formattedProgress + "% done...");
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

        if (config.slowGeneration) {
            scheduleActualPaste(gridCell);
        }
    }

    private void updateNeighbors(GridCell gridCell) {
        gridCell.getOrientedNeighbors().values().stream()
                .filter(neighbor -> neighbor != null && !neighbor.isGenerated())
                .forEach(spatialGrid::updateCellPriority);
    }

    private void scheduleActualPaste(GridCell gridCell) {
        new BukkitRunnable() {
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

        Module.paste(modulesContainer.getClipboard(),
                gridCell.getRealLocation().add(-1, 0, -1),
                modulesContainer.getRotation());

        if (config.debug) {
            gridCell.showDebugTextDisplays();
        }
    }

    private void actualBatchPaste(List<GridCell> batchedChunkData) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Module.batchPaste(batchedChunkData, world);
                if (config.debug) {
                    batchedChunkData.forEach(chunkData -> {
                        if (chunkData != null) {
                            chunkData.showDebugTextDisplays();
                        }
                    });
                }
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    private void generateNextChunk(GridCell gridCell) {
        gridCell.updateValidOptions();

        List<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            rollbackChunk(gridCell);
            return;
        }

        ModulesContainer modulesContainer = ModulesContainer.pickRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
            Logger.debug("Failed to pick a module for cell at " + gridCell.getCellLocation());
            rollbackChunk(gridCell);
            return;
        }

        spatialGrid.initializeCellNeighbors(gridCell, this);

        paste(gridCell.getCellLocation(), modulesContainer);
    }

    private void rollbackChunk(GridCell gridCell) {
        int rollbacks = stats.rollbackCounter.incrementAndGet();
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
        if (config.player != null) {
            config.player.sendMessage(message);
        }
    }

    private void performHardReset(GridCell gridCell) {
        int currentCount = chunkRollbackCounter.merge(gridCell, 1, Integer::sum);
//        int rollBackRadius = Math.min(currentCount / 10, 2);
        int rollBackRadius = 1;

        int cellsReset = gridCell.hardReset(spatialGrid, config.slowGeneration, rollBackRadius);
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
        isGenerating = false;

        if (config.player != null) {
            config.player.sendTitle("Done!", "Module assembly complete!");
        }

        Logger.warn("Done with infinity generator!");

        if (!config.slowGeneration) {
            messaging.timeMessage("Module assembly ", config.player);
            Logger.warn("Starting mass paste");
            if (config.player != null) {
                config.player.sendMessage("Starting mass paste...");
            }
            spatialGrid.clearGridGenerationData();
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, this::instantPaste);
        } else {
            messaging.timeMessage("Generation", config.player);
            messaging.clearBar();
            cleanup();
        }
    }

    private void cleanup() {
        reportUnusedModules();
        spatialGrid.clearAllData();
        messaging.clearBar();
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
            Logger.sendMessage(config.player, "Failed to use the following modules:");
            Logger.warn("Failed to use the following modules:");
            unusedModules.forEach(module -> {
                Logger.sendMessage(config.player, module);
                Logger.warn(module);
            });
        }
    }

    private void instantPaste() {
        new BukkitRunnable() {
            private final SpiralIterator iterator = new SpiralIterator(spatialGrid.getGridRadius());

            @Override
            public void run() {
                List<GridCell> batchedChunks = new ArrayList<>();
                int batchCount = 0;

                while (iterator.hasNext() && batchCount < config.massPasteSize) {
                    Vector3i pos = iterator.next();

                    if (isWithinBounds(pos)) {
                        processCellColumn(pos, batchedChunks);
                        batchCount++;
                    }
                }

                updatePasteProgress();
                actualBatchPaste(batchedChunks);

                if (!iterator.hasNext()) {
                    finishPasting();
                    this.cancel();
                }
            }

            private boolean isWithinBounds(Vector3i pos) {
                return Math.abs(pos.x) <= spatialGrid.getGridRadius() &&
                        Math.abs(pos.z) <= spatialGrid.getGridRadius();
            }

            private void processCellColumn(Vector3i pos, List<GridCell> batchedChunks) {
                for (int y = spatialGrid.getMinYLevel(); y <= spatialGrid.getMaxYLevel(); y++) {
                    GridCell cell = spatialGrid.getCellMap().get(new Vector3i(pos.x, y, pos.z));
                    if (cell != null) {
                        batchedChunks.add(cell);
                        stats.massPasteCount.incrementAndGet();
                    }
                }
            }

            private void updatePasteProgress() {
                if (stats.massPasteCount.get() % 1000 == 0) {
                    double progress = (stats.massPasteCount.get() / (double) stats.totalChunks) * 100;
                    String formattedProgress = Round.twoDecimalPlaces(progress) + "";
                    Logger.info("[" + formattedProgress + "%] Pasting chunk " +
                            stats.massPasteCount + "/" + stats.totalChunks);
                    messaging.updateProgress(progress / 100.0,
                            "Pasting world - " + formattedProgress + "% done...");
                }
            }

            private void finishPasting() {
                messaging.timeMessage("Mass paste", config.player);
                cleanup();
            }
        }.runTaskTimerAsynchronously(MetadataHandler.PLUGIN, 0, 1);
    }

    /**
     * Helper class for spiral iteration through the grid.
     */
    private static class SpiralIterator {
        private final int maxRadius;
        private int x = 0, z = 0;
        private int dx = 0, dz = -1;
        private final int maxIterations;
        private int iteration = 0;

        public SpiralIterator(int radius) {
            this.maxRadius = radius;
            this.maxIterations = (2 * radius + 1) * (2 * radius + 1);
        }

        public boolean hasNext() {
            return iteration < maxIterations;
        }

        public Vector3i next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Vector3i current = new Vector3i(x, 0, z);

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                // Turn the spiral
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;
            iteration++;

            return current;
        }
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
    }

    /**
     * @return true if generation is currently in progress
     */
    public boolean isGenerating() {
        return isGenerating;
    }
}