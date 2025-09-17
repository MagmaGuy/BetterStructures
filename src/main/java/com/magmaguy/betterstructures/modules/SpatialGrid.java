package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
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

    private Comparator<GridCell> createCellComparator() {
        //This way of comparing things is faster for small gens which are the ones we're currently using, but much slower at a scale due to the way things roll back
//        return Comparator
//                .comparingInt(GridCell::getValidOptionCount)
//                .thenComparingInt(GridCell::getMagnitudeSquared);
        return Comparator
                .comparingInt(GridCell::getMagnitudeSquared)
                .thenComparingInt(GridCell::getValidOptionCount);
    }

    public static Vector3i getDirectionOffset(Direction direction) {
        return new Vector3i(DIRECTION_OFFSETS.get(direction));
    }

    public void enqueueCell(GridCell gridCell) {
//        if (gridCell.getCellLocation().x ==0 && gridCell.getCellLocation().y ==0 && gridCell.getCellLocation().z == 0) Logger.debug("ABOUT TO ENQUEUE ZERO");
        if (gridCell == null || isNothing(gridCell) || isWorldBorder(gridCell)) {
            return;
        }
//        Logger.debug("enqueuing " + gridCell.getCellLocation());
//        if (gridCell.getCellLocation().x ==0 && gridCell.getCellLocation().y ==0 && gridCell.getCellLocation().z == 0) Logger.debug("ENQUEUED ZERO");
        gridCell.updateValidOptions();
        gridCellQueue.add(gridCell);
    }

    public void updateCellPriority(GridCell gridCell) {
        if (gridCell == null || isNothing(gridCell) || isWorldBorder(gridCell)) {
            return;
        }
        gridCellQueue.remove(gridCell);
        gridCell.updateValidOptions();
        gridCellQueue.add(gridCell);
    }

    public void rollbackCell(GridCell gridCell){
        if (gridCell.isStartModule()) {
            Logger.warn("Tried to rollback start module! This shouldn't happen.");
            return;
        }

        gridCellQueue.remove(gridCell);

        for (GridCell value : gridCell.getOrientedNeighbors().values()) {
            if (value == null) continue;
            value.resetData();
            updateCellPriority(value);
        }

        gridCell.resetData();
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

    /**
     * Initializes neighboring cells for a given cell, respecting "nothing" boundaries.
     *
     * @param gridCell The cell to initialize neighbors for
     */
    public void initializeCellNeighbors(GridCell gridCell, WaveFunctionCollapseGenerator waveFunctionCollapseGenerator) {
        if (gridCell == null || isNothing(gridCell)) {
//            Logger.debug("skipping because it's nothing");
            return;
        }

        for (Direction direction : Direction.values()) {
            Vector3i offset = DIRECTION_OFFSETS.get(direction);
            Vector3i neighborLocation = new Vector3i(gridCell.getCellLocation()).add(offset);

            // Skip if outside bounds
            if (!isWithinBounds(neighborLocation)) {
                continue;
            }

            // Check if neighbor already exists
            GridCell existingNeighbor = cellMap.get(neighborLocation);
            if (existingNeighbor == null) {
                // Create new neighbor only if the current cell isn't "nothing"
                GridCell neighborCell = new GridCell(neighborLocation, gridCell.getWorld(), this, cellMap, waveFunctionCollapseGenerator);
                cellMap.put(neighborLocation, neighborCell);
                enqueueCell(neighborCell);
            }
        }
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
        }

        //Skip if it has no generated neighbors, it's some random island in the sky that got detached
        boolean hasGeneratedNeighbors = false;
        for (GridCell value : next.getOrientedNeighbors().values()) {
            if (value !=null && value.isGenerated()) {
                hasGeneratedNeighbors = true;
                break;
            }
        }
        if (!hasGeneratedNeighbors)
            next = gridCellQueue.poll();

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