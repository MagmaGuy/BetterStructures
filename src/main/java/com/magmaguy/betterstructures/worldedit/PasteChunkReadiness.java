package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/** One requested chunk and one owned ticket per active paste; all world mutation stays on the server thread. */
public final class PasteChunkReadiness implements AutoCloseable {
    private final World world;
    private final Method asyncLoad;
    private CompletableFuture<Chunk> pending;
    private Chunk held;
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
        if (held != null && held.getX() == x && held.getZ() == z) return true;
        if (pending != null) {
            if (!pending.isDone()) return false;
            Chunk loaded = pending.join(); // isDone above: never waits on the server thread.
            pending = null;
            if (closed || Bukkit.getWorld(world.getUID()) != world)
                throw new IllegalStateException("Paste world was unloaded while loading a chunk");
            if (loaded == null || !loaded.isLoaded()) return false;
            hold(loaded);
            return loaded.getX() == x && loaded.getZ() == z;
        }
        if (world.isChunkLoaded(x, z)) {
            hold(world.getChunkAt(x, z));
            return true;
        }
        release();
        if (asyncLoad != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Chunk> request = (CompletableFuture<Chunk>) asyncLoad.invoke(world, x, z, true);
                pending = request;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not request a Paper chunk for pasting", failure);
            }
        } else {
            // Spigot exposes synchronous loading only. Isolate one load on a later tick;
            // the load itself cannot honor a time budget on that platform.
            CompletableFuture<Chunk> request = new CompletableFuture<>();
            pending = request;
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> {
                if (closed || Bukkit.getWorld(world.getUID()) != world) { request.cancel(false); return; }
                try { request.complete(world.getChunkAt(x, z)); }
                catch (Throwable failure) { request.completeExceptionally(failure); }
            });
        }
        return false;
    }

    private void hold(Chunk chunk) {
        release();
        chunk.addPluginChunkTicket(MetadataHandler.PLUGIN);
        held = chunk;
    }

    private void release() {
        if (held != null) {
            held.removePluginChunkTicket(MetadataHandler.PLUGIN);
            held = null;
        }
    }

    @Override public void close() {
        closed = true;
        release();
        pending = null;
    }
}
