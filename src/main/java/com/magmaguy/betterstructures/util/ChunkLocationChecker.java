package com.magmaguy.betterstructures.util;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class ChunkLocationChecker {
    private ChunkLocationChecker() {
    }

    /*
    Checks if a location is in a given chunk
     */
    public static boolean chunkLocationCheck(Location location, Chunk chunk) {
        return chunk.getWorld().equals(location.getWorld()) //location.getWorld() can return null
            && chunk.getX() == location.getBlockX() >> 4 
            && chunk.getZ() == location.getBlockZ() >> 4;
    }

    /*
    Checks if a given location is loaded
     */
    public static boolean locationIsLoaded(Location location) {
        return location != null && location.getWorld() != null &&
            location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
}

