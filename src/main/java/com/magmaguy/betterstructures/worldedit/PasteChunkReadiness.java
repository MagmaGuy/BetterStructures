package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * One requested chunk and one chunk hold per active paste; all world mutation stays on the server thread.
 * A requested chunk is claimed inside its load completion: Paper removes its own load ticket right after
 * completing the request, so a chunk claimed on a later tick is usually unloaded again when nobody is near.
 */
public final class PasteChunkReadiness implements AutoCloseable {
    // Loads take milliseconds to seconds. A paste still waiting after this fails instead of blocking the queue.
    private static final long LOAD_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60);
    private final World world;
    private final Method asyncLoad;
    private CompletableFuture<Void> pending;
    private Chunk held;
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
        release();
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
        if (held == null || held.getX() != x || held.getZ() != z) return false;
        waitingSince = -1;
        return true;
    }

    private void hold(Chunk chunk) {
        release();
        held = chunk;
        PluginChunkHolds.hold(chunk);
    }

    private void release() {
        if (held == null) return;
        Chunk chunk = held;
        held = null;
        PluginChunkHolds.release(chunk);
    }

    @Override public void close() {
        closed = true;
        release();
        pending = null;
    }
}
