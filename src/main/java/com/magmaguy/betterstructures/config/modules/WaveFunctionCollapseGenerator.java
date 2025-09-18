package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.modules.GridCell;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.modules.SpatialGrid;
import com.magmaguy.betterstructures.modules.WorldInitializer;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import static com.magmaguy.betterstructures.modules.ModulesContainer.pickWeightedRandomModule;

public class WaveFunctionCollapseGenerator {
    public static HashSet<WaveFunctionCollapseGenerator> waveFunctionCollapseGenerators = new HashSet<>();
    @Getter
    private ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;

    @Getter
    private SpatialGrid spatialGrid;
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

    public WaveFunctionCollapseGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Player player) {
        this.player = player;
        this.startLocation = player.getLocation();
        initialize(moduleGeneratorsConfigFields);
    }

    public WaveFunctionCollapseGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Location startLocation) {
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
                    new WaveFunctionCollapseGenerator(generatorsConfigFields, player);
                }
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        waveFunctionCollapseGenerators.forEach(WaveFunctionCollapseGenerator::cancel);
        waveFunctionCollapseGenerators.clear();
    }

    private void initialize(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;
        this.spatialGrid = new SpatialGrid(moduleGeneratorsConfigFields.getRadius(), moduleGeneratorsConfigFields.getModuleSizeXZ(), moduleGeneratorsConfigFields.getModuleSizeY(), moduleGeneratorsConfigFields.getMinChunkY(), moduleGeneratorsConfigFields.getMaxChunkY());
        waveFunctionCollapseGenerators.add(this);

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

    private void reserveChunks() {
        Logger.debug("Starting chunk reservation");
        if (moduleGeneratorsConfigFields.isWorldGeneration()) {
            this.world = WorldInitializer.generateWorld(worldName, player);
        } else {
            this.world = startLocation.getWorld();
        }

        spatialGrid.initializeGrid(world, this);

//        WorkloadRunnable reserveChunksTask = new WorkloadRunnable(.2, () -> {});
//        int realChunkRadius = (int) Math.ceil(spatialGrid.getChunkSizeXZ() / 16d) * spatialGrid.getGridRadius();
//        for (int x = -realChunkRadius; x < realChunkRadius; x++) {
//            for (int z = -realChunkRadius; z < realChunkRadius; z++) {
//                int finalX = x;
//                int finalZ = z;
//                reserveChunksTask.addWorkload(() -> {
//                    world.loadChunk(finalX, finalZ);
//                });
//            }
//        }
//        reserveChunksTask.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        startArrangingModules();
    }

    private void startArrangingModules() {
        Logger.debug("Starting module arrangement");
        new InitializeGenerationTask().runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private void start(String startingModule) {
        if (isGenerating) {
            return;
        }
        isGenerating = true;

        Logger.debug("Starting generation with starting module " + startingModule);

        try {
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
        GridCell startCell = spatialGrid.getCellMap().get(new Vector3i());

        Logger.debug("Looking up modules container for " + startingModule);
        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.warn("Starting module was null! Cancelling!");
            return null;
        }

        Logger.debug("Pasting starting module at origin");
        paste(startCell, modulesContainer);
        return startCell;
    }

    private void generateFast() {
        while (!isCancelled) {
            GridCell nextCell = spatialGrid.getNextGridCell();
            if (nextCell == null) {
                done();
                break;
            }

            generateNextChunk(nextCell);
        }
    }

    private void paste(GridCell gridCell, ModulesContainer modulesContainer) {
        Logger.debug("Pasting chunk at " + gridCell.getRealCenterLocation() + " with module " + modulesContainer.getClipboardFilename());
        gridCell.setModulesContainer(modulesContainer);
        gridCell.getOrientedNeighbors().values().forEach(spatialGrid::updateCellPriority);
    }

    private void generateNextChunk(GridCell gridCell) {
        Logger.debug("Generating next chunk at location: " + gridCell.getCellLocation());

        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            Logger.debug("No valid options for cell at " + gridCell.getCellLocation() + ", initiating rollback");
            org.bukkit.Location targetLocation = gridCell.getRealCenterLocation();
            if (player != null)
                player.spigot().sendMessage(Logger.commandHoverMessage("Rolling back cell at " + gridCell.getCellLocation(), "Click to teleport", "tp " + targetLocation.getX() + " " + targetLocation.getY() + " " + targetLocation.getZ()));
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

        paste(gridCell, modulesContainer);
    }

    private void rollbackChunk(GridCell gridCell) {
        Logger.debug("Starting rollback for chunk at: " + gridCell.getCellLocation());
        spatialGrid.rollbackCell(gridCell);
        rollbackCounter++;
        if (rollbackCounter > 1000) {
            cancel();
            Logger.debug("Exceeded rollback limit, cancelling generation");
        }
    }

    private void done() {
        Logger.debug("Done with generation");
        if (player != null) {
            player.sendMessage("Done assembling!");
            player.sendMessage("It will take a moment to paste the structure, and will require relogging.");
        }
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

        new ModulePasting(world, worldFolder, orderedPasteDeque, moduleGeneratorsConfigFields.getSpawnPoolSuffix(), startLocation);

        cleanup();
    }

    private class InitializeGenerationTask extends BukkitRunnable {
        @Override
        public void run() {
            start(startingModule);
        }
    }
}