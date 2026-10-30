package com.magmaguy.betterstructures.buildingfitter.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps natural terrain scans from loading chunks. Reading a block in a chunk that is not loaded
 * makes the server thread load or generate it on the spot, and Paper can leave a chunk stuck
 * mid-generation (PaperMC/Paper#11174), which turns that wait into a permanent freeze. Inside
 * {@link #collectMisses}, a read of an unloaded chunk is refused and recorded instead; the caller
 * loads those chunks without blocking and scans again. Outside it, as for commands, every read is
 * allowed. Server thread only.
 */
public final class TerrainReads {
    private static Set<Long> misses;
    private static World lastWorld;
    private static long lastChunk;

    private TerrainReads() {
    }

    /** Runs a scan with reads restricted to loaded chunks and returns the chunks it could not read. */
    public static Set<Long> collectMisses(Runnable scan) {
        if (misses != null) throw new IllegalStateException("Terrain scans do not nest");
        misses = new HashSet<>();
        lastWorld = null;
        try {
            scan.run();
            return misses;
        } finally {
            misses = null;
            lastWorld = null;
        }
    }

    /** Whether the chunk at this location can be read without loading it, recording it if not. */
    public static boolean readable(Location location) {
        if (misses == null) return true;
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        long chunk = key(chunkX, chunkZ);
        // Scans read many blocks per column, so most reads repeat the last loaded chunk.
        if (world == lastWorld && chunk == lastChunk) return true;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            misses.add(chunk);
            return false;
        }
        lastWorld = world;
        lastChunk = chunk;
        return true;
    }

    /** Whether the running scan has found a chunk it could not read, so its result must not be used. */
    public static boolean incomplete() {
        return misses != null && !misses.isEmpty();
    }

    public static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int chunkX(long key) {
        return (int) (key >> 32);
    }

    public static int chunkZ(long key) {
        return (int) key;
    }
}
