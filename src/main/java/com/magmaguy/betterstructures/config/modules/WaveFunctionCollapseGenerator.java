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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WaveFunctionCollapseGenerator {
    private final int massPasteSize = DefaultConfig.getModularChunkPastingSpeed();
    private final HashMap<GridCell, Integer> chunkRollbackCounter = new HashMap<>();
    public Messaging messaging;
    int rollbackCounter = 0;
    private Player player;
    private int totalMassPasteChunks;
    private boolean debug;
    private SpatialGrid spatialGrid;
    private int interval;
    private boolean slowGenerationForShowcase = false;
    private World world;
    private int massPasteCount = 0;
    private int totalChunks;
    private int generatedChunks = 0;

    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, int interval, Player player, String startingModule) {
        initialize(radius, worldName, player, debug);
        this.interval = interval;
        slowGenerationForShowcase = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                start(startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, Player player, String startingModule) {
        initialize(radius, worldName, player, debug);

        new BukkitRunnable() {
            @Override
            public void run() {
                start(startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        Messaging.shutdown();
    }

    private void initialize(int radius, String worldName, Player player, boolean debug) {
        spatialGrid = new SpatialGrid(radius);
        messaging = new Messaging(player);

        this.player = player;
        this.debug = debug;
        world = WorldInitializer.generateWorld(worldName, player);
        messaging.updateProgress(0, "Initializing...");

        // Calculate total chunks for progress tracking
        int xzRange = 2 * radius + 1;
        int yRange = SpatialGrid.MAX_Y_LEVEL - SpatialGrid.MIN_Y_LEVEL + 1;
        totalChunks = xzRange * xzRange * yRange;

        // Initialize totalMassPasteChunks based on radius
        totalMassPasteChunks = xzRange * xzRange;
    }

    private void start(String startingModule) {
        GridCell startChunk = new GridCell(new Vector3i(), world, spatialGrid.getCellMap());
        spatialGrid.getCellMap().put(new Vector3i(), startChunk);
        spatialGrid.initializeCellNeighbors(startChunk);

        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (startingModule == null) {
            Logger.sendMessage(player, "Starting chunk was null! Canceling!");
            Logger.warn("Starting chunk was null! Canceling!");
            return;
        }
        paste(new Vector3i(), modulesContainer);
        // Begin the recursive generation
        searchNextChunkToGenerate();
    }

    private void searchNextChunkToGenerate() {
        if (slowGenerationForShowcase) {
            GridCell selectedChunkLocationKey = spatialGrid.getNextGridCell();
            if (selectedChunkLocationKey == null) {
                done();
            } else {
                generateNextChunk(selectedChunkLocationKey);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        searchNextChunkToGenerate();
                    }
                }.runTaskLater(MetadataHandler.PLUGIN, interval);
            }
        } else {
            while (true) {
                GridCell selectedChunkLocationKey = spatialGrid.getNextGridCell();
                if (selectedChunkLocationKey == null) {
                    done();
                    return;
                }
                generateNextChunk(selectedChunkLocationKey);
            }
        }
    }

    private void updateAssemblingProgress() {
        double progress = Round.twoDecimalPlaces(((double) generatedChunks) / totalChunks * 100);
        Logger.debug("[" + progress + "%] Generated " + generatedChunks + " out of " + totalChunks + " chunks");
        messaging.updateProgress(progress / 100.0, "Assembling modules - " + progress + "% done...");
    }

    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer) {
        // Access the ChunkData from chunkArray
        GridCell gridCell = getChunkDataAt(chunkLocation.x, chunkLocation.y, chunkLocation.z);
        if (gridCell == null) {
            Logger.warn("ChunkData not found at location: " + chunkLocation);
            return;
        }

        // If this cell was not already generated, increment generatedChunks
        if (!gridCell.isGenerated()) {
            generatedChunks++;
        }

        gridCell.processPaste(modulesContainer);
        for (GridCell neighbor : gridCell.getOrientedNeighbors().values()) {
            if (neighbor != null && !neighbor.isGenerated()) {
                spatialGrid.updateCellPriority(neighbor);
            }
        }

        // If a successful paste is done then the rollback counter should at least reset
        chunkRollbackCounter.remove(gridCell);

        if (slowGenerationForShowcase) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualPaste(gridCell);
                }
            }.runTask(MetadataHandler.PLUGIN);
        }

        // Update progress every 100 chunks or as needed
        if (generatedChunks % 100 == 0) {
            updateAssemblingProgress();
        }
    }

    private GridCell getChunkDataAt(int x, int y, int z) {
        return spatialGrid.getCellMap().get(new Vector3i(x, y, z));
    }

    private void actualPaste(GridCell gridCell) {
        ModulesContainer modulesContainer = gridCell.getModulesContainer();
        if (modulesContainer == null || modulesContainer.getClipboard() == null) return;
        Module.paste(modulesContainer.getClipboard(), gridCell.getRealLocation().add(-1, 0, -1), gridCell.getModulesContainer().getRotation());
        if (debug) gridCell.showDebugTextDisplays();
    }

    private void actualBatchPaste(List<GridCell> batchedChunkData) {
        Module.batchPaste(batchedChunkData, world);
        if (debug) batchedChunkData.forEach(chunkData -> {
            if (chunkData != null) chunkData.showDebugTextDisplays();
        });
    }

    private void generateNextChunk(GridCell gridCell) {
        spatialGrid.initializeCellNeighbors(gridCell);

        // Update valid options
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

        // Paste the module
        paste(gridCell.getCellLocation(), modulesContainer);

        if (slowGenerationForShowcase) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualPaste(gridCell);
                }
            }.runTask(MetadataHandler.PLUGIN);
        }
    }

    private void rollbackChunk(GridCell gridCell) {
        // Logger.debug("Rolling back chunk at " + gridCell.getCellLocation());
        chunkRollbackCounter.put(gridCell, chunkRollbackCounter.getOrDefault(gridCell, 0) + 1);
        int rollBackRadius = Math.max((int) (chunkRollbackCounter.get(gridCell) / 30d), 1);
        rollbackCounter++;
        if (rollbackCounter % 1000 == 0) {
            Vector3i location = gridCell.getCellLocation();
            Logger.warn("Current rollback status: " + rollbackCounter + " chunks rolled back. Latest rollback location: " + location.x + ", " + location.y + ", " + location.z);
            player.sendMessage("Current rollback status: " + rollbackCounter + " chunks rolled back. Latest rollback location: " + location.x + ", " + location.y + ", " + location.z);
        }
        int cellsReset = gridCell.hardReset(spatialGrid, slowGenerationForShowcase, rollBackRadius);
        generatedChunks -= cellsReset; // Subtract only the number of generated cells that were reset
    }

    private void done() {
        if (player != null) {
            player.sendTitle("Done!", "Module assembly complete!");
        }

        Logger.warn("Done with infinity generator!");

        if (!slowGenerationForShowcase) {
            messaging.timeMessage("Module assembly ", player);
            Logger.warn("Starting mass paste");
            if (player != null) player.sendMessage("Starting mass paste...");
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, this::instantPaste);
        } else {
            messaging.timeMessage("Generation", player);
            messaging.clearBar();
            cleanup();
        }
    }

    private void cleanup() {
        boolean firstNotUsed = false;
        for (ModulesContainer modulesContainer : ModulesContainer.getModulesContainers().values()) {
            boolean used = false;
            for (GridCell gridCell : spatialGrid.getCellMap().values()) {
                if (gridCell.getModulesContainer() != null &&
                        modulesContainer.getClipboardFilename().equals(gridCell.getModulesContainer().getClipboardFilename())) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                if (!firstNotUsed) {
                    firstNotUsed = true;
                    Logger.sendMessage(player, "Failed to use the following modules:");
                    Logger.warn("Failed to use the following modules:");
                }
                Logger.sendMessage(player, modulesContainer.getClipboardFilename());
                Logger.warn(modulesContainer.getClipboardFilename());
            }
        }
        spatialGrid.clearAllData();
        messaging.clearBar();
    }

    private void instantPaste() {
        BukkitRunnable pasteTask = new BukkitRunnable() {
            // Initialize spiral variables
            int x = 0, z = 0;
            int dx = 0, dz = -1;
            final int max = (2 * spatialGrid.getGridRadius() + 1) * (2 * spatialGrid.getGridRadius() + 1);
            int i = 0;

            @Override
            public void run() {
                int batchCount = 0;

                List<GridCell> batchedChunks = new ArrayList<>();

                while (i < max && batchCount < massPasteSize) {
                    if (-spatialGrid.getGridRadius() <= x && x <= spatialGrid.getGridRadius() && -spatialGrid.getGridRadius() <= z && z <= spatialGrid.getGridRadius()) {
                        // Process position (x, z)
                        for (int y = SpatialGrid.MIN_Y_LEVEL; y <= SpatialGrid.MAX_Y_LEVEL; y++) {
                            GridCell gridCell = getChunkDataAt(x, y, z);
                            if (gridCell != null) {
                                batchedChunks.add(gridCell);
                                massPasteCount++;
                            }
                        }
                        batchCount++;
                    }

                    if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                        int temp = dx;
                        dx = -dz;
                        dz = temp;
                    }
                    x += dx;
                    z += dz;
                    i++;
                }

                // Update progress after each batch
                if (massPasteCount % 1000 == 0) {
                    double progress = Round.twoDecimalPlaces((massPasteCount / (double) totalMassPasteChunks) * 100);
                    Logger.info("[" + progress + "%] Pasting chunk " + massPasteCount + "/" + totalMassPasteChunks);
                    messaging.updateProgress(progress / 100.0, "Pasting world - " + progress + "% done...");
                }

                actualBatchPaste(batchedChunks);

                if (i >= max) {
                    messaging.timeMessage("Mass paste", player);
                    this.cancel();
                    cleanup();
                }
            }
        };

        // Schedule the task to run every tick until it's canceled
        pasteTask.runTaskTimerAsynchronously(MetadataHandler.PLUGIN, 0, 1);
    }

}
