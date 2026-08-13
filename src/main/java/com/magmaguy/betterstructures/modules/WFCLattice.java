package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.*;

public class WFCLattice {
    
    /**
     * Represents a single collapse decision for backtracking
     */
    private static class CollapseDecision {
        final Vector3i nodePosition;
        final ModulesContainer chosenModule;
        final Set<Vector3i> affectedNeighbors;

        CollapseDecision(Vector3i nodePosition, ModulesContainer chosenModule,
                         Set<Vector3i> affectedNeighbors) {
            this.nodePosition = nodePosition;
            this.chosenModule = chosenModule;
            this.affectedNeighbors = affectedNeighbors;
        }
    }
    private static final Map<Direction, Vector3i> DIRECTION_OFFSETS = new EnumMap<>(Direction.class);

    @Getter private final int latticeRadius;
    @Getter private final int nodeSizeXZ;
    @Getter private final int nodeSizeY;
    @Getter private final int minYLevel;
    @Getter private final int maxYLevel;
    @Getter private final Map<Vector3i, WFCNode> nodeMap;
    private final PriorityQueue<WFCNode> entropyQueue;
    
    // Backtracking system
    private final Deque<CollapseDecision> decisionStack = new ArrayDeque<>();

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

    public WFCLattice(int latticeRadius, int nodeSizeXZ, int nodeSizeY, int minYLevel, int maxYLevel) {
        this.latticeRadius = latticeRadius;
        this.nodeSizeXZ = nodeSizeXZ;
        this.nodeSizeY = nodeSizeY;
        this.minYLevel = minYLevel;
        this.maxYLevel = maxYLevel;
        this.nodeMap = new HashMap<>();

        Comparator<WFCNode> entropyComparator = createEntropyComparator();
        this.entropyQueue = new PriorityQueue<>(entropyComparator);
    }

    public void initializeLattice(World world, WFCGenerator wfcGenerator){
        for (int x = -latticeRadius; x <= latticeRadius; x++) {
            for (int z = -latticeRadius; z <= latticeRadius; z++) {
                for (int y = minYLevel-1; y <= maxYLevel+1; y++) {
                    Vector3i gridCoord = new Vector3i(x, y, z);
                    WFCNode node = new WFCNode(gridCoord, world, this, nodeMap, wfcGenerator);
                    nodeMap.put(gridCoord, node);
                }
            }
        }

        for (WFCNode node : nodeMap.values()) {
            node.initializeNeighbors();
        }
    }

    private Comparator<WFCNode> createEntropyComparator() {
        //This way of comparing things is faster for small gens which are the ones we're currently using, but much slower at a scale due to the way things roll back
        return Comparator
                .comparingInt(WFCNode::getValidOptionCount)
                .thenComparingInt(WFCNode::getMagnitudeSquared);
    }

    public static Vector3i getDirectionOffset(Direction direction) {
        return new Vector3i(DIRECTION_OFFSETS.get(direction));
    }

    public void updateNodeEntropy(WFCNode node) {
        if (node == null || node.isCollapsed() || node.isBoundary()) {
            return;
        }

        entropyQueue.remove(node);
        boolean hasCollapsedNonEmptyNeighbors = false;
        for (WFCNode neighbor : node.getOrientedNeighbors().values())
            if (neighbor != null && neighbor.isCollapsed() && !neighbor.isNothing())
                hasCollapsedNonEmptyNeighbors = true;
        if (!hasCollapsedNonEmptyNeighbors) return;
        node.updatePossibleStates();
        entropyQueue.add(node);
    }

    /**
     * Records a collapse decision for potential backtracking
     */
    public void recordCollapseDecision(WFCNode node, ModulesContainer chosenModule) {
        Set<Vector3i> affectedNeighbors = new HashSet<>();
        for (WFCNode neighbor : node.getOrientedNeighbors().values()) {
            if (neighbor != null) {
                affectedNeighbors.add(neighbor.getCellLocation());
            }
        }
        
        decisionStack.push(new CollapseDecision(
            node.getCellLocation(), 
            chosenModule, 
            affectedNeighbors
        ));
    }
    
    /**
     * Backtracks to the previous decision, undoing the last collapse
     * @return true if backtracking was successful, false if no decisions to backtrack
     */
    public boolean backtrack() {
        if (decisionStack.isEmpty()) {
            return false;
        }
        
        CollapseDecision decision = decisionStack.pop();
        WFCNode node = nodeMap.get(decision.nodePosition);
        
        if (node == null) {
            Logger.warn("Node not found for backtracking at " + decision.nodePosition);
            return false;
        }
        
        // Restore the node's previous state
        node.setModulesContainer(null);
        node.updatePossibleStates();

        // Recalculate entropy for affected neighbors
        for (Vector3i neighborPos : decision.affectedNeighbors) {
            WFCNode neighbor = nodeMap.get(neighborPos);
            if (neighbor != null) {
                updateNodeEntropy(neighbor);
            }
        }

        updateNodeEntropy(node);

        // Remove the failed module from the possible states only AFTER the entropy updates:
        // updateNodeEntropy() calls updatePossibleStates(), which reassigns the option set and
        // would wipe this exclusion, letting the same dead-end module be picked again.
        if (node.getValidOptions() != null) {
            node.getValidOptions().remove(decision.chosenModule);
        }

        return true;
    }
    
    /**
     * Gets the number of decisions that can be backtracked
     */
    public int getBacktrackDepth() {
        return decisionStack.size();
    }
    
    /**
     * Clears all backtracking history
     */
    public void clearBacktrackHistory() {
        decisionStack.clear();
    }

    public void clearAllData() {
        nodeMap.values().forEach(WFCNode::clearGenerationData);
        nodeMap.clear();
        entropyQueue.clear();
        clearBacktrackHistory();
    }


    public boolean isBoundary(Vector3i location) {
        return location.x == -latticeRadius
                || location.x == latticeRadius
                || location.z == -latticeRadius
                || location.z == latticeRadius;
    }

    public WFCNode getLowestEntropyNode() {
        // The queue only ever contains un-collapsed, non-boundary nodes: updateNodeEntropy() is the
        // sole insertion point and it rejects collapsed/boundary nodes, while collapses only happen
        // to nodes already polled off the queue (or the never-queued origin).
        return entropyQueue.poll();
    }

    public Vector3i latticeToWorld(Vector3i latticeCoord) {
        return new Vector3i(
                latticeCoord.x * nodeSizeXZ + (-nodeSizeXZ / 2),
                latticeCoord.y * nodeSizeY + (nodeSizeY / 2),
                latticeCoord.z * nodeSizeXZ + (-nodeSizeXZ / 2)
        );
    }
}
