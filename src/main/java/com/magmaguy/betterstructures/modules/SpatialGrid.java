package com.magmaguy.betterstructures.modules;

import lombok.Getter;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.*;

public class SpatialGrid {
    public static final int minYLevel = -4;
    public static final int maxYLevel = 20;
    @Getter
    private final int gridRadius;
    @Getter
    private final Map<Vector3i, GridCell> cellMap = new HashMap<>();
    @Getter
    private final PriorityQueue<GridCell> gridCellQueue;

    public SpatialGrid(int gridRadius) {
        this.gridRadius = gridRadius;
        Comparator<GridCell> cellComparator = Comparator
                .comparingInt((GridCell cd) -> {
                    Vector3i loc = cd.getChunkLocation();
                    return loc.x * loc.x + loc.y * loc.y + loc.z * loc.z;
                })
                .thenComparingInt(GridCell::getValidOptionCount);
        this.gridCellQueue = new PriorityQueue<>(cellComparator);
    }

    public void enqueueCell(GridCell gridCell) {
        gridCell.updateValidOptions(false); // Calculate valid options
        gridCellQueue.add(gridCell);
    }

    public void updateCellPriority(GridCell gridCell) {
        if (gridCell == null) return;
        // Remove and re-add the cell to update its position in the priority queue
        gridCellQueue.remove(gridCell);
        gridCell.updateValidOptions(true);
        if (gridCell.getValidOptionCount() > 0) {
            gridCellQueue.add(gridCell);
        }
    }

    public void initializeCellNeighbors(GridCell gridCell, World world) {
        for (Direction direction : Direction.values()) {
            Vector3i neighborLocation = gridCell.getChunkLocation().add(getOffset(direction));

            if (isWithinBounds(neighborLocation)) {
                GridCell neighborCell = cellMap.get(neighborLocation);

                if (neighborCell == null) {
                    neighborCell = new GridCell(neighborLocation, world, cellMap);
                    cellMap.put(neighborLocation, neighborCell);
                    enqueueCell(neighborCell);
                }
            }
        }
    }

    private boolean isWithinBounds(Vector3i location) {
        return Math.abs(location.x) <= gridRadius &&
                location.y >= minYLevel &&
                location.y <= maxYLevel &&
                Math.abs(location.z) <= gridRadius;
    }

    private Vector3i getOffset(Direction direction) {
        return switch (direction) {
            case NORTH -> new Vector3i(0, 0, -1);
            case SOUTH -> new Vector3i(0, 0, 1);
            case EAST -> new Vector3i(1, 0, 0);
            case WEST -> new Vector3i(-1, 0, 0);
            case UP -> new Vector3i(0, 1, 0);
            case DOWN -> new Vector3i(0, -1, 0);
        };
    }

    public GridCell getNextGridCell() {
        return gridCellQueue.poll();
    }
}
