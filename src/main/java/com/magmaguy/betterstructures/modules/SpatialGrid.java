package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.*;

public class SpatialGrid {
    private static final Map<Direction, Vector3i> DIRECTION_OFFSETS = new EnumMap<>(Direction.class);

    @Getter private final int gridRadius;
    @Getter private final int chunkSizeXZ;
    @Getter private final int chunkSizeY;
    @Getter private final int minYLevel;
    @Getter private final int maxYLevel;
    @Getter private final Map<Vector3i, GridCell> cellMap;
    @Getter private final PriorityQueue<GridCell> gridCellQueue;

    static {
        initializeDirectionOffsets();
    }

    private static void initializeDirectionOffsets() {
        DIRECTION_OFFSETS.put(Direction.NORTH, new Vector3i(0, 0, -1));
        DIRECTION_OFFSETS.put(Direction.SOUTH, new Vector3i(0, 0, 1));
        DIRECTION_OFFSETS.put(Direction.EAST, new Vector3i(1, 0, 0));
        DIRECTION_OFFSETS.put(Direction.WEST, new Vector3i(-1, 0, 0));
        DIRECTION_OFFSETS.put(Direction.UP, new Vector3i(0, 1, 0));
        DIRECTION_OFFSETS.put(Direction.DOWN, new Vector3i(0, -1, 0));
    }

    public SpatialGrid(int gridRadius, int chunkSizeXZ, int chunkSizeY, int minYLevel, int maxYLevel) {
        this.gridRadius = gridRadius;
        this.chunkSizeXZ = chunkSizeXZ;
        this.chunkSizeY = chunkSizeY;
        this.minYLevel = minYLevel;
        this.maxYLevel = maxYLevel;
        this.cellMap = new HashMap<>();

        Comparator<GridCell> cellComparator = createCellComparator();
        this.gridCellQueue = new PriorityQueue<>(cellComparator);
    }

    public void initializeGrid(World world, WaveFunctionCollapseGenerator waveFunctionCollapseGenerator){
        Logger.debug("Initializing grid with radius " + gridRadius);

        for (int x = -gridRadius; x <= gridRadius; x++) {
            for (int z = -gridRadius; z <= gridRadius; z++) {
                for (int y = minYLevel-1; y <= maxYLevel+1; y++) {
                    Vector3i gridCoord = new Vector3i(x, y, z);
                    GridCell gridCell = new GridCell(gridCoord, world, this, cellMap, waveFunctionCollapseGenerator);
                    cellMap.put(gridCoord, gridCell);
                }
            }
        }

        Logger.debug("Initializing neighbors for " + cellMap.size() + " cells");
        for (GridCell gridCell : cellMap.values()) {
            gridCell.initializeNeighbors();
        }
    }

    private Comparator<GridCell> createCellComparator() {
        //This way of comparing things is faster for small gens which are the ones we're currently using, but much slower at a scale due to the way things roll back
        return Comparator
                .comparingInt(GridCell::getValidOptionCount)
                .thenComparingInt(GridCell::getMagnitudeSquared);
//        return Comparator
//                .comparingInt(GridCell::getMagnitudeSquared)
//                .thenComparingInt(GridCell::getValidOptionCount);
    }

    public static Vector3i getDirectionOffset(Direction direction) {
        return new Vector3i(DIRECTION_OFFSETS.get(direction));
    }

    public void updateCellPriority(GridCell gridCell) {
        if (gridCell == null || gridCell.isGenerated() ||gridCell.isBorder()) {
            return;
        }

        Logger.debug("updating priority for cell at " + gridCell.getCellLocation() + " with " + gridCell.getValidOptionCount() + " valid options");

        gridCellQueue.remove(gridCell);
        boolean hasGeneratedNotNothingNeighbors = false;
        for (GridCell value : gridCell.getOrientedNeighbors().values())
            if (value != null&& value.isGenerated() && !value.isNothing())
                hasGeneratedNotNothingNeighbors = true;
        if (!hasGeneratedNotNothingNeighbors) return;
        gridCell.updateValidOptions();
        gridCellQueue.add(gridCell);
    }

    public void rollbackCell(GridCell gridCell){
        gridCell.resetData();
        for (GridCell value : gridCell.getOrientedNeighbors().values()) {
            if (value != null) value.resetData();
        }
        for (GridCell value : gridCell.getOrientedNeighbors().values()) {
            if (value != null) updateCellPriority(value);
        }
        updateCellPriority(gridCell);
    }

    private boolean isNothing(GridCell cell) {
        return cell.getModulesContainer() != null &&
                cell.getModulesContainer().isNothing();
    }

    private boolean isWorldBorder(GridCell cell){
        return cell.getModulesContainer() != null &&
                cell.getModulesContainer().getClipboardFilename().equals("world_border");
    }

    public void clearGridGenerationData() {
        cellMap.values().forEach(GridCell::clearGridGenerationData);
        gridCellQueue.clear();
    }

    public void clearAllData() {
        cellMap.clear();
        gridCellQueue.clear();
    }


    public boolean isWithinBounds(Vector3i location) {
        return Math.abs(location.x) <= gridRadius &&
                location.y >= minYLevel &&
                location.y <= maxYLevel &&
                Math.abs(location.z) <= gridRadius;
    }

    public boolean isBorder(Vector3i location) {
        return location.x == -gridRadius
                || location.x == gridRadius
                || location.z == -gridRadius
                || location.z == gridRadius;
    }

    public GridCell getNextGridCell() {
        GridCell next = gridCellQueue.poll();
        // Skip "nothing" cells in the queue
        while (next != null && isNothing(next)) {
            next = gridCellQueue.poll();

            if (next == null) {
                return null;
            }

            for (GridCell value : next.getOrientedNeighbors().values()) {
                if (value !=null && value.isGenerated()) {
                    next = null;
                    break;
                }
            }
        }

        return next;
    }

    public Vector3i worldToGrid(Vector3i worldCoord) {
        return new Vector3i(
                Math.floorDiv(worldCoord.x, chunkSizeXZ),
                Math.floorDiv(worldCoord.y, chunkSizeY),
                Math.floorDiv(worldCoord.z, chunkSizeXZ)
        );
    }

    public Vector3i gridToWorld(Vector3i gridCoord) {
        return new Vector3i(
                gridCoord.x * chunkSizeXZ + (-chunkSizeXZ / 2),
                gridCoord.y * chunkSizeY + (chunkSizeY / 2),
                gridCoord.z * chunkSizeXZ + (-chunkSizeXZ / 2)
        );
    }
}