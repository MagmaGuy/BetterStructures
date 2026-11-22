package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * One chunk request at a time per active paste; all world mutation stays on the server thread.
 * A requested chunk is claimed inside its load completion: Paper removes its own load ticket right after
 * completing the request, so a chunk claimed on a later tick is usually unloaded again when nobody is near.
 * <p>
 * Claimed chunks stay held until the paste closes, up to {@link #MAX_HELD_CHUNKS}, then the least recently
 * used goes. A paste walks its blocks layer by layer and returns to the same chunks every layer; releasing
 * each one as it moved on made it unload and load again, tens of thousands of times for one large build.
 * <p>
 * A block is ready once the eight chunks round its own are loaded too. Writing a block reads its neighbours,
 * and a block-entity write reads two blocks out (the comparator update behind a solid block), so a write
 * beside a chunk border loaded the next chunk on the server thread, inside the write. Paper can leave a
 * chunk stuck partway through generation for good (PaperMC/Paper#11174), and that load then never returned.
 * The surrounding chunks need no hold of their own: a held chunk's plugin ticket keeps every chunk within
 * two of it at full status, so they are only waited for.
 */
public final class PasteChunkReadiness implements AutoCloseable {
    // Loads take milliseconds to seconds. A paste still waiting after this gives up instead of blocking the
    // queue. Not final: the Autotester shortens it to watch a paste give up.
    static long loadTimeoutNanos = TimeUnit.SECONDS.toNanos(60);
    // Covers every chunk a 1024-block-deep layer crosses; the paste cursor walks z inside each layer.
    static final int MAX_HELD_CHUNKS = 64;
    private final World world;
    private final Method asyncLoad;
    private CompletableFuture<Void> pending;
    private final LinkedHashMap<Long, Held> held = new LinkedHashMap<>(16, 0.75f, true);
    private long waitingSince = -1;
    private int surveyed;
    private boolean closed;

    /** The chunk a paste gave up waiting for. */
    public static final class LoadTimeout extends IllegalStateException {
        private LoadTimeout(int x, int z, World world, long nanos) {
            super("chunk " + x + ", " + z + " in " + world.getName() + " did not finish loading within "
                    + TimeUnit.NANOSECONDS.toSeconds(nanos) + " seconds");
        }
    }

    private static final class Held {
        private final Chunk chunk;
        // Set once the eight chunks round this one were seen loaded; this chunk's ticket keeps them so.
        private boolean surrounded;

        private Held(Chunk chunk) {
            this.chunk = chunk;
        }
    }

    public PasteChunkReadiness(World world) {
        this.world = world;
        Method method;
        try { method = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class); }
        catch (NoSuchMethodException spigot) { method = null; }
        asyncLoad = method;
    }

    public boolean ready(Location location) {
        return ready(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Readies every chunk under a block rectangle in turn, each with the chunks round it, and returns true
     * once each has been ready. Call it again until then. A paste asks before its first write, so a chunk
     * that never loads stops it with nothing placed. One rectangle per instance.
     */
    public boolean readyAll(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int minX = minBlockX >> 4, minZ = minBlockZ >> 4;
        int width = (maxBlockX >> 4) - minX + 1, depth = (maxBlockZ >> 4) - minZ + 1;
        while (surveyed < width * depth) {
            if (!ready((minX + surveyed % width) << 4, (minZ + surveyed / width) << 4)) return false;
            surveyed++;
        }
        return true;
    }

    /** {@link #ready(Location)} for block coordinates, for callers that already have them. */
    public boolean ready(int blockX, int blockZ) {
        if (closed) return false;
        int x = blockX >> 4, z = blockZ >> 4;
        Held centre = held.get(key(x, z));
        if (centre != null && centre.surrounded) {
            waitingSince = -1;
            return true;
        }
        if (Bukkit.getWorld(world.getUID()) != world)
            throw new IllegalStateException("Paste world was unloaded while loading a chunk");
        if (pending != null) {
            if (!pending.isDone()) {
                checkTimeout(x, z, centre);
                return false;
            }
            CompletableFuture<Void> completed = pending;
            pending = null;
            completed.join(); // isDone above: never waits; rethrows a failed load.
            centre = held.get(key(x, z));
        }
        if (centre == null && world.isChunkLoaded(x, z)) {
            hold(world.getChunkAt(x, z));
            centre = held.get(key(x, z));
        }
        if (centre == null) {
            checkTimeout(x, z, null);
            // Runs on the server thread while the loader still holds the chunk.
            pending = request(x, z).thenAccept(chunk -> {
                if (!closed && chunk != null && chunk.isLoaded() && Bukkit.getWorld(world.getUID()) == world) hold(chunk);
            });
            return false;
        }
        CompletableFuture<?>[] missing = requestMissingNeighbours(x, z);
        if (missing.length == 0) {
            centre.surrounded = true;
            waitingSince = -1;
            return true;
        }
        checkTimeout(x, z, centre);
        pending = CompletableFuture.allOf(missing);
        return false;
    }

    /** Throws once one wait has lasted the timeout, naming a chunk that is still missing. */
    private void checkTimeout(int x, int z, Held centre) {
        long now = System.nanoTime();
        if (waitingSince < 0) {
            waitingSince = now;
            return;
        }
        if (now - waitingSince <= loadTimeoutNanos) return;
        if (centre != null)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if ((dx != 0 || dz != 0) && !world.isChunkLoaded(x + dx, z + dz))
                        throw new LoadTimeout(x + dx, z + dz, world, loadTimeoutNanos);
        throw new LoadTimeout(x, z, world, loadTimeoutNanos);
    }

    /**
     * Requests the chunks round a held chunk that are not loaded yet. Its ticket already asks the server for
     * them; the requests say when they are in. Spigot loads synchronously, so there it is one chunk a tick.
     */
    private CompletableFuture<?>[] requestMissingNeighbours(int x, int z) {
        CompletableFuture<?>[] requests = new CompletableFuture<?>[8];
        int count = 0;
        for (int dx = -1; dx <= 1 && (asyncLoad != null || count == 0); dx++)
            for (int dz = -1; dz <= 1 && (asyncLoad != null || count == 0); dz++)
                if ((dx != 0 || dz != 0) && !world.isChunkLoaded(x + dx, z + dz))
                    requests[count++] = request(x + dx, z + dz);
        return Arrays.copyOf(requests, count);
    }

    private CompletableFuture<Chunk> request(int x, int z) {
        if (asyncLoad != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Chunk> paperRequest = (CompletableFuture<Chunk>) asyncLoad.invoke(world, x, z, true);
                return paperRequest;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not request a Paper chunk for pasting", failure);
            }
        }
        // Spigot exposes synchronous loading only. Isolate one load on a later tick;
        // the load itself cannot honor a time budget on that platform.
        CompletableFuture<Chunk> spigotRequest = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> {
            if (closed || Bukkit.getWorld(world.getUID()) != world) { spigotRequest.cancel(false); return; }
            try { spigotRequest.complete(world.getChunkAt(x, z)); }
            catch (Throwable failure) { spigotRequest.completeExceptionally(failure); }
        });
        return spigotRequest;
    }

    private void hold(Chunk chunk) {
        long key = key(chunk.getX(), chunk.getZ());
        if (held.get(key) != null) return;
        held.put(key, new Held(chunk));
        PluginChunkHolds.hold(chunk);
        if (held.size() <= MAX_HELD_CHUNKS) return;
        Iterator<Held> eldest = held.values().iterator();
        Chunk released = eldest.next().chunk;
        eldest.remove();
        PluginChunkHolds.release(released);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @Override public void close() {
        closed = true;
        for (Held hold : held.values()) PluginChunkHolds.release(hold.chunk);
        held.clear();
        pending = null;
    }
}
