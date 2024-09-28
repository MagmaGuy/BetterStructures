package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ChunkData {
    @Getter
    private final Map<ModulesConfigFields.BuildBorder, ChunkData> orientedNeighbours = new EnumMap<>(ModulesConfigFields.BuildBorder.class);
    private final World world;
    @Getter
    private final Vector3i chunkLocation;
    private final HashSet<Vector3i> emptyChunksCopy;
    private Boolean canOnlyBeNothing = null;
    @Getter
    @Setter
    private Integer rotation = null;
    private ModulesContainer.PastableModulesContainer pastableModulesContainer = null;

    public ChunkData(Vector3i chunkLocation, World world, HashSet<Vector3i> emptyChunksCopy) {
        this.chunkLocation = chunkLocation;
        this.world = world;
        this.emptyChunksCopy = emptyChunksCopy;
    }

    public Boolean getCanOnlyBeNothing() {
        if (canOnlyBeNothing != null) return canOnlyBeNothing;
        return recalculateCanOnlyBeNothing();
    }

    public boolean recalculateCanOnlyBeNothing() {
        boolean localCheck = true;
        boolean wasNotNull = false;
        for (List<String> value : collectValidBordersFromNeighbours().values()) {
            if (value == null) continue;
            wasNotNull = true;
            for (String string : value) {
                if (!string.equalsIgnoreCase("nothing")) {
                    localCheck = false;
                    break;
                }
            }
            if (!localCheck) break;
        }
        if (!wasNotNull) return canOnlyBeNothing = false;
        return canOnlyBeNothing = localCheck;
    }

    public void addNeighbor(ModulesConfigFields.BuildBorder buildBorder, ChunkData neighborToAdd) {
        if (neighborToAdd == null) return;
        orientedNeighbours.put(buildBorder, neighborToAdd);
    }

    public int getGeneratedNeighborCount() {
        int count = 0;
        for (ChunkData neighbor : orientedNeighbours.values()) {
            if (neighbor.isGenerated()) count++;
        }
        return count;
    }

    public void paste(ModulesContainer.PastableModulesContainer pastableModulesContainer) {
        this.pastableModulesContainer = pastableModulesContainer;
        this.rotation = pastableModulesContainer.rotation();
        if (orientedNeighbours.get(ModulesConfigFields.BuildBorder.UP) != null) {
            orientedNeighbours.get(ModulesConfigFields.BuildBorder.UP).setRotation(rotation);
        }
        if (orientedNeighbours.get(ModulesConfigFields.BuildBorder.DOWN) != null) {
            orientedNeighbours.get(ModulesConfigFields.BuildBorder.DOWN).setRotation(rotation);
        }

        orientedNeighbours.values().forEach(neighbour -> {
            if (!neighbour.isGenerated() && !neighbour.recalculateCanOnlyBeNothing()) {
                emptyChunksCopy.add(neighbour.chunkLocation);
            } else if (neighbour.getCanOnlyBeNothing()) emptyChunksCopy.remove(neighbour.chunkLocation);
        });

        emptyChunksCopy.remove(chunkLocation);
    }

    private boolean isGenerated() {
        return pastableModulesContainer != null || getCanOnlyBeNothing();
    }

    private void resetData() {
        clearChunk();
        this.pastableModulesContainer = null;
        this.canOnlyBeNothing = null;
        this.rotation = null;
//        emptyChunksCopy.remove(chunkLocation);
    }

    public void hardReset() {
        resetData();
        orientedNeighbours.values().forEach(neighbour -> {
            Logger.debug("resetting neighbor " + neighbour.getChunkLocation().toString());
            neighbour.resetNeighbor();
        });

        orientedNeighbours.values().forEach(neighbour -> {
            Logger.debug("recalculating neighbor");
            if (!recalculateCanOnlyBeNothing())
                emptyChunksCopy.add(chunkLocation);
            else
                emptyChunksCopy.remove(chunkLocation);
        });

//        if (!recalculateCanOnlyBeNothing()) emptyChunksCopy.add(chunkLocation);
    }

    public void resetNeighbor() {
        resetData();
        Logger.debug("resetting neighbor");
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
        ModulesContainer.BorderTags borderTags = new ModulesContainer.BorderTags(new EnumMap<>(ModulesConfigFields.BuildBorder.class));
        for (Map.Entry<ModulesConfigFields.BuildBorder, ChunkData> buildBorderChunkDataEntry : orientedNeighbours.entrySet()) {
            if (buildBorderChunkDataEntry.getValue().pastableModulesContainer != null &&
                    buildBorderChunkDataEntry.getValue().pastableModulesContainer.modulesContainer() != null)
                borderTags.put(
                        buildBorderChunkDataEntry.getKey(),
                        buildBorderChunkDataEntry.getValue().pastableModulesContainer.modulesContainer().getBorderTags().getRotatedTagsForDirection(buildBorderChunkDataEntry.getKey().getOpposite(), buildBorderChunkDataEntry.getValue().getRotation()));
        }
        return borderTags;
    }
}