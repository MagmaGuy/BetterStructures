package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.modules.BuildBorder;
import com.magmaguy.betterstructures.modules.ChunkData;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.Round;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.*;

public class WaveFunctionCollapseGenerator {
    @Getter
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    private static final HashSet<BossBar> bars = new HashSet<>();
    private static final Vector3i[] NEIGHBOR_OFFSETS = {
            new Vector3i(0, 1, 0),    // UP
            new Vector3i(0, -1, 0),   // DOWN
            new Vector3i(1, 0, 0),    // EAST
            new Vector3i(-1, 0, 0),   // WEST
            new Vector3i(0, 0, -1),   // NORTH
            new Vector3i(0, 0, 1)     // SOUTH
    };
    private static final BuildBorder[] NEIGHBOR_DIRECTIONS = {
            BuildBorder.UP,
            BuildBorder.DOWN,
            BuildBorder.EAST,
            BuildBorder.WEST,
            BuildBorder.NORTH,
            BuildBorder.SOUTH
    };
    private static final int MIN_Y_LEVEL = -4;
    private static final int MAX_Y_LEVEL = 20;
//    private static final int Y_SIZE = MAX_Y_LEVEL - MIN_Y_LEVEL; // Y_SIZE = 25
    //    private final HashSet<ChunkData> emptyChunks = new HashSet<>();
    private final Player player;
    private final int radius;
    private final int massPasteSize = DefaultConfig.getModularChunkPastingSpeed();
    private final int totalMassPasteChunks;
    private final Set<Vector2i> processedXZ = new HashSet<>();
    private final int averageYLevels = 10; // this is used to estimate the progress for the chunk detection
    private final BossBar completionPercentage;
    private final List<Vector2i> spiralPositions;
    private final boolean debug;
    private final boolean preventInsularModules = true; //todo: not all modes should do this
    private final Map<Vector3i, ChunkData> chunkMap = new HashMap<>();
    private final Queue<ChunkData> chunkQueue = new PriorityQueue<>(
            Comparator.<ChunkData>comparingInt(chunkData -> -chunkData.getGeneratedNeighborCount())
                    .thenComparingLong(chunkData -> {
                        Vector3i loc = chunkData.getChunkLocation();
                        return (long) loc.x * loc.x + (long) loc.y * loc.y + (long) loc.z * loc.z;
                    })
    );

