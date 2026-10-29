package com.magmaguy.betterstructures.util;

import com.magmaguy.betterstructures.MetadataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bukkit keeps one plugin ticket per chunk for each plugin, so a part of BetterStructures that
 * released its chunk would also drop any other part's hold on it. Holds are counted per chunk and
 * the ticket goes with the last release. Server thread only.
 */
public final class PluginChunkHolds {
    private static final Map<ChunkKey, Hold> holds = new HashMap<>();

    private PluginChunkHolds() {
    }

    public static void hold(Chunk chunk) {
        World world = chunk.getWorld();
        ChunkKey key = new ChunkKey(world.getUID(), chunk.getX(), chunk.getZ());
        Hold hold = holds.get(key);
        // An entry left by an unloaded copy of this world holds nothing: tickets die with their world.
        if (hold != null && hold.world == world) {
            hold.count++;
            return;
        }
        chunk.addPluginChunkTicket(MetadataHandler.PLUGIN);
        holds.put(key, new Hold(world));
    }

    public static void release(Chunk chunk) {
        World world = chunk.getWorld();
        ChunkKey key = new ChunkKey(world.getUID(), chunk.getX(), chunk.getZ());
        Hold hold = holds.get(key);
        if (hold == null || hold.world != world || --hold.count > 0) return;
        holds.remove(key);
        // Tickets die with their world; Bukkit also drops a disabled plugin's tickets.
        if (Bukkit.getWorld(key.worldId()) == world && MetadataHandler.PLUGIN != null)
            chunk.removePluginChunkTicket(MetadataHandler.PLUGIN);
    }

    public static void shutdown() {
        holds.clear();
    }

    private record ChunkKey(UUID worldId, int x, int z) {
    }

    private static final class Hold {
        private final World world;
        private int count = 1;

        private Hold(World world) {
            this.world = world;
        }
    }
}
