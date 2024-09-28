package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.BuildBorder;
import com.magmaguy.betterstructures.modules.ChunkData;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WaveFunctionCollapseGenerator {
    @Getter
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    private final HashSet<ChunkData> emptyChunks = new HashSet<>();
    private final Player player;
    private final Map<Vector3i, ChunkData> chunkMap = new HashMap<>();
    private final int interval;
    private final int radius;
    private final boolean systemShowcaseSpeed = false;
    private final int massPasteSize = 20;
    private World world;
    private int processedChunks = 0;
    private int massPasteCount = 0;
    private long startTime;

    public WaveFunctionCollapseGenerator(String worldName, int radius, int interval, Player player, String startingModule) {
        this.player = player;
        this.interval = interval;
        this.radius = radius;
        generateWorld(worldName);
        initializeChunkData();
        new BukkitRunnable() {
            @Override
            public void run() {
                start(startingModule);
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private void initializeChunkData() {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 20; y++) {
                for (int z = -radius; z <= radius; z++) {
                    chunkMap.put(new Vector3i(x, y, z), new ChunkData(new Vector3i(x, y, z), world, emptyChunks));
                }
            }
        }
        for (ChunkData chunkData : chunkMap.values()) {
            Vector3i chunkLocation = chunkData.getChunkLocation();
            chunkData.addNeighbor(BuildBorder.UP, chunkMap.get(new Vector3i(chunkLocation.x, chunkLocation.y + 1, chunkLocation.z)));
            chunkData.addNeighbor(BuildBorder.DOWN, chunkMap.get(new Vector3i(chunkLocation.x, chunkLocation.y - 1, chunkLocation.z)));
            chunkData.addNeighbor(BuildBorder.EAST, chunkMap.get(new Vector3i(chunkLocation.x + 1, chunkLocation.y, chunkLocation.z)));
            chunkData.addNeighbor(BuildBorder.WEST, chunkMap.get(new Vector3i(chunkLocation.x - 1, chunkLocation.y, chunkLocation.z)));
            chunkData.addNeighbor(BuildBorder.NORTH, chunkMap.get(new Vector3i(chunkLocation.x, chunkLocation.y, chunkLocation.z - 1)));
            chunkData.addNeighbor(BuildBorder.SOUTH, chunkMap.get(new Vector3i(chunkLocation.x, chunkLocation.y, chunkLocation.z + 1)));
        }
    }

    @Nullable
    private Vector3i getClosestEmptyChunkLocationKey(List<Vector3i> elements) {
        Vector3i selectedLocation = null;
        if (elements.isEmpty()) {
            return selectedLocation;
        }
        if (elements.size() > 1) {
            double smallestDistance = Double.MAX_VALUE;
            for (Vector3i chunkLocation : elements) {
                double distance = chunkLocation.lengthSquared();
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    selectedLocation = chunkLocation;
                }
            }
            return selectedLocation;
        } else {
            selectedLocation = elements.get(0);
        }
        return selectedLocation;
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
        paste(new Vector3i(), modulesContainer, validRotations.get(ThreadLocalRandom.current().nextInt(0, validRotations.size()))); //todo reenable rotations
        // Begin the recursive generation
        searchNextChunkToGenerate();
    }

    private void searchNextChunkToGenerate() {
        int mostElements = 0;
        List<Vector3i> elements = new ArrayList<>();

        Iterator<ChunkData> iterator = emptyChunks.iterator();

        while (iterator.hasNext()) {
            ChunkData chunkData = iterator.next();

            if (chunkData.isGenerated() || chunkData.canOnlyBeNothing()) {
                iterator.remove();
                continue;
            }

            int borders = chunkData.getGeneratedNeighborCount();

            if (borders > mostElements) {
                mostElements = borders;
                elements.clear();
                elements.add(chunkData.getChunkLocation());
            } else if (borders == mostElements) {
                elements.add(chunkData.getChunkLocation());
            }
        }

        Vector3i selectedChunkLocationKey = getClosestEmptyChunkLocationKey(elements);
        if (selectedChunkLocationKey == null) {
            done();
            return;
        }
        generateNextChunk(selectedChunkLocationKey);

        if (systemShowcaseSpeed) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    searchNextChunkToGenerate();
                }
            }.runTaskLater(MetadataHandler.PLUGIN, interval);
        } else if (processedChunks % 1000 != 0) {
            searchNextChunkToGenerate();
        } else {
            // Avoid stack overflows
            new BukkitRunnable() {
                @Override
                public void run() {
                    searchNextChunkToGenerate();
                }
            }.runTaskLaterAsynchronously(MetadataHandler.PLUGIN, 1);
        }
    }


    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer, Integer rotation) {
        if (processedChunks % 500 == 0) {
            Logger.debug("Processed " + processedChunks + " chunks, latest location " + chunkLocation.toString() + " with rotation " + rotation);
        }
        processedChunks++;
        if (systemShowcaseSpeed) actualPaste(chunkMap.get(chunkLocation));
        chunkMap.get(chunkLocation).processPaste(new ModulesContainer.PastableModulesContainer(modulesContainer, rotation));
    }

    private void actualPaste(ChunkData chunkData) {
        ModulesContainer modulesContainer = chunkData.getModulesContainer();
        if (modulesContainer == null || modulesContainer.getClipboard() == null) return;
        Location pasteLocation = new Location(world, chunkData.getChunkLocation().x * 16, chunkData.getChunkLocation().y * 16, chunkData.getChunkLocation().z * 16);
        Module.paste(modulesContainer.getClipboard(), pasteLocation.clone().add(-1, 0, -1), chunkData.getRotation());
    }

    private void generateNextChunk(Vector3i nextChunkKey) {
        // Get valid modules
        ModulesContainer.PastableModulesContainer pastableModulesContainer = ModulesContainer.pickRandomModuleFromSurroundings(chunkMap.get(nextChunkKey), chunkMap.get(nextChunkKey).getRotation());
        if (pastableModulesContainer == null) {
            Logger.warn("No valid modules to place at (" + nextChunkKey.x + ", " + nextChunkKey.y + ", " + nextChunkKey.z + ")");
            rollbackChunk(chunkMap.get(nextChunkKey));
            return;
        }

        // Paste the module
        paste(nextChunkKey, pastableModulesContainer.modulesContainer(), pastableModulesContainer.rotation());
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
            Logger.warn("Starting mass paste");
            if (player != null) {
                player.sendMessage("Starting mass paste...");
            }
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> instantPaste(-radius, -radius));
        } else {
            // Calculate and display the elapsed time
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            long seconds = (elapsedTime / 1000) % 60;
            long minutes = (elapsedTime / (1000 * 60)) % 60;
            long hours = elapsedTime / (1000 * 60 * 60);

            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            if (player != null) {
                player.sendMessage("Generation complete! Time taken: " + timeString);
            }
            Logger.warn("Generation complete! Time taken: " + timeString);
        }
    }


    private void instantPaste(int currentX, int currentZ) {
        massPasteCount++;
        if (massPasteCount % massPasteSize == 0) {
            Logger.debug("Pasting chunk " + currentX + ", " + currentZ);
        }
        int x = currentX + 1;
        int z = currentZ;
        if (x > radius) {
            x = -radius;
            z++;
        }
        if (z > radius) {
            // Mass paste is complete
            if (player != null) {
                player.sendMessage("Mass paste complete!");

                // Calculate and display the elapsed time
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;

                long seconds = (elapsedTime / 1000) % 60;
                long minutes = (elapsedTime / (1000 * 60)) % 60;
                long hours = elapsedTime / (1000 * 60 * 60);

                String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                player.sendMessage("Total time taken (including mass paste): " + timeString);
                Logger.warn("Total time taken (including mass paste): " + timeString);
            }
            return;
        }
        for (int y = -4; y <= 20; y++) {
            actualPaste(chunkMap.get(new Vector3i(x, y, z)));
        }
        int finalX = x;
        int finalZ = z;
        if (massPasteCount % massPasteSize == 0) {
            Bukkit.getScheduler().runTaskLater(MetadataHandler.PLUGIN, () -> instantPaste(finalX, finalZ), 1);
        } else {
            instantPaste(finalX, finalZ);
        }
    }


    private static class VoidGenerator extends ChunkGenerator {
    }

}