    int rollbackCounter = 0;
    private int interval;
    private boolean slowGenerationForShowcase = false;
    private World world;
    private int processedChunks = 0;
    private int massPasteCount = 0;
    private long startTime;

    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, int interval, Player player, String startingModule) {
        this.player = player;
        this.debug = debug;
        this.interval = interval;
        slowGenerationForShowcase = true;
        this.radius = radius;
        this.spiralPositions = generateSpiralPositions(radius);
        generateWorld(worldName);

        completionPercentage = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        completionPercentage.addPlayer(player);
        bars.add(completionPercentage);
        updateProgress(0, "Initializing...");
        // Initialize total chunks for mass paste progress tracking
        int xzRange = 2 * radius + 1;
        totalMassPasteChunks = xzRange * xzRange;

        new BukkitRunnable() {
            @Override
            public void run() {
                start(startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, Player player, String startingModule) {
        this.player = player;
        this.debug = debug;
        this.radius = radius;
        this.spiralPositions = generateSpiralPositions(radius);
        generateWorld(worldName);

        completionPercentage = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        completionPercentage.addPlayer(player);
        bars.add(completionPercentage);
        updateProgress(0, "Initializing...");
        // Initialize total chunks for mass paste progress tracking
        int xzRange = 2 * radius + 1;
        totalMassPasteChunks = xzRange * xzRange;

        new BukkitRunnable() {
            @Override
            public void run() {
                start(startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        for (BossBar bar : bars) {
            bar.removeAll();
        }
        bars.clear();
    }

    private static boolean isPassable(ChunkData chunkData, BuildBorder border) {
        if (chunkData == null) return false; // Return false if chunkData is null
        ModulesContainer modulesContainer = chunkData.getModulesContainer();
        ModulesConfigFields configField = modulesContainer != null ? modulesContainer.getModulesConfigField() : null;
        return (configField == null) || switch (border) {
            case UP -> configField.isUpIsPassable();
            case DOWN -> configField.isDownIsPassable();
            case NORTH -> configField.isNorthIsPassable();
            case SOUTH -> configField.isSouthIsPassable();
            case EAST -> configField.isEastIsPassable();
            case WEST -> configField.isWestIsPassable();
        };
    }

    private void clearBar() {
        bars.remove(completionPercentage);
        completionPercentage.removeAll();
    }

    private void updateProgress(double progress, String message) {
        completionPercentage.setProgress(progress);
        completionPercentage.setTitle(message);
    }

    // And remove the getDistanceSquared method or keep it if used elsewhere:
    private Long getDistanceSquared(Vector3i location) {
        return (long) location.x * location.x + (long) location.y * location.y + (long) location.z * location.z;
    }


    private void generateWorld(String worldName) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(World.Environment.NORMAL);
        worldCreator.keepSpawnInMemory(false);
        worldCreator.generator(new VoidGenerator());
        world = worldCreator.createWorld();
        world.setAutoSave(false);
        player.teleport(new Location(world, 8, 16, 8));
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void start(String startingModule) {
        startTime = System.currentTimeMillis();

        ChunkData startChunk = new ChunkData(new Vector3i(), world, chunkMap);
        chunkMap.put(new Vector3i(), startChunk);
        initializeChunkDataNeighbours(startChunk);

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
            ChunkData selectedChunkLocationKey = chunkQueue.poll();
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
                ChunkData selectedChunkLocationKey = chunkQueue.poll();
                if (selectedChunkLocationKey == null) {
                    done();
                    return;
                }
                generateNextChunk(selectedChunkLocationKey);
            }
        }
    }

    private void updateAssemblingProgress() {
        int totalEstimatedChunks = ((2 * radius + 1) * (2 * radius + 1)) * averageYLevels;
        double progress = Round.twoDecimalPlaces(((double) processedXZ.size() * averageYLevels) / totalEstimatedChunks * 100);
        Logger.debug("[" + progress + "%] Processed " + processedChunks + " chunks");
        updateProgress(progress / 100L, "Assembling modules - " + progress + "% done...");
    }

    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer) {
        processedChunks++;
        processedXZ.add(new Vector2i(chunkLocation.x, chunkLocation.z));

        if (processedChunks % 1000 == 0) {
            updateAssemblingProgress();
        }

        // Access the ChunkData from chunkArray
        ChunkData chunkData = getChunkDataAt(chunkLocation.x, chunkLocation.y, chunkLocation.z);
        if (chunkData == null) {
            Logger.warn("ChunkData not found at location: " + chunkLocation);
            return;
        }

        chunkData.processPaste(modulesContainer);

        if (slowGenerationForShowcase) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualPaste(chunkData);
                }
            }.runTask(MetadataHandler.PLUGIN);
        }
    }

    private ChunkData getChunkDataAt(int x, int y, int z) {
        return chunkMap.get(new Vector3i(x, y, z));
    }

    private void actualPaste(ChunkData chunkData) {
        ModulesContainer modulesContainer = chunkData.getModulesContainer();
        if (modulesContainer == null || modulesContainer.getClipboard() == null) return;
        Module.paste(modulesContainer.getClipboard(), chunkData.getRealLocation().add(-1, 0, -1), chunkData.getModulesContainer().getRotation());
        if (debug) chunkData.showDebugTextDisplays();
    }

    private void actualBatchPaste(List<ChunkData> batchedChunkData) {
        Module.batchPaste(batchedChunkData, world);
        if (debug) batchedChunkData.forEach(chunkData -> {
            if (chunkData != null) chunkData.showDebugTextDisplays();
        });
    }

    private void initializeChunkDataNeighbours(ChunkData chunkData) {
        for (BuildBorder border : BuildBorder.values()) {
            Vector3i neighborLocation = chunkData.getChunkLocation().add(getOffset(border));

            if (withinBounds(neighborLocation)) {
                ChunkData neighborChunk = chunkMap.get(neighborLocation);

                if (neighborChunk == null) {
                    Logger.debug("Initializing chunkData neighbours for chunk at " + chunkData.getChunkLocation());
                    // Initialize neighbor
                    neighborChunk = new ChunkData(neighborLocation, world, chunkMap);
                    chunkMap.put(neighborLocation, neighborChunk);
                    chunkQueue.add(neighborChunk);
                }
            }
        }
    }

    private Vector3i getOffset(BuildBorder border) {
        return switch (border) {
            case NORTH -> new Vector3i(0, 0, -1);
            case SOUTH -> new Vector3i(0, 0, 1);
            case EAST -> new Vector3i(1, 0, 0);
            case WEST -> new Vector3i(-1, 0, 0);
            case UP -> new Vector3i(0, 1, 0);
            case DOWN -> new Vector3i(0, -1, 0);
        };
    }

    private void generateNextChunk(ChunkData chunkData) {
        initializeChunkDataNeighbours(chunkData);

        // Get valid modules for the current chunk
        ModulesContainer modulesContainer = ModulesContainer.pickRandomModuleFromSurroundings(chunkData);
        if (modulesContainer == null) {
            rollbackChunk(chunkData);
            return;
        }

        // Paste the module
        paste(chunkData.getChunkLocation(), modulesContainer);
//        chunkData.processPaste(modulesContainer);

        if (slowGenerationForShowcase) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualPaste(chunkData);
                }
            }.runTask(MetadataHandler.PLUGIN);
        }
    }

    private boolean withinBounds(Vector3i location) {
        return Math.abs(location.x) <= radius &&
                location.y >= MIN_Y_LEVEL &&
                location.y <= MAX_Y_LEVEL &&
                Math.abs(location.z) <= radius;
    }

    private void rollbackChunk(ChunkData chunkData) {
        rollbackCounter++;
        if (rollbackCounter % 1000 == 0) {
            Vector3i location = chunkData.getChunkLocation();
            Logger.warn("Current rollback status: " + rollbackCounter + " chunks rolled back. Latest rollback location: " + location.x + ", " + location.y + ", " + location.z);
            player.sendMessage("Current rollback status: " + rollbackCounter + " chunks rolled back. Latest rollback location: " + location.x + ", " + location.y + ", " + location.z);
        }
        chunkData.hardReset(slowGenerationForShowcase, chunkQueue);
    }

