package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.BuildBorder;
import com.magmaguy.betterstructures.modules.ChunkData;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
    private final HashSet<Vector3i> emptyChunks = new HashSet<>();
    private final Player player;
    private final Map<Vector3i, ChunkData> chunkMap = new HashMap<>();
    private final int interval;
    private final int radius;
    private World world;

    public WaveFunctionCollapseGenerator(String worldName, int radius, int interval, Player player, String startingModule) {
        this.player = player;
        this.interval = interval;
        this.radius = radius;
        generateWorld(worldName);
        initializeChunkData();
        start(startingModule);
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
        Logger.debug("empty size: " + elements.size());
        Vector3i selectedLocation = null;
        if (elements.isEmpty()) {return selectedLocation;}

        if (elements.size() > 1) {
            double smallestDistance = Double.MAX_VALUE;
            for (Vector3i chunkLocation : elements) {
                double distance = chunkLocation.lengthSquared();
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    selectedLocation = chunkLocation;
                }
            }
            Logger.debug("placing at " + selectedLocation);

            return selectedLocation;
        } else {
            selectedLocation = elements.get(0);
        }

        Logger.debug("placing at " + selectedLocation);

        return selectedLocation;
    }

    private void generateWorld(String worldName) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(World.Environment.NORMAL);
        worldCreator.keepSpawnInMemory(false);
        worldCreator.generator(new VoidGenerator());
        world = worldCreator.createWorld();
        world.setAutoSave(false);
        player.teleport(new Location(world, -8, 16, -8));
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void start(String startingModule) {
        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        paste(new Vector3i(), modulesContainer, validRotations.get(ThreadLocalRandom.current().nextInt(0, validRotations.size()))); //todo reenable rotations
        // Begin the recursive generation
        searchNextChunkToGenerate();
    }

    private void searchNextChunkToGenerate() {
        int mostElements = 0;
        List<Vector3i> elements = new ArrayList<>();

        for (ChunkData chunkData : chunkMap.values()) {

            if (chunkData.isGenerated() || chunkData.canOnlyBeNothing()) {
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

        Logger.debug("most elements for border: " + mostElements);
        Vector3i selectedChunkLocationKey = getClosestEmptyChunkLocationKey(elements);
        generateNextChunk(selectedChunkLocationKey);

        if (selectedChunkLocationKey != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    searchNextChunkToGenerate();
                }
            }.runTaskLater(MetadataHandler.PLUGIN, interval);
        } else player.sendTitle("Done!", "Generation complete!");
    }

    private void paste(Vector3i chunkLocation, ModulesContainer modulesContainer, Integer rotation) {
        Location pasteLocation = new Location(world, chunkLocation.x * 16, chunkLocation.y * 16, chunkLocation.z * 16);
        if (modulesContainer != null && modulesContainer.getClipboard() != null)
            Module.paste(modulesContainer.getClipboard(), pasteLocation.clone().add(-17, 0, -17), rotation);
        chunkMap.get(chunkLocation).paste(new ModulesContainer.PastableModulesContainer(modulesContainer, rotation));
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

    private static class VoidGenerator extends ChunkGenerator {
    }

}
