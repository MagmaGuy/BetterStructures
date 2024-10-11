package com.magmaguy.betterstructures.modules;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class ChunkData {
    @Getter
    private final Map<BuildBorder, ChunkData> orientedNeighbours = new EnumMap<>(BuildBorder.class);
    @Getter
    private final World world;
    private final Vector3i chunkLocation;
    List<PriorityQueue<ChunkData>> emptyNeighborBuckets;
    @Getter
    private ModulesContainer modulesContainer;
    @Getter
    @Setter
    private int generatedNeighborCount = 0;

    public ChunkData(Vector3i chunkLocation, World world, List<PriorityQueue<ChunkData>> emptyNeighborBuckets) {
        this.chunkLocation = chunkLocation;
        this.world = world;
        this.emptyNeighborBuckets = emptyNeighborBuckets;
    }

    public Location getRealLocation(){
        return new Location(world, getChunkLocation().x * 16, getChunkLocation().y * 16, getChunkLocation().z * 16);
    }

    public void showDebugTextDisplays() {
        Color color = Color.fromRGB(ThreadLocalRandom.current().nextInt(0, 256), ThreadLocalRandom.current().nextInt(0, 256), ThreadLocalRandom.current().nextInt(0, 256));
        Vector3i centerLocation = getChunkLocation().mul(16).add(8, 8, 8);
        Location actualCenterLocation = new Location(world, centerLocation.x, centerLocation.y, centerLocation.z);
        spawnDebugText(actualCenterLocation, getModulesContainer().getClipboardFilename(), color, 1);
        spawnDebugText(actualCenterLocation.clone().add(new Vector(0, -1 / 4d, 0)), "Rotation: " + getModulesContainer().getRotation(), color, 1);

        //possible module borders
        for (Map.Entry<BuildBorder, List<ModulesContainer.NeighborTag>> buildBorderListEntry : getModulesContainer().getBorderTags().entrySet()) {
            Vector3i offset = switch (BuildBorder.transformDirection(buildBorderListEntry.getKey(), getModulesContainer().getRotation())) {
                case UP -> new Vector3i(0, 5, 0);
                case DOWN -> new Vector3i(0, -5, 0);
                case EAST -> new Vector3i(5, 0, 0);
                case WEST -> new Vector3i(-5, 0, 0);
                case NORTH -> new Vector3i(0, 0, -5);
                case SOUTH -> new Vector3i(0, 0, 5);
                default -> new Vector3i(0, 0, 0);
            };

            spawnDebugText(actualCenterLocation.clone().add(offset.x, offset.y, offset.z), buildBorderListEntry.getKey().name(), color, 1);
            int counter = 0;
            for (ModulesContainer.NeighborTag neighborTag : buildBorderListEntry.getValue()) {
                counter++;
                spawnDebugText(actualCenterLocation.clone().add(offset.x, offset.y - counter / 4d, offset.z), neighborTag.getTag(), color, 1);
            }
        }
    }

    private void spawnDebugText(Location location, String text, Color color, int scale) {
        TextDisplay textDisplay = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
        textDisplay.setBackgroundColor(color);
        textDisplay.setTextOpacity((byte) 1);
        textDisplay.setSeeThrough(true);
        textDisplay.setText(text);
        textDisplay.setViewRange(0.1f);
    }

    public Vector3i getChunkLocation() {
        return new Vector3i(chunkLocation.x, chunkLocation.y, chunkLocation.z);
    }

    public void addNeighbor(BuildBorder buildBorder, ChunkData neighborToAdd) {
        if (neighborToAdd == null) return;
        orientedNeighbours.put(buildBorder, neighborToAdd);
    }

    public void processPaste(ModulesContainer.PastableModulesContainer pastableModulesContainer) {
        modulesContainer = pastableModulesContainer.modulesContainer();
        orientedNeighbours.values().forEach(ChunkData::updateGeneratedNeighborCount);
    }

    public boolean isGenerated() {
        return modulesContainer != null;
    }

    private void resetData() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    Location blockLocation = new Location(world,
                            chunkLocation.x * 16 + x,
                            chunkLocation.y * 16 + y,
                            chunkLocation.z * 16 + z);
                    blockLocation.getBlock().setType(org.bukkit.Material.AIR);
                }
            }
        }
        this.modulesContainer = null;
//        updateGeneratedNeighborCount();
    }

    public void hardReset() {
        resetData();
        orientedNeighbours.values().forEach(ChunkData::resetData);
        updateGeneratedNeighborCount();
        orientedNeighbours.values().forEach(ChunkData::updateGeneratedNeighborCount);
    }

    private void recalculateGeneratedNeighborCount() {
        generatedNeighborCount = 0;
        for (ChunkData value : orientedNeighbours.values())
            if (value.isGenerated())
                this.generatedNeighborCount++;
    }

    public void updateGeneratedNeighborCount() {
        if (isGenerated()) return;
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