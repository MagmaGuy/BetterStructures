package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import com.magmaguy.betterstructures.buildingfitter.util.TerrainReads;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Runs the terrain scans of new structure grid chunks without loading chunks on the server thread.
 * <p>
 * A grid chunk is held from its load event until its scans finish. Its plugin ticket also brings
 * every chunk within two of it to full status. A scan that reads a chunk that is not loaded yet
 * (see {@link TerrainReads}) runs again, with the same schematic and anchor, once every chunk it
 * missed has loaded. Chunks beyond the grid chunk's ticket are requested without blocking and held
 * until the scans finish. Spigot has no asynchronous chunk API, so there they load synchronously,
 * one per tick. A scan still waiting after a minute is dropped: Paper can leave a chunk stuck
 * partway through generation for good (PaperMC/Paper#11174), and waiting for one on the server
 * thread froze the server.
 */
final class TerrainScanQueue {
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final long WARNING_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);
    // A plugin ticket brings its chunk and every chunk within this distance of it to full status.
    private static final int TICKET_RADIUS = 2;
    private static final Map<NewChunkLoadEvent.LoadingChunkKey, Job> jobs = new LinkedHashMap<>();
    private static final ArrayDeque<Request> synchronousRequests = new ArrayDeque<>();
    private static BukkitTask driver;
    private static boolean warned;
    private static long lastWarning;
    private static int suppressedWarnings;

    private TerrainScanQueue() {
    }

    /** Holds a new grid chunk and starts its scans on the next tick. */
    static void enqueue(NewChunkLoadEvent.LoadingChunkKey key, Chunk grid, List<Supplier<FitAnything>> scans) {
        if (scans.isEmpty() || jobs.containsKey(key)) return;
        PluginChunkHolds.hold(grid);
        jobs.put(key, new Job(key, grid, scans));
        if (driver == null)
            driver = Bukkit.getScheduler().runTaskTimer(MetadataHandler.PLUGIN, TerrainScanQueue::tick, 1L, 1L);
    }

    /**
     * Whether a waiting scan asked for this chunk. Such a chunk starts no scans of its own: a
     * scan's reads used to load it synchronously and were skipped the same way, because scans
     * started by other scans can spread generation across the world when structures are close.
     */
    static boolean requested(Chunk chunk) {
        if (jobs.isEmpty()) return false;
        World world = chunk.getWorld();
        long key = TerrainReads.key(chunk.getX(), chunk.getZ());
        for (Job job : jobs.values())
            if (job.world == world && job.requested.contains(key)) return true;
        return false;
    }

    /**
     * Drops every scan and its chunk holds. A scan that has not run yet is passed to replay, when
     * given. One that has run is dropped: it holds schematics from before a reload, and running it
     * again could place a second structure.
     */
    static void clear(Consumer<NewChunkLoadEvent.LoadingChunkKey> replay) {
        for (Job job : jobs.values()) {
            if (replay != null && !job.started) replay.accept(job.key);
            job.close();
        }
        jobs.clear();
        synchronousRequests.clear();
        if (driver != null) {
            driver.cancel();
            driver = null;
        }
    }

    private static void tick() {
        loadNextSynchronousRequest();
        // A scan can place a structure, and anything that runs then may load a chunk and add a job.
        for (Job job : new ArrayList<>(jobs.values())) {
            if (job.closed) continue;
            // An unloaded world took its chunks and tickets with it.
            if (Bukkit.getWorld(job.world.getUID()) != job.world || !job.grid.isLoaded()) {
                finish(job);
                continue;
            }
            if (!job.ready()) {
                if (System.nanoTime() - job.waitingSince > TIMEOUT_NANOS) {
                    warnTimedOut(job);
                    finish(job);
                }
                continue;
            }
            job.run();
            if (job.waiting.isEmpty()) {
                finish(job);
                continue;
            }
            job.waitingSince = System.nanoTime();
            job.ready();
        }
        if (jobs.isEmpty() && synchronousRequests.isEmpty() && driver != null) {
            driver.cancel();
            driver = null;
        }
    }

    private static void finish(Job job) {
        jobs.remove(job.key);
        job.close();
    }

    private static void loadNextSynchronousRequest() {
        Request request;
        while ((request = synchronousRequests.poll()) != null) {
            Job job = request.job();
            int x = TerrainReads.chunkX(request.chunk()), z = TerrainReads.chunkZ(request.chunk());
            if (job.closed || job.world.isChunkLoaded(x, z)) continue;
            try {
                job.hold(request.chunk(), job.world.getChunkAt(x, z));
            } catch (Throwable failure) {
                job.requested.remove(request.chunk());
                MetadataHandler.PLUGIN.getLogger().log(Level.WARNING, "Failed to load chunk " + x + ", " + z
                        + " in " + job.world.getName() + " for a structure scan", failure);
            }
            return;
        }
    }

    private static void warnTimedOut(Job job) {
        long now = System.nanoTime();
        if (warned && now - lastWarning < WARNING_INTERVAL_NANOS) {
            suppressedWarnings++;
            return;
        }
        List<String> missing = new ArrayList<>();
        for (long chunk : job.awaited) {
            int x = TerrainReads.chunkX(chunk), z = TerrainReads.chunkZ(chunk);
            if (!job.world.isChunkLoaded(x, z)) missing.add("(" + x + ", " + z + ")");
        }
        Logger.warn("Skipped structure placement at chunk " + job.grid.getX() + ", " + job.grid.getZ() + " in "
                + job.world.getName() + " because chunks " + String.join(", ", missing)
                + " did not finish loading within 60 seconds. Paper can leave a chunk stuck partway through"
                + " generation (PaperMC/Paper#11174); BetterStructures skips it instead of waiting on the server thread."
                + (suppressedWarnings > 0 ? " " + suppressedWarnings + " more scans were skipped the same way since the last warning." : ""));
        warned = true;
        lastWarning = now;
        suppressedWarnings = 0;
    }

    private record Request(Job job, long chunk) {
    }

    private static final class Job {
        private final NewChunkLoadEvent.LoadingChunkKey key;
        private final Chunk grid;
        private final World world;
        private final Method asyncLoad;
        private List<Supplier<FitAnything>> scans;
        private final List<FitAnything> waiting = new ArrayList<>();
        // Chunks the waiting scans could not read, and those of them this job asked to load.
        private final Set<Long> awaited = new HashSet<>();
        private final Set<Long> requested = new HashSet<>();
        private final Map<Long, Chunk> held = new HashMap<>();
        private long waitingSince;
        private boolean started;
        private boolean closed;

        private Job(NewChunkLoadEvent.LoadingChunkKey key, Chunk grid, List<Supplier<FitAnything>> scans) {
            this.key = key;
            this.grid = grid;
            this.world = grid.getWorld();
            this.scans = scans;
            Method method;
            try {
                method = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class);
            } catch (NoSuchMethodException spigot) {
                method = null;
            }
            asyncLoad = method;
        }

        /** Runs the scans that have not run yet and those waiting for chunks that have now loaded. */
        private void run() {
            started = true;
            List<Supplier<FitAnything>> attempts = new ArrayList<>();
            if (scans != null) attempts.addAll(scans);
            scans = null;
            for (FitAnything fit : waiting)
                attempts.add(() -> {
                    fit.rescan();
                    return fit;
                });
            waiting.clear();
            awaited.clear();
            requested.clear();
            for (Supplier<FitAnything> attempt : attempts) {
                List<FitAnything> fit = new ArrayList<>(1);
                Set<Long> misses;
                try {
                    misses = TerrainReads.collectMisses(() -> fit.add(attempt.get()));
                } catch (Throwable failure) {
                    MetadataHandler.PLUGIN.getLogger().log(Level.WARNING, "Failed chunk scan at "
                            + world.getName() + " " + grid.getX() + ", " + grid.getZ(), failure);
                    continue;
                }
                if (misses.isEmpty() || fit.get(0) == null) continue;
                waiting.add(fit.get(0));
                awaited.addAll(misses);
            }
        }

        /** Whether every awaited chunk is loaded. Holds those beyond the grid ticket and requests the rest. */
        private boolean ready() {
            boolean ready = true;
            for (long chunk : awaited) {
                int x = TerrainReads.chunkX(chunk), z = TerrainReads.chunkZ(chunk);
                if (world.isChunkLoaded(x, z)) {
                    hold(chunk, world.getChunkAt(x, z));
                } else {
                    ready = false;
                    request(chunk);
                }
            }
            return ready;
        }

        private void request(long chunk) {
            // The grid chunk's own ticket is already loading it.
            if (withinGridTicket(chunk) || !requested.add(chunk)) return;
            if (asyncLoad == null) {
                synchronousRequests.add(new Request(this, chunk));
                return;
            }
            CompletableFuture<?> load;
            try {
                load = (CompletableFuture<?>) asyncLoad.invoke(world, TerrainReads.chunkX(chunk), TerrainReads.chunkZ(chunk), true);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not request a Paper chunk for a structure scan", failure);
            }
            // Paper completes the request on the server thread, then drops its own load ticket:
            // the chunk must be held here or it unloads before the scan runs.
            load.whenComplete((loaded, failure) -> {
                if (closed) return;
                if (failure == null && loaded instanceof Chunk loadedChunk && loadedChunk.isLoaded())
                    hold(chunk, loadedChunk);
                else
                    requested.remove(chunk);
            });
        }

        private void hold(long chunk, Chunk loaded) {
            if (withinGridTicket(chunk) || held.containsKey(chunk)) return;
            held.put(chunk, loaded);
            PluginChunkHolds.hold(loaded);
        }

        private boolean withinGridTicket(long chunk) {
            return Math.abs(TerrainReads.chunkX(chunk) - grid.getX()) <= TICKET_RADIUS
                    && Math.abs(TerrainReads.chunkZ(chunk) - grid.getZ()) <= TICKET_RADIUS;
        }

        private void close() {
            if (closed) return;
            closed = true;
            PluginChunkHolds.release(grid);
            for (Chunk chunk : held.values()) PluginChunkHolds.release(chunk);
            held.clear();
        }
    }
}
