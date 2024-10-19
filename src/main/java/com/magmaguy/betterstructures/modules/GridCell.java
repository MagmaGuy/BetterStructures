package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
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
    @Getter
    private final World world;
    private final Vector3i cellLocation;
    private final Map<Vector3i, GridCell> cellMap;
    @Getter
    private ModulesContainer modulesContainer;
    @Getter
    @Setter
    private int generatedNeighborCount = 0;
    private List<TextDisplay> textDisplays;

    @Getter
    private List<ModulesContainer> validOptions = null;

    public Vector3i getCellLocation() {
        return new Vector3i(cellLocation);
    }

    public GridCell(Vector3i cellLocation, World world, Map<Vector3i, GridCell> cellMap) {
        this.cellLocation = cellLocation;
        this.world = world;
        this.cellMap = cellMap;
    }

    public void updateValidOptions() {
        if (isGenerated()) return;
//        if (validOptions != null && !forceUpdate) return;
        validOptions = ModulesContainer.getValidModulesFromSurroundings(this);
    }

    public int getValidOptionCount() {
        if (validOptions == null) updateValidOptions();
        if (validOptions == null) {
            Logger.warn("Valid options were null when trying to get the size; this should not happen.");
            return 0;
        }
        return validOptions.size();
    }

    public Map<Direction, GridCell> getOrientedNeighbors() {
        Map<Direction, GridCell> neighbors = new EnumMap<>(Direction.class);

        for (Map.Entry<Direction, Vector3i> entry : SpatialGrid.DIRECTION_OFFSETS.entrySet()) {
            Vector3i neighborPos = new Vector3i(cellLocation).add(entry.getValue());
            neighbors.put(entry.getKey(), cellMap.get(neighborPos));
        }

        return neighbors;
    }

    public Location getRealLocation() {
        return new Location(world, cellLocation.x * 16, cellLocation.y * 16, cellLocation.z * 16);
    }

    public void showDebugTextDisplays() {
        if (modulesContainer == null) return;
        if (textDisplays != null) textDisplays.forEach(Entity::remove);
        else textDisplays = new ArrayList<>();
        textDisplays.clear();
        Color color = Color.fromRGB(ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(256));
        Vector3i centerLocation = new Vector3i(cellLocation).mul(16).add(8, 8, 8);
        Location actualCenterLocation = new Location(world, centerLocation.x, centerLocation.y, centerLocation.z);
        spawnDebugText(actualCenterLocation, modulesContainer.getClipboardFilename(), color, 1);
        spawnDebugText(actualCenterLocation.clone().add(0, -0.25, 0), "Rotation: " + modulesContainer.getRotation(), color, 1);

        for (Map.Entry<Direction, List<ModulesContainer.NeighborTag>> entry : modulesContainer.getBorderTags().entrySet()) {
            Vector3i offset = switch (entry.getKey()) {
                case UP -> new Vector3i(0, 5, 0);
                case DOWN -> new Vector3i(0, -5, 0);
                case EAST -> new Vector3i(5, 0, 0);
                case WEST -> new Vector3i(-5, 0, 0);
                case NORTH -> new Vector3i(0, 0, -5);
                case SOUTH -> new Vector3i(0, 0, 5);
            };

            spawnDebugText(actualCenterLocation.clone().add(offset.x, offset.y, offset.z), entry.getKey().name(), color, 1);
            int counter = 0;
            for (ModulesContainer.NeighborTag neighborTag : entry.getValue()) {
                counter++;
                spawnDebugText(actualCenterLocation.clone().add(offset.x, offset.y - counter / 4.0, offset.z), neighborTag.getTag(), color, 1);
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

    public void processPaste(ModulesContainer modulesContainer) {
        this.modulesContainer = modulesContainer;
        getOrientedNeighbors().values().forEach(neighbor -> {
            if (neighbor != null && !neighbor.isGenerated()) {
                neighbor.updateValidOptions();
            }
        });
    }

    public void updateNeighborCount() {
        generatedNeighborCount = (int) getOrientedNeighbors().values().stream().filter(Objects::nonNull).count();
    }

    public boolean isGenerated() {
        return modulesContainer != null;
    }

    private void resetData(boolean showGenerationForShowcase) {
        if (showGenerationForShowcase) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Location blockLocation = new Location(world,
                                cellLocation.x * 16 + x,
                                cellLocation.y * 16 + y,
                                cellLocation.z * 16 + z);
                        blockLocation.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        this.modulesContainer = null;
        this.validOptions = null;
    }

    public int hardReset(SpatialGrid spatialGrid, boolean slowGenerationForShowcase, int radius) {
        Set<GridCell> visited = new HashSet<>();
        Set<GridCell> resetCells = new HashSet<>();
        int resetGeneratedCells = hardResetRecursive(slowGenerationForShowcase, radius, visited, resetCells);

        for (GridCell cell : resetCells) {
            spatialGrid.updateCellPriority(cell);
        }
        return resetGeneratedCells;
    }

    private int hardResetRecursive(boolean showGenerationForShowcase, int radius, Set<GridCell> visited, Set<GridCell> resetCells) {
        if (radius < 0 || !visited.add(this)) {
            return 0;
        }

        boolean wasGenerated = this.isGenerated(); // Check if the cell was generated before reset
        resetData(showGenerationForShowcase);
        updateNeighborCount();

        resetCells.add(this);

        int resetGeneratedCells = wasGenerated ? 1 : 0;

        if (radius > 0) {
            for (GridCell neighbor : getOrientedNeighbors().values()) {
                if (neighbor != null) {
                    resetGeneratedCells += neighbor.hardResetRecursive(showGenerationForShowcase, radius - 1, visited, resetCells);
                }
            }
        }
        return resetGeneratedCells;
    }
}
