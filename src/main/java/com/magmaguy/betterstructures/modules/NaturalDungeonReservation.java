package com.magmaguy.betterstructures.modules;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Reserves the complete chunk footprint of a naturally generated modular structure.
 *
 * <p>The chunk that triggered generation is not the structure's boundary. A modular dungeon can
 * extend several chunks in every direction, so checking only {@code ChunkLoadEvent#isNewChunk()}
 * on the origin can overwrite an older base at the edge of the paste. This class keeps the
 * geometry and state decision independent of Bukkit so the destructive case remains covered by a
 * deterministic unit test.</p>
 */
public final class NaturalDungeonReservation {
    private final BlockBounds blockBounds;
    private final Set<ChunkCoordinate> initiallyUngeneratedChunks;

    private NaturalDungeonReservation(
            BlockBounds blockBounds,
            Set<ChunkCoordinate> initiallyUngeneratedChunks) {
        this.blockBounds = blockBounds;
        this.initiallyUngeneratedChunks = Set.copyOf(initiallyUngeneratedChunks);
    }

    public static Optional<NaturalDungeonReservation> tryCreate(
            int originBlockX,
            int originBlockZ,
            int latticeRadius,
            int moduleSize,
            int spawnProtectionRadius,
            ChunkProbe isGenerated,
            ChunkProbe wasObservedNew) {
        if (latticeRadius < 1) throw new IllegalArgumentException("latticeRadius must be positive");
        if (moduleSize < 1) throw new IllegalArgumentException("moduleSize must be positive");

        BlockBounds bounds = blockBounds(originBlockX, originBlockZ, latticeRadius, moduleSize);
        if (bounds.intersectsSpawnProtection(spawnProtectionRadius)) return Optional.empty();

        Set<ChunkCoordinate> initiallyUngenerated = new HashSet<>();
        for (int x = bounds.minChunkX(); x <= bounds.maxChunkX(); x++) {
            for (int z = bounds.minChunkZ(); z <= bounds.maxChunkZ(); z++) {
                ChunkCoordinate coordinate = new ChunkCoordinate(x, z);
                if (!isGenerated.test(coordinate)) {
                    initiallyUngenerated.add(coordinate);
                    continue;
                }
                if (!wasObservedNew.test(coordinate)) return Optional.empty();
            }
        }

        return Optional.of(new NaturalDungeonReservation(bounds, initiallyUngenerated));
    }

    public boolean remainsSafe(ChunkProbe isGenerated) {
        for (ChunkCoordinate coordinate : initiallyUngeneratedChunks)
            if (isGenerated.test(coordinate)) return false;
        return true;
    }

    public BlockBounds blockBounds() {
        return blockBounds;
    }

    static BlockBounds blockBounds(
            int originBlockX,
            int originBlockZ,
            int latticeRadius,
            int moduleSize) {
        // WFCLattice maps each cell with a -moduleSize/2 offset. Only the cells inside the
        // +/-radius boundary can contain modules, so the populated coordinates are
        // [1-radius, radius-1] on each horizontal axis.
        int localMinimum = (1 - latticeRadius) * moduleSize - moduleSize / 2;
        int localMaximum = (latticeRadius - 1) * moduleSize - moduleSize / 2 + moduleSize - 1;
        return new BlockBounds(
                Math.addExact(originBlockX, localMinimum),
                Math.addExact(originBlockZ, localMinimum),
                Math.addExact(originBlockX, localMaximum),
                Math.addExact(originBlockZ, localMaximum));
    }

    @FunctionalInterface
    public interface ChunkProbe {
        boolean test(ChunkCoordinate coordinate);
    }

    public record ChunkCoordinate(int x, int z) {
    }

    public record BlockBounds(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        public BlockBounds {
            if (minBlockX > maxBlockX || minBlockZ > maxBlockZ)
                throw new IllegalArgumentException("minimum bounds must not exceed maximum bounds");
        }

        public int minChunkX() {
            return Math.floorDiv(minBlockX, 16);
        }

        public int minChunkZ() {
            return Math.floorDiv(minBlockZ, 16);
        }

        public int maxChunkX() {
            return Math.floorDiv(maxBlockX, 16);
        }

        public int maxChunkZ() {
            return Math.floorDiv(maxBlockZ, 16);
        }

        boolean intersectsSpawnProtection(int radius) {
            if (radius <= 0) return false;
            long closestX = Math.max(minBlockX, Math.min(0, maxBlockX));
            long closestZ = Math.max(minBlockZ, Math.min(0, maxBlockZ));
            return closestX * closestX + closestZ * closestZ
                    < (long) radius * radius;
        }
    }
}
