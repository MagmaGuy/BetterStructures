package com.magmaguy.betterstructures.modules;

import java.util.Optional;

/**
 * The block footprint of a naturally generated modular dungeon.
 *
 * <p>The chunk that triggered generation is not the structure's boundary: a modular dungeon
 * extends several chunks in every direction, so spawn protection applies to its whole footprint,
 * and WorldGuard regions are checked against these bounds. Chunk generation state is deliberately
 * not consulted. Vanilla keeps partially generated chunks around every loaded area, and treating
 * them as existing terrain refused nearly every dungeon a player approached.</p>
 */
public final class NaturalDungeonReservation {
    private final BlockBounds blockBounds;

    private NaturalDungeonReservation(BlockBounds blockBounds) {
        this.blockBounds = blockBounds;
    }

    public static Optional<NaturalDungeonReservation> tryCreate(
            int originBlockX,
            int originBlockZ,
            int latticeRadius,
            int moduleSize,
            int spawnProtectionRadius) {
        if (latticeRadius < 1) throw new IllegalArgumentException("latticeRadius must be positive");
        if (moduleSize < 1) throw new IllegalArgumentException("moduleSize must be positive");

        BlockBounds bounds = blockBounds(originBlockX, originBlockZ, latticeRadius, moduleSize);
        if (bounds.intersectsSpawnProtection(spawnProtectionRadius)) return Optional.empty();
        return Optional.of(new NaturalDungeonReservation(bounds));
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
