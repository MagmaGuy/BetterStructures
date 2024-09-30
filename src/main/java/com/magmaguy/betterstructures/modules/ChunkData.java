package com.magmaguy.betterstructures.modules;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ChunkData {
    @Getter
    private final Map<BuildBorder, ChunkData> orientedNeighbours = new EnumMap<>(BuildBorder.class);
    private final World world;
    @Getter
    private final Vector3i chunkLocation;
    private String clipboardName = null;
    private Byte rotation = null;
    @Getter
    @Setter
    private int generatedNeighborCount = 0;
    List<PriorityQueue<ChunkData>> emptyNeighborBuckets;

    public ChunkData(Vector3i chunkLocation, World world, List<PriorityQueue<ChunkData>> emptyNeighborBuckets) {
        this.chunkLocation = chunkLocation;
        this.world = world;
        this.emptyNeighborBuckets = emptyNeighborBuckets;
    }

    public Integer getRotation() {
        if (rotation == null) return null;
        return switch (rotation) {
            case 0 -> 0;
            case 1 -> 90;
            case 2 -> 180;
            case 3 -> 270;
            default -> null;
        };
    }

    public void setRotation(Integer intRotation) {
        if (intRotation == null) {
            this.rotation = null;
            return;
        }
        switch (intRotation) {
            case 0 -> rotation = 0;
            case 90 -> rotation = 1;
            case 180 -> rotation = 2;
            case 270 -> rotation = 3;
            default -> rotation = 0;
        }
    }

    public ModulesContainer getModulesContainer() {
        if (clipboardName == null) return null;
        return ModulesContainer.getModulesContainers().get(clipboardName);
    }

    private void serializeData(ModulesContainer.PastableModulesContainer pastableModulesContainer) {
        if (pastableModulesContainer.modulesContainer().getClipboardFilename() == null)
            clipboardName = null;
        else
            clipboardName = pastableModulesContainer.modulesContainer().getClipboardFilename();
        setRotation(pastableModulesContainer.rotation());
    }

    public boolean isNothing() {
        return clipboardName != null && getModulesContainer().isNothing();
    }

    public boolean canOnlyBeNothing() {
        for (ChunkData value : orientedNeighbours.values()) {
            if (value.isGenerated() && !value.isNothing()) return false;
        }
        return true;
    }

    public void addNeighbor(BuildBorder buildBorder, ChunkData neighborToAdd) {
        if (neighborToAdd == null) return;
        orientedNeighbours.put(buildBorder, neighborToAdd);
    }

    public int getGeneratedNeighborCount() {
        int count = 0;
        for (ChunkData neighbor : orientedNeighbours.values())
            if (neighbor.isGenerated()) count++;
        return count;
    }

    public void processPaste(ModulesContainer.PastableModulesContainer pastableModulesContainer) {
        serializeData(pastableModulesContainer);
        int rotation = pastableModulesContainer.rotation();
        if (pastableModulesContainer.modulesContainer().getModulesConfigField() != null &&
                pastableModulesContainer.modulesContainer().getModulesConfigField().isEnforceVerticalRotation()) {
            if (orientedNeighbours.get(BuildBorder.UP) != null) {
                orientedNeighbours.get(BuildBorder.UP).setRotation(rotation);
            }
            if (orientedNeighbours.get(BuildBorder.DOWN) != null) {
                orientedNeighbours.get(BuildBorder.DOWN).setRotation(rotation);
            }
        }
//        emptyChunks.remove(this); this should be done at the time of selecting it
        orientedNeighbours.values().forEach(orientedNeighbour -> {
            if (!orientedNeighbour.isGenerated()) orientedNeighbour.updateGeneratedNeighborCount();
        });
    }

    public boolean isGenerated() {
        return clipboardName != null;
    }

    private void resetData() {
        clearChunk();
        this.clipboardName = null;
        setRotation(getValidRotationFromNeighbor(BuildBorder.DOWN));
        if (rotation == null)
            setRotation(getValidRotationFromNeighbor(BuildBorder.UP));
        updateGeneratedNeighborCount();
//        recalculateGeneratedNeighborCount();
    }

    private Integer getValidRotationFromNeighbor(BuildBorder border) {
        ModulesContainer modulesContainer = orientedNeighbours.get(border).getModulesContainer();
        if (orientedNeighbours.get(border) != null &&
                orientedNeighbours.get(border).rotation != null &&
                modulesContainer != null &&
                modulesContainer.getModulesConfigField() != null &&
                modulesContainer.getModulesConfigField().isEnforceVerticalRotation()) {
            return orientedNeighbours.get(border).getRotation();
        }
        return null;
    }

    public void hardReset() {
        resetData();
        orientedNeighbours.values().forEach(ChunkData::resetData);
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
            ModulesContainer modulesContainer = buildBorderChunkDataEntry.getValue().getModulesContainer();
            if (modulesContainer != null)
                borderTags.put(
                        buildBorderChunkDataEntry.getKey(),
                        modulesContainer.getBorderTags().getRotatedTagsForDirection(
                                buildBorderChunkDataEntry.getKey().getOpposite(),
                                buildBorderChunkDataEntry.getValue().getRotation()));
        }
        return borderTags;
    }

    private void recalculateGeneratedNeighborCount() {
        generatedNeighborCount = 0;
        for (ChunkData value : orientedNeighbours.values())
            if (value.isGenerated())
                this.generatedNeighborCount++;
    }

    public void updateGeneratedNeighborCount() {
        int oldCount = this.generatedNeighborCount;
        recalculateGeneratedNeighborCount();

        if (oldCount != this.generatedNeighborCount) {
            // Remove from old bucket
            emptyNeighborBuckets.get(oldCount).remove(this);

            // Add to new bucket
            emptyNeighborBuckets.get(this.generatedNeighborCount).add(this);
        }
    }

}