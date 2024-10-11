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
import java.util.concurrent.ThreadLocalRandom;

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
    private static final int Y_SIZE = MAX_Y_LEVEL - MIN_Y_LEVEL; // Y_SIZE = 25
    //    private final HashSet<ChunkData> emptyChunks = new HashSet<>();
    private final Player player;
    private final int radius;
    private final int massPasteSize = DefaultConfig.getModularChunkPastingSpeed();
    private final int totalMassPasteChunks;
    private final Set<Vector2i> processedXZ = new HashSet<>();
    private final int averageYLevels = 10; // this is used to estimate the progress for the chunk detection
    private final BossBar completionPercentage;
    private final Map<Vector3i, Long> distanceCache = new HashMap<>();
    private final List<Vector2i> spiralPositions;
    private final List<PriorityQueue<ChunkData>> emptyNeighborBuckets = new ArrayList<>(7);
    private int interval;
    private boolean systemShowcaseSpeed = false;
    private ChunkData[][][] chunkArray;
    private World world;
    private int processedChunks = 0;
    private int massPasteCount = 0;
    private long startTime;
    private final boolean debug;

    public WaveFunctionCollapseGenerator(String worldName, int radius, boolean debug, int interval, Player player, String startingModule) {
        this.player = player;
        this.debug = debug;
        this.interval = interval;
        systemShowcaseSpeed = true;
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
                for (int i = 0; i <= 6; i++) {
                    emptyNeighborBuckets.add(new PriorityQueue<>(Comparator.comparingLong(chunkData -> getDistanceSquared(chunkData.getChunkLocation()))));
                }

                initializeChunkData();

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
                for (int i = 0; i <= 6; i++) {
                    emptyNeighborBuckets.add(new PriorityQueue<>(Comparator.comparingLong(chunkData ->
                            getDistanceSquared(chunkData.getChunkLocation()))));
                }

                initializeChunkData();
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

    private void clearBar() {
        bars.remove(completionPercentage);
        completionPercentage.removeAll();
    }

    private void updateProgress(double progress, String message) {
        completionPercentage.setProgress(progress);
        completionPercentage.setTitle(message);
    }

    private Long getDistanceSquared(Vector3i location) {
        return distanceCache.computeIfAbsent(location, Vector3i::lengthSquared);
    }

    private void initializeChunkData() {
        int size = 2 * radius + 1; // Size in X and Z dimensions
        chunkArray = new ChunkData[size][Y_SIZE][size];

        // Initialize ChunkData instances
        for (int x = -radius; x <= radius; x++) {
            int arrayX = x + radius;
            for (int y = MIN_Y_LEVEL; y < MAX_Y_LEVEL; y++) {
                int arrayY = y - MIN_Y_LEVEL;
                for (int z = -radius; z <= radius; z++) {
                    int arrayZ = z + radius;
                    Vector3i chunkLocation = new Vector3i(x, y, z);
                    ChunkData chunkData = new ChunkData(chunkLocation, world, emptyNeighborBuckets);

                    // Add to the appropriate bucket
                    emptyNeighborBuckets.get(0).add(chunkData);

                    chunkArray[arrayX][arrayY][arrayZ] = chunkData;
                }
            }
        }

        // Initialize neighbors
        int xSize = chunkArray.length;
        int ySize = chunkArray[0].length;
        int zSize = chunkArray[0][0].length;

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    ChunkData chunkData = chunkArray[x][y][z];
                    if (chunkData != null) {
                        for (int i = 0; i < NEIGHBOR_OFFSETS.length; i++) {
                            Vector3i offset = NEIGHBOR_OFFSETS[i];
                            int neighborX = x + offset.x();
                            int neighborY = y + offset.y();
                            int neighborZ = z + offset.z();

                            // Check boundaries
                            if (neighborX >= 0 && neighborX < xSize &&
                                    neighborY >= 0 && neighborY < ySize &&
                                    neighborZ >= 0 && neighborZ < zSize) {

                                ChunkData neighborChunkData = chunkArray[neighborX][neighborY][neighborZ];
                                chunkData.addNeighbor(NEIGHBOR_DIRECTIONS[i], neighborChunkData);
                            } else {
                                // No neighbor in this direction
                                chunkData.addNeighbor(NEIGHBOR_DIRECTIONS[i], null);
                            }
                        }
                    }
                }
            }
        }
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
        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (startingModule == null) {
            Logger.sendMessage(player, "Starting chunk was null! Canceling!");
            Logger.warn("Starting chunk was null! Canceling!");
            return;
        }
        paste(new Vector3i(), modulesContainer, validRotations.get(ThreadLocalRandom.current().nextInt(0, validRotations.size()))); //todo reenable rotations
        // Begin the recursive generation
        searchNextChunkToGenerate();
    }

    private void searchNextChunkToGenerate() {
        if (systemShowcaseSpeed) {
            ChunkData selectedChunkLocationKey = getNextChunk();
            if (selectedChunkLocationKey == null) {
                done();
                return;
            }
            generateNextChunk(selectedChunkLocationKey);

            new BukkitRunnable() {
                @Override
                public void run() {
                    searchNextChunkToGenerate();
                }
            }.runTaskLater(MetadataHandler.PLUGIN, interval);
        } else {
            while (true) {
                ChunkData selectedChunkLocationKey = getNextChunk();
                if (selectedChunkLocationKey == null) {
                    done();
                    return;
                }
                generateNextChunk(selectedChunkLocationKey);
            }
        }
    }

    private ChunkData getNextChunk() {
        for (int i = 6; i >= 0; i--) {
            PriorityQueue<ChunkData> bucket = emptyNeighborBuckets.get(i);
            if (!bucket.isEmpty()) {
                return bucket.poll(); // Retrieves and removes the chunk with the smallest distance
            }
        }
        return null; // All buckets are empty
    }

    private void updateAssemblingProgress() {
        int totalEstimatedChunks = ((2 * radius + 1) * (2 * radius + 1)) * averageYLevels;
        double progress = Round.twoDecimalPlaces(((double) processedXZ.size() * averageYLevels) / totalEstimatedChunks * 100);
        Logger.debug("[" + progress + "%] Processed " + processedChunks + " chunks");
        updateProgress(progress / 100L, "Assembling modules - " + progress + "% done...");
    }

    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer, Integer rotation) {
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

        chunkData.processPaste(new ModulesContainer.PastableModulesContainer(modulesContainer, rotation));

        if (systemShowcaseSpeed) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualPaste(chunkData);
                }
            }.runTask(MetadataHandler.PLUGIN);
        }
    }

    private ChunkData getChunkDataAt(int x, int y, int z) {
        int arrayX = x + radius;
        int arrayY = y - MIN_Y_LEVEL;
        int arrayZ = z + radius;

        // Check bounds if necessary
        if (arrayX >= 0 && arrayX < chunkArray.length &&
                arrayY >= 0 && arrayY < chunkArray[0].length &&
                arrayZ >= 0 && arrayZ < chunkArray[0][0].length) {

            return chunkArray[arrayX][arrayY][arrayZ];
        } else {
            return null;
        }
    }

    private void actualPaste(ChunkData chunkData) {
        ModulesContainer modulesContainer = chunkData.getModulesContainer();
        if (modulesContainer == null || modulesContainer.getClipboard() == null) return;
        Module.paste(modulesContainer.getClipboard(), chunkData.getRealLocation().add(-1, 0, -1), chunkData.getModulesContainer().getRotation());
        if (debug) chunkData.showDebugTextDisplays();
    }

    private void actualBatchPaste(List<ChunkData> batchedChunkData) {
        Module.batchPaste(batchedChunkData, world);
        if (debug) batchedChunkData.forEach(ChunkData::showDebugTextDisplays);
    }

    private void generateNextChunk(ChunkData chunkData) {
        // Get valid modules
        ModulesContainer.PastableModulesContainer pastableModulesContainer = ModulesContainer.pickRandomModuleFromSurroundings(chunkData);
        if (pastableModulesContainer == null) {
            Logger.warn("No valid modules to place at (" + chunkData.getChunkLocation().x + ", " + chunkData.getChunkLocation().y + ", " + chunkData.getChunkLocation().z + ")");
            rollbackChunk(chunkData);
            return;
        }

        // Paste the module
        paste(chunkData.getChunkLocation(), pastableModulesContainer.modulesContainer(), pastableModulesContainer.rotation());
    }

    private void rollbackChunk(ChunkData chunkData) {
        Vector3i location = chunkData.getChunkLocation();
        Logger.warn("Rolling back chunk at (" + location.x + ", " + location.y + ", " + location.z + ")");
        player.sendMessage("Rolling back invalid chunk at (" + location.x + ", " + location.y + ", " + location.z + ")");
        chunkData.hardReset();
    }

    private void done() {
        if (player != null) {
            player.sendTitle("Done!", "Generation complete!");
        }

        Logger.warn("Done with infinity generator!");

        if (!systemShowcaseSpeed) {
            timeMessage("Module assembly ");
            Logger.warn("Starting mass paste");
            if (player != null) player.sendMessage("Starting mass paste...");
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, this::instantPaste);
        } else {
            timeMessage("Generation");
            clearBar();
        }
    }

    private void cleanup(){
        emptyNeighborBuckets.clear();
        spiralPositions.clear();
        distanceCache.clear();
        processedXZ.clear();
        chunkArray = null;
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
//                        if (chunkData == null) {
//                            Logger.warn("ChunkData not found at location: " + x + ", " + y + ", " + z);
//                        } else {
//                            actualPaste(chunkData);
//                        }
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
                // Else, the task will automatically reschedule itself on the next tick
            }
        };

        // Schedule the task to run every tick until it's canceled
        pasteTask.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    private static class VoidGenerator extends ChunkGenerator {
    }

}
