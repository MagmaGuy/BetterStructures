package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GridCell {
    public static final Map<Direction, Vector3i> NEIGHBOURS = Map.of(
            Direction.NORTH, new Vector3i(0, 0, -1),
            Direction.SOUTH, new Vector3i(0, 0, 1),
            Direction.EAST, new Vector3i(1, 0, 0),
            Direction.WEST, new Vector3i(-1, 0, 0),
            Direction.UP, new Vector3i(0, 1, 0),
            Direction.DOWN, new Vector3i(0, -1, 0)
    );
    @Getter
    private final World world;
    private final Vector3i chunkLocation;
    private final Map<Vector3i, GridCell> chunkMap;
    @Getter
    private ModulesContainer modulesContainer;
    @Getter
    @Setter
    private int generatedNeighborCount = 0;
    private List<TextDisplay> textDisplays;

    @Getter
    private List<ModulesContainer> validOptions = null;

    @Getter
    @Setter
    private int validOptionsSize;

    public GridCell(Vector3i chunkLocation, World world, Map<Vector3i, GridCell> chunkMap) {
        this.chunkLocation = chunkLocation;
        this.world = world;
        this.chunkMap = chunkMap;
    }

    public void updateValidOptions(boolean forceUpdate) {
        if (isGenerated()) return;
        if (validOptions != null && !forceUpdate) return;
        validOptions = ModulesContainer.getValidModulesFromSurroundings(this);
    }

    public int getValidOptionCount() {
        if (validOptions == null) updateValidOptions(false);
        if (validOptions == null) Logger.warn("valid options was null when trying to get the size that should not happen");
        return validOptions.size();
    }

    public Map<Direction, GridCell> getOrientedNeighbours() {
        Vector3i chunkPos = getChunkLocation();
        Map<Direction, GridCell> results = new HashMap<>();

        for (Map.Entry<Direction, Vector3i> entry : NEIGHBOURS.entrySet())
            results.put(entry.getKey(), chunkMap.get(new Vector3i(chunkPos).add(entry.getValue())));

        return results;
    }

    public Location getRealLocation() {
        return new Location(world, getChunkLocation().x * 16, getChunkLocation().y * 16, getChunkLocation().z * 16);
    }

    public void showDebugTextDisplays() {
        if (getModulesContainer() == null) return;
        if (textDisplays != null) textDisplays.forEach(Entity::remove);
        else textDisplays = new ArrayList<>();
        textDisplays.clear();
        Color color = Color.fromRGB(ThreadLocalRandom.current().nextInt(0, 256), ThreadLocalRandom.current().nextInt(0, 256), ThreadLocalRandom.current().nextInt(0, 256));
        Vector3i centerLocation = getChunkLocation().mul(16).add(8, 8, 8);
        Location actualCenterLocation = new Location(world, centerLocation.x, centerLocation.y, centerLocation.z);
        spawnDebugText(actualCenterLocation, getModulesContainer().getClipboardFilename(), color, 1);
        spawnDebugText(actualCenterLocation.clone().add(new Vector(0, -1 / 4d, 0)), "Rotation: " + getModulesContainer().getRotation(), color, 1);

        //possible module borders
        for (Map.Entry<Direction, List<ModulesContainer.NeighborTag>> buildBorderListEntry : getModulesContainer().getBorderTags().entrySet()) {
            Vector3i offset = switch (buildBorderListEntry.getKey()) {
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
        textDisplays.add(textDisplay);
    }

    public Vector3i getChunkLocation() {
        return new Vector3i(chunkLocation.x, chunkLocation.y, chunkLocation.z);
    }

    public void processPaste(ModulesContainer modulesContainer) {
        this.modulesContainer = modulesContainer;
        getOrientedNeighbours().values().forEach(chunkData -> {if (chunkData !=null)chunkData.updateValidOptions(true);});
    }

    public void updateNeighbourCount() {
        generatedNeighborCount = 0;
        for (GridCell neighbor : getOrientedNeighbours().values())
            if (neighbor != null)
                generatedNeighborCount++;
    }

    public boolean isGenerated() {
        return modulesContainer != null;
    }

    private void resetData(boolean showGenerationForShowcase) {
        if (showGenerationForShowcase)
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Location blockLocation = new Location(world,
                                chunkLocation.x * 16 + x,
                                chunkLocation.y * 16 + y,
                                chunkLocation.z * 16 + z);
                        blockLocation.getBlock().setType(Material.AIR);
                    }
                }
            }
        this.modulesContainer = null;
    }

    public void hardReset(SpatialGrid spatialGrid, boolean slowGenerationForShowcase, int radius) {
        Set<GridCell> visited = new HashSet<>();
        Set<GridCell> resetCells = new HashSet<>();
        hardResetRecursive(slowGenerationForShowcase, radius, visited, resetCells);

        // After all resets are complete, update priorities
        for (GridCell cell : resetCells) {
            spatialGrid.updateCellPriority(cell);
        }
    }

    private void hardResetRecursive(boolean slowGenerationForShowcase, int radius, Set<GridCell> visited, Set<GridCell> resetCells) {
        if (radius < 0 || !visited.add(this)) {
            return;
        }

        resetData(slowGenerationForShowcase);
        updateNeighbourCount();

        resetCells.add(this);

        if (radius > 0) {
            for (GridCell neighbor : getOrientedNeighbours().values()) {
                if (neighbor != null) {
                    neighbor.hardResetRecursive(slowGenerationForShowcase, radius - 1, visited, resetCells);
                }
            }
        }
    }
}