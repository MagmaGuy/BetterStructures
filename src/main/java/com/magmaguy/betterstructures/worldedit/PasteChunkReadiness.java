package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * One requested chunk at a time per active paste; all world mutation stays on the server thread.
 * A requested chunk is claimed inside its load completion: Paper removes its own load ticket right after
 * completing the request, so a chunk claimed on a later tick is usually unloaded again when nobody is near.
 * <p>
 * Claimed chunks stay held until the paste closes, up to {@link #MAX_HELD_CHUNKS}, then the least recently
 * used goes. A paste walks its blocks layer by layer and returns to the same chunks every layer; releasing
 * each one as it moved on made it unload and load again, tens of thousands of times for one large build.
 */
public final class PasteChunkReadiness implements AutoCloseable {
    // Loads take milliseconds to seconds. A paste still waiting after this fails instead of blocking the queue.
    private static final long LOAD_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60);
    // Covers every chunk a 1024-block-deep layer crosses; the paste cursor walks z inside each layer.
    static final int MAX_HELD_CHUNKS = 64;
    private final World world;
    private final Method asyncLoad;
    private CompletableFuture<Void> pending;
    private final LinkedHashMap<Long, Chunk> held = new LinkedHashMap<>(16, 0.75f, true);
    private long waitingSince = -1;
    private boolean closed;

    public PasteChunkReadiness(World world) {
        this.world = world;
        Method method;
        try { method = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class); }
        catch (NoSuchMethodException spigot) { method = null; }
        asyncLoad = method;
    }

    public boolean ready(Location location) {
        if (closed) return false;
        int x = location.getBlockX() >> 4, z = location.getBlockZ() >> 4;
        if (isHeld(x, z)) return true;
        if (Bukkit.getWorld(world.getUID()) != world)
            throw new IllegalStateException("Paste world was unloaded while loading a chunk");
        long now = System.nanoTime();
        if (waitingSince < 0) waitingSince = now;
        else if (now - waitingSince > LOAD_TIMEOUT_NANOS)
            throw new IllegalStateException("Timed out waiting for chunk " + x + ", " + z + " in " + world.getName()
                    + " to load for pasting");
        if (pending != null) {
            if (!pending.isDone()) return false;
            CompletableFuture<Void> completed = pending;
            pending = null;
            completed.join(); // isDone above: never waits; rethrows a failed load.
            if (isHeld(x, z)) return true;
        }
        if (world.isChunkLoaded(x, z)) {
            hold(world.getChunkAt(x, z));
            return isHeld(x, z);
        }
        CompletableFuture<Chunk> request;
        if (asyncLoad != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Chunk> paperRequest = (CompletableFuture<Chunk>) asyncLoad.invoke(world, x, z, true);
                request = paperRequest;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not request a Paper chunk for pasting", failure);
            }
        } else {
            // Spigot exposes synchronous loading only. Isolate one load on a later tick;
            // the load itself cannot honor a time budget on that platform.
            CompletableFuture<Chunk> spigotRequest = new CompletableFuture<>();
            request = spigotRequest;
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> {
                if (closed || Bukkit.getWorld(world.getUID()) != world) { spigotRequest.cancel(false); return; }
                try { spigotRequest.complete(world.getChunkAt(x, z)); }
                catch (Throwable failure) { spigotRequest.completeExceptionally(failure); }
            });
        }
        // Runs on the server thread while the loader still holds the chunk.
        pending = request.thenAccept(chunk -> {
            if (!closed && chunk != null && chunk.isLoaded() && Bukkit.getWorld(world.getUID()) == world) hold(chunk);
        });
        return false;
    }

    private boolean isHeld(int x, int z) {
        if (held.get(key(x, z)) == null) return false;
        waitingSince = -1;
        return true;
    }

    private void hold(Chunk chunk) {
        long key = key(chunk.getX(), chunk.getZ());
        if (held.get(key) != null) return;
        held.put(key, chunk);
        PluginChunkHolds.hold(chunk);
        if (held.size() <= MAX_HELD_CHUNKS) return;
        Iterator<Chunk> eldest = held.values().iterator();
        Chunk released = eldest.next();
        eldest.remove();
        PluginChunkHolds.release(released);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @Override public void close() {
        closed = true;
        for (Chunk chunk : held.values()) PluginChunkHolds.release(chunk);
        held.clear();
        pending = null;
    }
}
