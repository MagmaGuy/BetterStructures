package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WFCNode {
    private final Vector3i nodePosition;
    private final WFCLattice lattice;
    @Getter
    private final World world;
    private final Map<Vector3i, WFCNode> nodeMap;
    @Getter
    private final int magnitudeSquared;
    @Getter
    private final WFCGenerator wfcGenerator;
    @Getter
    private ModulesContainer modulesContainer;
    @Getter
    private HashSet<ModulesContainer> possibleStates = null;
    private List<TextDisplay> textDisplays;
    private Map<Direction, WFCNode> adjacentNodes = new EnumMap<>(Direction.class);

    /**
     * Creates a new WFCNode.
     *
     * @param nodePosition The lattice coordinates of this node
     * @param world        The world this node belongs to
     * @param lattice      The WFC lattice this node belongs to
     * @param nodeMap      The global node map reference
     */
    public WFCNode(Vector3i nodePosition, World world, WFCLattice lattice, Map<Vector3i, WFCNode> nodeMap, WFCGenerator wfcGenerator) {
        this.nodePosition = new Vector3i(nodePosition);  // Defensive copy
        this.world = world;
        this.lattice = lattice;
        this.nodeMap = nodeMap;
        this.magnitudeSquared = (int) nodePosition.lengthSquared();
        this.wfcGenerator = wfcGenerator;
        if (wfcGenerator.getModuleGeneratorsConfigFields().isDebug()) {
            if (isBoundary()) debugPaste(Material.PURPLE_STAINED_GLASS);
            else debugPaste(Material.RED_STAINED_GLASS);
        }
        if (isBoundary()) modulesContainer = ModulesContainer.nothingContainer;
    }

    public void initializeNeighbors() {
        for (Direction direction : Direction.values()) {
            Vector3i offset = WFCLattice.getDirectionOffset(direction);
            Vector3i neighborPos = new Vector3i(nodePosition).add(offset);
            adjacentNodes.put(direction, nodeMap.get(neighborPos));
        }
    }

    public void setModulesContainer(ModulesContainer modulesContainer) {
        this.modulesContainer = modulesContainer;
        if (wfcGenerator.getModuleGeneratorsConfigFields().isDebug()) {
            if (modulesContainer == null) debugPaste(Material.GRAY_STAINED_GLASS);
            else if (modulesContainer.isNothing()) debugPaste(Material.BLUE_STAINED_GLASS);
            else debugPaste(Material.GREEN_STAINED_GLASS);
        }
    }

    public boolean isBoundary() {
        return Math.abs(nodePosition.x) == lattice.getLatticeRadius() || Math.abs(nodePosition.z) == lattice.getLatticeRadius() || nodePosition.y < lattice.getMinYLevel() || nodePosition.y > lattice.getMaxYLevel();
    }

    /**
     * Gets a safe copy of the cell location.
     *
     * @return A new Vector3i containing the cell location
     */
    public Vector3i getCellLocation() {
        return new Vector3i(nodePosition);
    }

    /**
     * Updates the possible states for this node based on its adjacent nodes.
     */
    public void updatePossibleStates() {
        possibleStates = ModulesContainer.getValidModulesFromSurroundings(this);
        showDebugTextDisplays();
    }

    /**
     * Gets the count of valid module options for this cell.
     *
     * @return The number of valid options, or 0 if none are available
     */
    public int getValidOptionCount() {
        if (possibleStates == null) {
            updatePossibleStates();
        }
        if (possibleStates == null) {
            Logger.warn("Valid options were null when trying to get the size for cell at " + nodePosition);
            return 0;
        }
        return possibleStates.size();
    }

    /**
     * Gets a map of neighboring cells in each direction.
     *
     * @return Map of Direction to WFCNode for each neighbor
     */
    public Map<Direction, WFCNode> getOrientedNeighbors() {
        return adjacentNodes;
    }

    /**
     * Gets the possible states for this node.
     * 
     * @return Set of possible module states for this node
     */
    public HashSet<ModulesContainer> getValidOptions() {
        if (possibleStates == null) {
            updatePossibleStates();
        }
        return possibleStates;
    }

    /**
     * Gets the real world location of this cell's origin point.
     *
     * @return Location object representing the cell's origin in the world
     */
    public Location getRealLocation(Location startLocation) {
        Vector3i worldCoord;
        if (startLocation != null)
            worldCoord = lattice.latticeToWorld(nodePosition).add(startLocation.getBlockX(), startLocation.getBlockY(), startLocation.getBlockZ());
        else
            worldCoord = lattice.latticeToWorld(nodePosition);
        return new Location(world, worldCoord.x, worldCoord.y, worldCoord.z);
    }

    /**
     * Creates debug text displays showing cell information.
     */
    public void showDebugTextDisplays() {
        if (!wfcGenerator.getModuleGeneratorsConfigFields().isDebug()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (textDisplays != null && !textDisplays.isEmpty())        clearDebugDisplays();
                textDisplays = new ArrayList<>();

                if (modulesContainer == null) {
                    if (possibleStates == null) {
                        spawnDebugText(getRealCenterLocation(), "Uninitialized", Color.RED, 1);
                        return;
                    } else {
                        spawnDebugText(getRealCenterLocation(), "Uninitialized", Color.GREEN, 1);
                        spawnDebugText(getRealCenterLocation(), "Options count: " + possibleStates.size(), Color.GREEN, 1);
                        return;
                    }
                }

                Color color = generateRandomColor();
                Location centerLocation = getRealCenterLocation();

                displayMainInfo(centerLocation, color);
                displayBorderInfo(centerLocation, color);
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    private Color generateRandomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private Location getLocalCenterLocation() {
        double y = lattice.getNodeSizeY() / 2d;
        if (modulesContainer != null && modulesContainer.getClipboard() != null)
            y = modulesContainer.getClipboard().getDimensions().y() / 2d;
        Vector3i worldPos = lattice.latticeToWorld(nodePosition).add((int) (lattice.getNodeSizeXZ() / 2d), (int) y, (int) (lattice.getNodeSizeXZ() / 2d));
        return new Location(world, worldPos.x, worldPos.y, worldPos.z);
    }

    public Location getRealCenterLocation() {
        return getLocalCenterLocation().add(wfcGenerator.getStartLocation());
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
                Location adjustedLocation = location.clone().subtract(new Vector(0,textDisplays.size()/2d,0));
                TextDisplay textDisplay = (TextDisplay) world.spawnEntity(adjustedLocation, EntityType.TEXT_DISPLAY);
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
        display.setSeeThrough(true);
        display.setText(text);
        display.setViewRange(1);
    }

    /**
     * Checks if this cell has been generated.
     *
     * @return true if the cell has a module container
     */
    public boolean isCollapsed() {
        return modulesContainer != null;
    }

    public boolean isNothing(){
        return modulesContainer != null && modulesContainer.isNothing();
    }

    /**
     * Resets this cell's data.
     *
     */
    public void resetState() {
        if (isInitialNode() || isBoundary()) return;

        setModulesContainer(null);
        this.possibleStates = null;
        if (wfcGenerator.getModuleGeneratorsConfigFields().isDebug()) {
            debugPaste(Material.GRAY_STAINED_GLASS);
        }
    }


    public boolean isInitialNode() {
        return new Vector3i().equals(nodePosition);
    }

    /**
     * Clears generation data for this cell.
     */
    public void clearGenerationData() {
        clearDebugDisplays();
        possibleStates = null;
    }

    private void clearDebugDisplays() {
        if (textDisplays != null) {
            textDisplays.forEach(Entity::remove);
            textDisplays.clear();
        }
    }

    private void placeMaterial(Location startLocation, Material material) {
        int sizeXZ = wfcGenerator.getModuleGeneratorsConfigFields().getModuleSizeXZ();
        int sizeY = wfcGenerator.getModuleGeneratorsConfigFields().getModuleSizeY();

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
                showDebugTextDisplays();

                Location startLocation = getRealLocation(wfcGenerator.getStartLocation());

                if (modulesContainer == null || modulesContainer.isNothing()) {
                    placeMaterial(startLocation, material);
                    return;
                }

                ModulePasting.paste(modulesContainer.getClipboard(), startLocation, modulesContainer.getRotation());
            }
        }.runTask(MetadataHandler.PLUGIN);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}