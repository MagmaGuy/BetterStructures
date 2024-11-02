package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GridCell {
    private final Vector3i cellLocation;
    private final SpatialGrid grid;
    @Getter private final World world;
    private final Map<Vector3i, GridCell> cellMap;

    @Getter @Setter private ModulesContainer modulesContainer;
    @Getter @Setter private int generatedNeighborCount = 0;
    @Getter private List<ModulesContainer> validOptions = null;
    private List<TextDisplay> textDisplays;

    @Getter private final int magnitudeSquared;

    /**
     * Creates a new GridCell.
     * @param cellLocation The grid coordinates of this cell
     * @param world The world this cell belongs to
     * @param grid The spatial grid this cell belongs to
     * @param cellMap The global cell map reference
     */
    public GridCell(Vector3i cellLocation, World world, SpatialGrid grid, Map<Vector3i, GridCell> cellMap) {
        this.cellLocation = new Vector3i(cellLocation);  // Defensive copy
        this.world = world;
        this.grid = grid;
        this.cellMap = cellMap;
        this.magnitudeSquared = (int) cellLocation.lengthSquared();
    }

    /**
     * Gets a safe copy of the cell location.
     * @return A new Vector3i containing the cell location
     */
    public Vector3i getCellLocation() {
        return new Vector3i(cellLocation);
    }

    /**
     * Updates the valid module options for this cell based on its surroundings.
     */
    public void updateValidOptions() {
        validOptions = ModulesContainer.getValidModulesFromSurroundings(this);
    }

    /**
     * Gets the count of valid module options for this cell.
     * @return The number of valid options, or 0 if none are available
     */
    public int getValidOptionCount() {
        if (validOptions == null) {
            updateValidOptions();
        }
        if (validOptions == null) {
            Logger.warn("Valid options were null when trying to get the size for cell at " + cellLocation);
            return 0;
        }
        return validOptions.size();
    }

    /**
     * Gets a map of neighboring cells in each direction.
     * @return Map of Direction to GridCell for each neighbor
     */
    public Map<Direction, GridCell> getOrientedNeighbors() {
        Map<Direction, GridCell> neighbors = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values()) {
            Vector3i offset = SpatialGrid.getDirectionOffset(direction);
            Vector3i neighborPos = new Vector3i(cellLocation).add(offset);
            neighbors.put(direction, cellMap.get(neighborPos));
        }

        return neighbors;
    }

    /**
     * Gets the real world location of this cell's origin point.
     * @return Location object representing the cell's origin in the world
     */
    public Location getRealLocation() {
        Vector3i worldCoord = grid.gridToWorld(cellLocation);
        return new Location(world, worldCoord.x, worldCoord.y, worldCoord.z);
    }

    /**
     * Creates debug text displays showing cell information.
     */
    public void showDebugTextDisplays() {
        if (modulesContainer == null) {
            return;
        }

        clearDebugDisplays();
        textDisplays = new ArrayList<>();

        Color color = generateRandomColor();
        Location centerLocation = getCenterLocation();

        displayMainInfo(centerLocation, color);
        displayBorderInfo(centerLocation, color);
    }

    private Color generateRandomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private Location getCenterLocation() {
//        Vector3i centerOffset = new Vector3i(grid.getChunkSize() / 2);
        Vector3i worldPos = grid.gridToWorld(cellLocation);
        return new Location(world, worldPos.x, worldPos.y, worldPos.z);
    }

    private void displayMainInfo(Location centerLocation, Color color) {
        spawnDebugText(centerLocation, modulesContainer.getClipboardFilename(), color, 1);
        spawnDebugText(centerLocation.clone().add(0, -0.25, 0),
                "Rotation: " + modulesContainer.getRotation(), color, 1);
    }

    private void displayBorderInfo(Location centerLocation, Color color) {
        for (Map.Entry<Direction, List<ModulesContainer.NeighborTag>> entry :
                modulesContainer.getBorderTags().entrySet()) {
            Vector3i offset = getDirectionOffset(entry.getKey(), 5);
            Location tagLocation = centerLocation.clone().add(offset.x, offset.y, offset.z);

            spawnDebugText(tagLocation, entry.getKey().name(), color, 1);

            displayNeighborTags(tagLocation, entry.getValue(), color);
        }
    }

    private Vector3i getDirectionOffset(Direction direction, int distance) {
        return switch (direction) {
            case UP -> new Vector3i(0, distance, 0);
            case DOWN -> new Vector3i(0, -distance, 0);
            case EAST -> new Vector3i(distance, 0, 0);
            case WEST -> new Vector3i(-distance, 0, 0);
            case NORTH -> new Vector3i(0, 0, -distance);
            case SOUTH -> new Vector3i(0, 0, distance);
        };
    }

    private void displayNeighborTags(Location baseLocation, List<ModulesContainer.NeighborTag> tags, Color color) {
        for (int i = 0; i < tags.size(); i++) {
            Location tagLocation = baseLocation.clone().add(0, -(i + 1) / 4.0, 0);
            spawnDebugText(tagLocation, tags.get(i).getTag(), color, 1);
        }
    }

    private void spawnDebugText(Location location, String text, Color color, float scale) {
        new BukkitRunnable() {
            @Override
            public void run() {
                TextDisplay textDisplay = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
                configureTextDisplay(textDisplay, text, color, scale);
                textDisplays.add(textDisplay);
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    private void configureTextDisplay(TextDisplay display, String text, Color color, float scale) {
        display.setBillboard(Display.Billboard.CENTER);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()
        ));
        display.setBackgroundColor(color);
        display.setTextOpacity((byte) 1);
        display.setSeeThrough(true);
        display.setText(text);
        display.setViewRange(0.1f);
    }

    /**
     * Process a module being pasted into this cell.
     * @param modulesContainer The module container to paste
     */
    public void processPaste(ModulesContainer modulesContainer) {
        this.modulesContainer = modulesContainer;

        // Update neighbor options after paste
        getOrientedNeighbors().values().stream()
                .filter(Objects::nonNull)
                .forEach(GridCell::updateValidOptions);
    }

    /**
     * Updates the count of generated neighbors.
     */
    public void updateNeighborCount() {
        generatedNeighborCount = (int) getOrientedNeighbors().values().stream()
                .filter(Objects::nonNull)
                .count();
    }

    /**
     * Checks if this cell has been generated.
     * @return true if the cell has a module container
     */
    public boolean isGenerated() {
        return modulesContainer != null;
    }

    /**
     * Resets this cell's data.
     * @param showGenerationForShowcase Whether to clear blocks for showcase mode
     */
    public void resetData(boolean showGenerationForShowcase) {
        if (cellLocation.equals(new Vector3i())) return;
        if (showGenerationForShowcase) {
            clearBlocks();
        }
        this.modulesContainer = null;
        this.validOptions = null;
    }

    private void clearBlocks() {
        int size = grid.getChunkSize();
        Vector3i worldPos = grid.gridToWorld(cellLocation);

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    Location blockLocation = new Location(world,
                            worldPos.x + x - (size / 2),
                            worldPos.y + y - (size / 2),
                            worldPos.z + z - (size / 2));
                    blockLocation.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Performs a hard reset of this cell and its neighbors.
     * @param spatialGrid The spatial grid reference
     * @param slowGenerationForShowcase Whether to show generation progress
     * @param radius The radius of cells to reset
     * @return Number of cells reset
     */
    public int hardReset(SpatialGrid spatialGrid, boolean slowGenerationForShowcase, int radius) {
        Set<GridCell> visited = new HashSet<>();
        Set<GridCell> resetCells = new HashSet<>();

        int resetCount = hardResetRecursive(slowGenerationForShowcase, radius, visited, resetCells);

        resetCells.forEach(spatialGrid::updateCellPriority);
        return resetCount;
    }

    private int hardResetRecursive(boolean showGenerationForShowcase, int radius,
                                   Set<GridCell> visited, Set<GridCell> resetCells) {

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

    /**
     * Clears generation data for this cell.
     */
    public void clearGridGenerationData() {
        clearDebugDisplays();
        validOptions = null;
    }

    private void clearDebugDisplays() {
        if (textDisplays != null) {
            textDisplays.forEach(Entity::remove);
            textDisplays.clear();
        }
    }
}