//    private void removeInsularModules() {
//        if (!preventInsularModules) return;
//
//        ChunkData startingModule = getChunkDataAt(0, 0, 0);
//        if (startingModule == null) {
//            Logger.warn("Starting module is null at (0, 0, 0). Cannot remove insular modules.");
//            return;
//        }
//
//        Queue<ChunkData> uncheckedNeighbors = new LinkedList<>();
//        Set<ChunkData> validatedModules = new HashSet<>();
//        Set<ChunkData> queuedChunks = new HashSet<>();
//
//        uncheckedNeighbors.add(startingModule);
//        queuedChunks.add(startingModule);
//
//        while (!uncheckedNeighbors.isEmpty()) {
//            ChunkData chunkData = uncheckedNeighbors.poll();
//            if (validatedModules.contains(chunkData)) continue;
//
//            for (Map.Entry<BuildBorder, ChunkData> entry : chunkData.getOrientedNeighbours().entrySet()) {
//                BuildBorder border = entry.getKey();
//                ChunkData neighborChunk = entry.getValue();
//
//                if (neighborChunk == null) continue; // Skip if neighborChunk is null
//
//                boolean isPassable = isPassable(chunkData, border);
//                boolean isNeighborPassable = isPassable(neighborChunk, border.getOpposite());
//
//                if (isPassable && isNeighborPassable && !validatedModules.contains(neighborChunk) && !queuedChunks.contains(neighborChunk)) {
//                    uncheckedNeighbors.add(neighborChunk);
//                    queuedChunks.add(neighborChunk);
//                }
//            }
//
//            validatedModules.add(chunkData);
//        }
//
//        for (ChunkData[][] chunkDataArray : chunkArray) {
//            for (ChunkData[] chunkDatum : chunkDataArray) {
//                for (ChunkData data : chunkDatum) {
//                    if (data == null) continue; // Ensure data is not null
//                    if (validatedModules.contains(data)) continue;
//                    data.preventInsularity();
//                }
//            }
//        }
//    }

    private void done() {
//        removeInsularModules();

        if (player != null) {
            player.sendTitle("Done!", "Module assembly complete!");
        }

        Logger.warn("Done with infinity generator!");

        if (!slowGenerationForShowcase) {
            timeMessage("Module assembly ");
            Logger.warn("Starting mass paste");
            if (player != null) player.sendMessage("Starting mass paste...");
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, this::instantPaste);
        } else {
            timeMessage("Generation");
            clearBar();
        }
    }

    private void cleanup() {
        HashSet<ModulesContainer> usedModules = new HashSet<>();
        chunkMap.clear();
        boolean firstNotUsed = false;
        for (ModulesContainer modulesContainer : ModulesContainer.getModulesContainers().values()) {
            boolean used = false;
            for (ModulesContainer usedModule : usedModules) {
                if (modulesContainer.getClipboardFilename().equals(usedModule.getClipboardFilename())) {
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

        spiralPositions.clear();
        processedXZ.clear();
    }

    private void timeMessage(String whatEnded) {
        // Calculate and display the elapsed time
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = elapsedTime / (1000 * 60 * 60);

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        if (player != null) {
            player.sendMessage(whatEnded + " completed! Time taken: " + timeString);
        }
        Logger.warn("Generation complete! Time taken: " + timeString);
    }

    private List<Vector2i> generateSpiralPositions(int radius) {
        List<Vector2i> positions = new ArrayList<>();
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int max = (2 * radius + 1) * (2 * radius + 1);

        for (int i = 0; i < max; i++) {
            if (-radius <= x && x <= radius && -radius <= z && z <= radius) {
                positions.add(new Vector2i(x, z));
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx;
            z += dz;
        }
        return positions;
    }


    private void instantPaste() {
        BukkitRunnable pasteTask = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int batchCount = 0;

                List<ChunkData> batchedChunks = new ArrayList<>();

                while (index < spiralPositions.size() && batchCount < massPasteSize) {
                    massPasteCount++;
                    Vector2i pos = spiralPositions.get(index);
                    int x = pos.x();
                    int z = pos.y();

                    if (massPasteCount % massPasteSize == 0) {
                        double progress = Round.twoDecimalPlaces(massPasteCount / (double) totalMassPasteChunks * 100);
                        Logger.info("[" + progress + "%] Pasting chunk " + massPasteCount + "/" + totalMassPasteChunks + " at " + x + ", " + z);
                        updateProgress(progress / 100L, "Pasting world - " + progress + "% done...");
                    }

                    for (int y = MIN_Y_LEVEL; y < MAX_Y_LEVEL; y++) {
                        ChunkData chunkData = getChunkDataAt(x, y, z);
                        batchedChunks.add(chunkData);
                    }

                    index++;
                    batchCount++;
                }

                actualBatchPaste(batchedChunks);

                if (index >= spiralPositions.size()) {
                    timeMessage("Mass paste");
                    clearBar();
                    this.cancel(); // Stop the task
                    cleanup();
                }
            }
        };

        // Schedule the task to run every tick until it's canceled
        pasteTask.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    private static class VoidGenerator extends ChunkGenerator {
    }

}
