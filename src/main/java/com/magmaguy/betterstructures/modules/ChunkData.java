package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

public class ChunkData {
    @Getter
    private final Map<BuildBorder, ChunkData> orientedNeighbours = new EnumMap<>(BuildBorder.class);
    private final World world;
    @Getter
    private final Vector3i chunkLocation;
    private final HashSet<Vector3i> emptyChunksCopy;
    @Getter
    @Setter
    private Integer rotation = null;
    private ModulesContainer.PastableModulesContainer pastableModulesContainer = null;

    public ChunkData(Vector3i chunkLocation, World world, HashSet<Vector3i> emptyChunksCopy) {
        this.chunkLocation = chunkLocation;
        this.world = world;
        this.emptyChunksCopy = emptyChunksCopy;
    }

    public boolean isNothing() {
        return pastableModulesContainer != null && pastableModulesContainer.modulesContainer().isNothing();
    }

    public boolean canOnlyBeNothing() {
        for (ChunkData value : orientedNeighbours.values()) {
            if (!value.isNothing()) return false;
        }
        return true;
    }

    public void addNeighbor(BuildBorder buildBorder, ChunkData neighborToAdd) {
        if (neighborToAdd == null) return;
        orientedNeighbours.put(buildBorder, neighborToAdd);
    }

    public int getGeneratedNeighborCount() {
        int count = 0;
        if (orientedNeighbours.values().isEmpty() ) Logger.debug("uh wtf it's empty!!))!)!)!)");
        for (ChunkData neighbor : orientedNeighbours.values()) {
            if (neighbor.isGenerated()) count++;
        }
        return count;
    }

    public void paste(ModulesContainer.PastableModulesContainer pastableModulesContainer) {
        this.pastableModulesContainer = pastableModulesContainer;
        this.rotation = pastableModulesContainer.rotation();
        if (orientedNeighbours.get(BuildBorder.UP) != null) {
            orientedNeighbours.get(BuildBorder.UP).setRotation(rotation);
        }
        if (orientedNeighbours.get(BuildBorder.DOWN) != null) {
            orientedNeighbours.get(BuildBorder.DOWN).setRotation(rotation);
        }
    }

    public boolean isGenerated() {
        return pastableModulesContainer != null;
    }

    private void resetData() {
        clearChunk();
        this.pastableModulesContainer = null;
        this.rotation = null;
    }

    public void hardReset() {
        resetData();
        orientedNeighbours.values().forEach(neighbour -> {
            Logger.debug("resetting neighbor " + neighbour.getChunkLocation().toString());
            neighbour.resetData();
        });
    }

    private void clearChunk() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    Location blockLocation = new Location(world, chunkLocation.x * 16 + x, chunkLocation.y * 16 + y, chunkLocation.z * 16 + z);
                    blockLocation.getBlock().setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    public ModulesContainer.BorderTags collectValidBordersFromNeighbours() {
        ModulesContainer.BorderTags borderTags = new ModulesContainer.BorderTags(new EnumMap<>(BuildBorder.class));
        for (Map.Entry<BuildBorder, ChunkData> buildBorderChunkDataEntry : orientedNeighbours.entrySet()) {
            if (buildBorderChunkDataEntry.getValue().pastableModulesContainer != null &&
                    buildBorderChunkDataEntry.getValue().pastableModulesContainer.modulesContainer() != null)
                borderTags.put(
                        buildBorderChunkDataEntry.getKey(),
                        buildBorderChunkDataEntry.getValue().pastableModulesContainer.modulesContainer().getBorderTags()
                                .getRotatedTagsForDirection(
                                        buildBorderChunkDataEntry.getKey().getOpposite(),
                                        buildBorderChunkDataEntry.getValue().getRotation()));
        }
        return borderTags;
    }
}