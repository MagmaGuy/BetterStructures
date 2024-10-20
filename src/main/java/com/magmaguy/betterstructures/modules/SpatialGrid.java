package com.magmaguy.betterstructures.modules;

import lombok.Getter;
import org.joml.Vector3i;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class SpatialGrid {
    public static final int MIN_Y_LEVEL = -4;
    public static final int MAX_Y_LEVEL = 20;
    public static final Map<Direction, Vector3i> DIRECTION_OFFSETS = Map.of(
            Direction.NORTH, new Vector3i(0, 0, -1),
            Direction.SOUTH, new Vector3i(0, 0, 1),
            Direction.EAST, new Vector3i(1, 0, 0),
            Direction.WEST, new Vector3i(-1, 0, 0),
            Direction.UP, new Vector3i(0, 1, 0),
            Direction.DOWN, new Vector3i(0, -1, 0)
    );
    @Getter
    private final int gridRadius;
    @Getter
    private final Map<Vector3i, GridCell> cellMap = new HashMap<>();
    @Getter
    private final PriorityQueue<GridCell> gridCellQueue;

    public SpatialGrid(int gridRadius) {
        this.gridRadius = gridRadius;
        Comparator<GridCell> cellComparator = Comparator
                .comparingInt(GridCell::getMagnitudeSquared)
                .thenComparingInt(GridCell::getValidOptionCount);
//        Comparator<GridCell> cellComparator = Comparator
//                .comparingInt(GridCell::getValidOptionCount)
//                .thenComparingInt(GridCell::getMagnitudeSquared);
        this.gridCellQueue = new PriorityQueue<>(cellComparator);
    }

    public void enqueueCell(GridCell gridCell) {
        gridCell.updateValidOptions();
        gridCellQueue.add(gridCell);
    }

    public void updateCellPriority(GridCell gridCell) {
        if (gridCell == null) return;
        gridCellQueue.remove(gridCell);
        gridCell.updateValidOptions();
        gridCellQueue.add(gridCell);
    }

    public void clearGridGenerationData(){
        cellMap.values().forEach(GridCell::clearGridGenerationData);
        gridCellQueue.clear();
    }

    public void clearAllData(){
        cellMap.clear();
    }

    public void initializeCellNeighbors(GridCell gridCell) {
        for (Direction direction : Direction.values()) {
            Vector3i offset = DIRECTION_OFFSETS.get(direction);
            Vector3i neighborLocation = new Vector3i(gridCell.getCellLocation()).add(offset);

            if (isWithinBounds(neighborLocation)) {
                GridCell neighborCell = cellMap.get(neighborLocation);

                if (neighborCell == null) {
                    neighborCell = new GridCell(neighborLocation, gridCell.getWorld(), cellMap);
                    cellMap.put(neighborLocation, neighborCell);
                    enqueueCell(neighborCell);
                }
            }
        }
    }

    private boolean isWithinBounds(Vector3i location) {
        return Math.abs(location.x) <= gridRadius &&
                location.y >= MIN_Y_LEVEL &&
                location.y <= MAX_Y_LEVEL &&
                Math.abs(location.z) <= gridRadius;
    }

    public GridCell getNextGridCell() {
        return gridCellQueue.poll();
    }
}
