package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modules.ModulePasting;
import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
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
    @Getter
    private final World world;
    private final Map<Vector3i, GridCell> cellMap;
    @Getter
    private final int magnitudeSquared;
    @Getter
    private final WaveFunctionCollapseGenerator waveFunctionCollapseGenerator;
    @Getter
    private ModulesContainer modulesContainer;
    @Getter
    private HashSet<ModulesContainer> validOptions = null;
    private List<TextDisplay> textDisplays;

    public void setModulesContainer(ModulesContainer modulesContainer){
        this.modulesContainer = modulesContainer;
        if (waveFunctionCollapseGenerator.getModuleGeneratorsConfigFields().isDebug()) {
            if (modulesContainer == null) debugPaste(Material.GRAY_STAINED_GLASS);
            else if (modulesContainer.isNothing()) debugPaste(Material.BLUE_STAINED_GLASS);
            else debugPaste(Material.GREEN_STAINED_GLASS);
        }
        if (modulesContainer == null || modulesContainer.isNothing()) return;
        waveFunctionCollapseGenerator.getSpatialGrid().initializeCellNeighbors(this, waveFunctionCollapseGenerator);
    }

    /**
     * Creates a new GridCell.
     *
     * @param cellLocation The grid coordinates of this cell
     * @param world        The world this cell belongs to
     * @param grid         The spatial grid this cell belongs to
     * @param cellMap      The global cell map reference
     */
    public GridCell(Vector3i cellLocation, World world, SpatialGrid grid, Map<Vector3i, GridCell> cellMap, WaveFunctionCollapseGenerator waveFunctionCollapseGenerator) {
        this.cellLocation = new Vector3i(cellLocation);  // Defensive copy
        this.world = world;
        this.grid = grid;
        this.cellMap = cellMap;
        this.magnitudeSquared = (int) cellLocation.lengthSquared();
        this.waveFunctionCollapseGenerator = waveFunctionCollapseGenerator;
        if (waveFunctionCollapseGenerator.getModuleGeneratorsConfigFields().isDebug()) debugPaste(Material.RED_STAINED_GLASS);
    }

    public boolean isHorizontalEdge() {
        return Math.abs(cellLocation.x) == grid.getGridRadius() || Math.abs(cellLocation.z) == grid.getGridRadius();
    }

    /**
     * Gets a safe copy of the cell location.
     *
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
     *
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
     *
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
     *
     * @return Location object representing the cell's origin in the world
     */
    public Location getRealLocation(Location startLocation) {
        Vector3i worldCoord;
        if (startLocation != null)
            worldCoord = grid.gridToWorld(cellLocation).add(startLocation.getBlockX(), startLocation.getBlockY(), startLocation.getBlockZ());
        else
            worldCoord = grid.gridToWorld(cellLocation);
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
        Location centerLocation = getRealCenterLocation();

        displayMainInfo(centerLocation, color);
        displayBorderInfo(centerLocation, color);
    }

    private Color generateRandomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private Location getLocalCenterLocation() {
        double y = waveFunctionCollapseGenerator.getSpatialGrid().getChunkSizeY() / 2d;
        if (modulesContainer != null && modulesContainer.getClipboard() != null) y = modulesContainer.getClipboard().getDimensions().y() / 2d;
        Vector3i worldPos = grid.gridToWorld(cellLocation).add((int) (waveFunctionCollapseGenerator.getSpatialGrid().getChunkSizeXZ() / 2d), (int) y, (int) (waveFunctionCollapseGenerator.getSpatialGrid().getChunkSizeXZ() / 2d));
        return new Location(world, worldPos.x, worldPos.y, worldPos.z);
    }

    public Location getRealCenterLocation(){
        return getLocalCenterLocation().add(waveFunctionCollapseGenerator.getStartLocation());
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
//        display.setTextOpacity((byte) 1);
        display.setSeeThrough(true);
        display.setText(text);
        display.setViewRange(32);
    }

    /**
     * Process a module being pasted into this cell.
     *
     * @param modulesContainer The module container to paste
     */
    public void processPaste(ModulesContainer modulesContainer) {
        setModulesContainer(modulesContainer);

        // Update neighbor options after paste
        getOrientedNeighbors().values().stream()
                .filter(Objects::nonNull)
                .forEach(GridCell::updateValidOptions);
    }

    /**
     * Checks if this cell has been generated.
     *
     * @return true if the cell has a module container
     */
    public boolean isGenerated() {
        return modulesContainer != null;
    }

    /**
     * Resets this cell's data.
     *
     */
    public void resetData() {
        if (isStartModule()) return;

        setModulesContainer(null);
        this.validOptions = null;
        if (waveFunctionCollapseGenerator.getModuleGeneratorsConfigFields().isDebug()) {
            debugPaste(Material.GRAY_STAINED_GLASS);
        }
    }


    public boolean isStartModule() {
        return new Vector3i().equals(cellLocation);
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

    private void placeMaterial(Location startLocation, Material material) {
        int sizeXZ = waveFunctionCollapseGenerator.getModuleGeneratorsConfigFields().getModuleSizeXZ();
        int sizeY = waveFunctionCollapseGenerator.getModuleGeneratorsConfigFields().getModuleSizeY();

        for (int x = 0; x < sizeXZ; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeXZ; z++) {
                    Location blockLocation = startLocation.clone().add(x, y, z);

                    // Check if block is on an edge (intersection of at least 2 faces)
                    boolean isOnXEdge = (x == 0 || x == sizeXZ - 1);
                    boolean isOnYEdge = (y == 0 || y == sizeY - 1);
                    boolean isOnZEdge = (z == 0 || z == sizeXZ - 1);

                    // Count how many edges this block touches
                    int edgeCount = 0;
                    if (isOnXEdge) edgeCount++;
                    if (isOnYEdge) edgeCount++;
                    if (isOnZEdge) edgeCount++;

                    // Place material only if block is on at least 2 edges (true edge/corner)
                    if (edgeCount >= 2) {
                        blockLocation.getBlock().setType(material);
                    } else {
                        blockLocation.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void debugPaste(Material material) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Location startLocation = getRealLocation(waveFunctionCollapseGenerator.getStartLocation());

                if (modulesContainer == null || modulesContainer.isNothing()) {
                    placeMaterial(startLocation, material);
                    return;
                }

                ModulePasting.paste(modulesContainer.getClipboard(), startLocation, modulesContainer.getRotation());

                showDebugTextDisplays();
                Logger.debug("Pasted " + modulesContainer.getClipboardFilename() + " at " + startLocation + " with rotation " + modulesContainer.getRotation());
            }
        }.runTask(MetadataHandler.PLUGIN);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}