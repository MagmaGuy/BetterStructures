package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.BetterStructures;
import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAirBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitLiquidBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitSurfaceBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitUndergroundShallowBuilding;
import com.magmaguy.betterstructures.buildingfitter.util.FitUndergroundDeepBuilding;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.modules.WFCGenerator;
import com.magmaguy.betterstructures.modules.NaturalDungeonReservation;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class NewChunkLoadEvent implements Listener {

    private static final Set<LoadingChunkKey> loadingChunks = new HashSet<>();
    // A content reload temporarily owns and rebuilds every registry read by a
    // chunk scan. Keep only stable coordinates for new chunks observed during
    // that window; retaining Chunk/World objects would pin unloaded worlds.
    private static final Set<LoadingChunkKey> deferredNewChunks = new LinkedHashSet<>();
    // Structure grid chunks waiting one tick for their terrain scans, each held
    // loaded with a plugin ticket. The Chunk is kept for that tick only; a reload
    // moves these to deferredNewChunks as coordinates.
    private static final Map<LoadingChunkKey, Chunk> heldTerrainScans = new LinkedHashMap<>();
    private static final ChunkScanReentrancyGuard chunkScanReentrancyGuard = new ChunkScanReentrancyGuard();
    private static final int MAX_DEFERRED_SCANS_PER_DRAIN = 32;
    private static final long MAX_DEFERRED_SCAN_NANOS = 5_000_000L;
    private static BukkitTask deferredDrainTask;
    private static BukkitTask heldTerrainScanTask;
    private static int remainingNewInspections;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        LoadingChunkKey loadingChunkKey = LoadingChunkKey.from(chunk);
        boolean deferred = deferredNewChunks.contains(loadingChunkKey);

        if (BetterStructures.isReloading()) {
            if (event.isNewChunk()) deferredNewChunks.add(loadingChunkKey);
            return;
        }
        // A deferred chunk may have unloaded while content was rebuilding. Its
        // later load is no longer reported as "new", but it still needs the one
        // generation scan that was postponed by the reload gate.
        if (!event.isNewChunk() && !deferred) return;

        if (chunkScanReentrancyGuard.isActive()) {
            // This load is a side effect of a running scan's terrain reads. It is
            // not queued: replaying those would recursively expand generation. A
            // reload-deferred chunk still needs its scan; the chunk is loaded now,
            // so drain it next tick, once the outer scan has released the guard.
            if (deferred) scheduleDeferredDrain();
            return;
        }
        deferredNewChunks.remove(loadingChunkKey);
        // The fitters also read terrain in the neighbouring chunks. Reading one
        // that is not generated yet from inside this callback makes the server
        // thread wait for it while the chunk system is still dispatching this
        // load, which can stall the server for good. Scan on the next tick
        // instead, holding the chunk until then: pregenerators such as
        // /bs pregenerate and Chunky release their chunks right after loading.
        if (claimNewChunk(chunk, loadingChunkKey)) holdForTerrainScan(chunk, loadingChunkKey);
    }

    private static void scanNewChunk(Chunk chunk, LoadingChunkKey loadingChunkKey) {
        if (claimNewChunk(chunk, loadingChunkKey)) runTerrainScans(chunk);
    }

    /**
     * Claims a new chunk's one generation scan and runs its dungeon scan, which
     * needs only coordinates. Reads no terrain. Returns whether the chunk is a
     * grid point for a structure type whose fitter must read terrain.
     */
    private static boolean claimNewChunk(Chunk chunk, LoadingChunkKey loadingChunkKey) {
        if (!ValidWorldsConfig.isValidWorld(chunk.getWorld())) return false;
        if (loadingChunks.contains(loadingChunkKey)) return false;
        //In some cases the same chunk gets loaded (at least at an event level) several times, this prevents the plugin from doing multiple scans and placing multiple builds, enhancing performance
        loadingChunks.add(loadingChunkKey);
        new BukkitRunnable() {
            @Override
            public void run() {
                loadingChunks.remove(loadingChunkKey);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20L);

        dungeonScanner(chunk.getWorld(), chunk.getX(), chunk.getZ());
        return isSurfacePosition(chunk) || isShallowUndergroundPosition(chunk) || isDeepUndergroundPosition(chunk)
                || isSkyPosition(chunk) || isLiquidSurfacePosition(chunk);
    }

    private static void runTerrainScans(Chunk chunk) {
        if (isSurfacePosition(chunk)) new FitSurfaceBuilding(chunk);
        if (isShallowUndergroundPosition(chunk)) FitUndergroundShallowBuilding.fit(chunk);
        if (isDeepUndergroundPosition(chunk)) FitUndergroundDeepBuilding.fit(chunk);
        if (isSkyPosition(chunk)) new FitAirBuilding(chunk);
        if (isLiquidSurfacePosition(chunk)) new FitLiquidBuilding(chunk);
    }

    private static void holdForTerrainScan(Chunk chunk, LoadingChunkKey loadingChunkKey) {
        if (heldTerrainScans.putIfAbsent(loadingChunkKey, chunk) != null) return;
        PluginChunkHolds.hold(chunk);
        if (heldTerrainScanTask == null)
            heldTerrainScanTask = Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, NewChunkLoadEvent::runHeldTerrainScans);
    }

    private static void runHeldTerrainScans() {
        heldTerrainScanTask = null;
        if (BetterStructures.isReloading()) {
            releaseHeldTerrainScans(true);
            return;
        }
        List<Chunk> chunks = new ArrayList<>(heldTerrainScans.values());
        heldTerrainScans.clear();
        for (Chunk chunk : chunks) {
            try {
                World world = chunk.getWorld();
                // An unloaded world took its chunks and tickets with it.
                if (Bukkit.getWorld(world.getUID()) == world && chunk.isLoaded())
                    chunkScanReentrancyGuard.runIfIdle(() -> runTerrainScans(chunk));
            } catch (Throwable failure) {
                MetadataHandler.PLUGIN.getLogger().log(Level.WARNING, "Failed chunk scan at "
                        + chunk.getWorld().getName() + " " + chunk.getX() + ", " + chunk.getZ(), failure);
            } finally {
                PluginChunkHolds.release(chunk);
            }
        }
    }

    private static void releaseHeldTerrainScans(boolean keepForReplay) {
        if (heldTerrainScanTask != null) {
            heldTerrainScanTask.cancel();
            heldTerrainScanTask = null;
        }
        for (Map.Entry<LoadingChunkKey, Chunk> held : heldTerrainScans.entrySet()) {
            if (keepForReplay) deferredNewChunks.add(held.getKey());
            PluginChunkHolds.release(held.getValue());
        }
        heldTerrainScans.clear();
    }

    public static void prepareForContentReload() {
        cancelDeferredDrain();
        releaseHeldTerrainScans(true);
        loadingChunks.clear();
    }

    /**
     * Schedules a bounded replay of chunks that generated during a content
     * reload and are still loaded, without force-loading worlds or chunks.
     * Unloaded coordinates stay queued and are scanned by their next ordinary
     * ChunkLoadEvent.
     */
    public static void replayDeferredNewChunks() {
        scheduleDeferredDrain();
    }

    private static void scheduleDeferredDrain() {
        scheduleDeferredDrain(true);
    }

    private static void scheduleDeferredDrain(boolean wakeup) {
        if (wakeup) remainingNewInspections = deferredNewChunks.size();
        if (BetterStructures.isReloading() || deferredNewChunks.isEmpty()
                || deferredDrainTask != null || MetadataHandler.PLUGIN == null
                || !MetadataHandler.PLUGIN.isEnabled()) return;

        deferredDrainTask = Bukkit.getScheduler().runTaskLater(
                MetadataHandler.PLUGIN,
                () -> {
                    deferredDrainTask = null;
                    drainDeferredNewChunks();
                },
                1L);
    }

    private static void drainDeferredNewChunks() {
        if (BetterStructures.isReloading() || MetadataHandler.PLUGIN == null || !MetadataHandler.PLUGIN.isEnabled()) return;
        long started = System.nanoTime();
        // Only keys that are scanned or scheduled count against the cap. Skipping
        // a key is cheap, and charging it would let dormant keys starve live ones.
        int scanned = 0;
        while (remainingNewInspections > 0 && !deferredNewChunks.isEmpty()
                && scanned < MAX_DEFERRED_SCANS_PER_DRAIN
                && System.nanoTime() - started < MAX_DEFERRED_SCAN_NANOS) {
            var iterator = deferredNewChunks.iterator();
            LoadingChunkKey key = iterator.next();
            iterator.remove();
            remainingNewInspections--;
            World world = Bukkit.getWorld(key.worldId());
            if (world == null || !world.isChunkLoaded(key.x(), key.z())) {
                deferredNewChunks.add(key);
                continue;
            }
            scanned++;
            try {
                Chunk chunk = world.getChunkAt(key.x(), key.z());
                if (!chunkScanReentrancyGuard.runIfIdle(() -> scanNewChunk(chunk, key))) deferredNewChunks.add(key);
            } catch (Throwable failure) {
                deferredNewChunks.add(key);
                MetadataHandler.PLUGIN.getLogger().warning("Failed deferred chunk scan at " + key + ": " + failure.getMessage());
            }
        }
        if (deferredNewChunks.isEmpty()) remainingNewInspections = 0;
        // Complete at most one bounded traversal per wakeup. Dormant chunks await their next load event.
        if (remainingNewInspections > 0) scheduleDeferredDrain(false);
    }
    private static void cancelDeferredDrain() {
        if (deferredDrainTask == null) return;
        deferredDrainTask.cancel();
        deferredDrainTask = null;
    }

    public static void discardDeferredNewChunks() {
        cancelDeferredDrain();
        releaseHeldTerrainScans(false);
        deferredNewChunks.clear();
    }

    public static void shutdown() {
        cancelDeferredDrain();
        releaseHeldTerrainScans(false);
        loadingChunks.clear();
        deferredNewChunks.clear();
    }

    private record LoadingChunkKey(UUID worldId, int x, int z) {
        private static LoadingChunkKey from(Chunk chunk) {
            return new LoadingChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private static boolean isValidStructurePosition(Chunk chunk, GeneratorConfigFields.StructureType structureType,
                                                    int gridDistance, int maxOffset) {
        return isValidStructurePosition(chunk.getWorld(), chunk.getX(), chunk.getZ(),
                structureType, gridDistance, maxOffset);
    }

    /**
     * Determines if the given chunk is a valid structure position based on
     * a diamond grid pattern with seeded random offsets.
     *
     * @param world The world of the chunk
     * @param x The chunk X coordinate
     * @param z The chunk Z coordinate
     * @param structureType The type of structure
     * @param gridDistance The distance between grid points
     * @param maxOffset The maximum random offset from grid points
     * @return True if this chunk should have a structure
     */
    private static boolean isValidStructurePosition(World world, int x, int z,
                                                    GeneratorConfigFields.StructureType structureType,
                                                    int gridDistance, int maxOffset) {
        // Check spawn protection radius (2D distance from 0,0 in blocks)
        int spawnProtectionRadius = DefaultConfig.getSpawnProtectionRadius();
        if (spawnProtectionRadius > 0) {
            int blockX = x * 16 + 8;
            int blockZ = z * 16 + 8;
            if ((long) blockX * blockX + (long) blockZ * blockZ < (long) spawnProtectionRadius * spawnProtectionRadius) {
                return false;
            }
        }

        long worldSeed = world.getSeed();

        // Create a unique seed for each structure type
        long typeSeed = worldSeed + structureType.name().hashCode() * 7919; // Use a prime number for better distribution

        // Check all nearby grid cells that could have a structure landing on this chunk
        long minimumGridX = ((long) x - maxOffset) / gridDistance - 1;
        long maximumGridX = ((long) x + maxOffset) / gridDistance + 1;
        long minimumGridZ = ((long) z - maxOffset) / gridDistance - 1;
        long maximumGridZ = ((long) z + maxOffset) / gridDistance + 1;
        int offsetBound = (int) (2L * maxOffset + 1L);
        for (long gridX = minimumGridX; gridX <= maximumGridX; gridX++) {
            for (long gridZ = minimumGridZ; gridZ <= maximumGridZ; gridZ++) {
                // Base position of this grid cell
                long baseX = gridX * gridDistance;
                long baseZ = gridZ * gridDistance;

                // Apply diamond pattern offset (shift every other row by gridDistance/2)
                if (gridZ % 2L != 0) {
                    baseX += gridDistance / 2;
                }

                // Create a seeded random for this specific grid cell
                Random cellRandom = new Random(typeSeed ^ (((long)baseX << 32) | (baseZ & 0xFFFFFFFFL)));

                // Generate the random offset for structure in this grid cell
                int offsetX = maxOffset > 0 ? cellRandom.nextInt(offsetBound) - maxOffset : 0;
                int offsetZ = maxOffset > 0 ? cellRandom.nextInt(offsetBound) - maxOffset : 0;

                // Final structure position for this grid cell
                long structureX = baseX + offsetX;
                long structureZ = baseZ + offsetZ;

                // If this chunk matches the structure position
                if (x == structureX && z == structureZ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isSurfacePosition(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SURFACE).isEmpty()) return false;
        // Get config values directly instead of using static finals
        return isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SURFACE,
                DefaultConfig.getDistanceSurface(), DefaultConfig.getMaxOffsetSurface());
    }

    private static boolean isShallowUndergroundPosition(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW).isEmpty()) return false;
        return isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW,
                DefaultConfig.getDistanceShallow(), DefaultConfig.getMaxOffsetShallow());
    }

    private static boolean isDeepUndergroundPosition(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_DEEP).isEmpty()) return false;
        return isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP,
                DefaultConfig.getDistanceDeep(), DefaultConfig.getMaxOffsetDeep());
    }

    private static boolean isSkyPosition(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SKY).isEmpty()) return false;
        return isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SKY,
                DefaultConfig.getDistanceSky(), DefaultConfig.getMaxOffsetSky());
    }

    private static boolean isLiquidSurfacePosition(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.LIQUID_SURFACE).isEmpty()) return false;
        return isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.LIQUID_SURFACE,
                DefaultConfig.getDistanceLiquid(), DefaultConfig.getMaxOffsetLiquid());
    }

    // Runs from coordinates alone: the origin chunk only marks where the dungeon
    // starts, and the paste loads and holds every chunk it writes to.
    private static void dungeonScanner(World world, int chunkX, int chunkZ) {
        if (ModuleGeneratorsConfig.getModuleGenerators().isEmpty()) return;
        if (!isValidStructurePosition(world, chunkX, chunkZ, GeneratorConfigFields.StructureType.DUNGEON,
                DefaultConfig.getDistanceDungeon(), DefaultConfig.getMaxOffsetDungeon())) return;
        List<ModuleGeneratorsConfigFields> validatedGenerators = new ArrayList<>();
        for (ModuleGeneratorsConfigFields moduleGeneratorsConfigFields : ModuleGeneratorsConfig.getModuleGenerators().values()){
            if (!moduleGeneratorsConfigFields.isEnabled()) continue;
            if (moduleGeneratorsConfigFields.getValidWorlds() != null && !moduleGeneratorsConfigFields.getValidWorlds().isEmpty() && !moduleGeneratorsConfigFields.getValidWorlds().contains(world.getName())) continue;
            if (moduleGeneratorsConfigFields.getValidWorldEnvironments() != null && !moduleGeneratorsConfigFields.getValidWorldEnvironments().isEmpty() && !moduleGeneratorsConfigFields.getValidWorldEnvironments().contains(world.getEnvironment())) continue;
            validatedGenerators.add(moduleGeneratorsConfigFields);
        }
        if (validatedGenerators.isEmpty()) return;
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = validatedGenerators.get(ThreadLocalRandom.current().nextInt(0, validatedGenerators.size()));
        int altitude = moduleGeneratorsConfigFields.getCenterModuleAltitude();
        // Chunk#getBlock used to reject this; keep refusing altitudes outside the world.
        if (altitude < world.getMinHeight() || altitude >= world.getMaxHeight()) {
            Logger.warn("Skipped natural modular generation because centerModuleAltitude " + altitude
                    + " of " + moduleGeneratorsConfigFields.getFilename() + " is outside the height of world "
                    + world.getName() + ".");
            return;
        }
        var startLocation = new Location(world, (chunkX << 4) + 8, altitude, (chunkZ << 4) + 8);
        // Only explicit protections stop a natural dungeon: spawn protection and WorldGuard regions.
        // Whether footprint chunks were already generated is not a usable signal: vanilla keeps
        // partially generated chunks around every loaded area, so that check refused nearly every
        // dungeon a player approached.
        var reservation = NaturalDungeonReservation.tryCreate(
                startLocation.getBlockX(),
                startLocation.getBlockZ(),
                moduleGeneratorsConfigFields.getRadius(),
                moduleGeneratorsConfigFields.getModuleSizeXZ(),
                DefaultConfig.getSpawnProtectionRadius());
        if (reservation.isEmpty()) {
            Logger.info("Skipped natural modular generation because its footprint overlaps spawn protection.");
            return;
        }

        NaturalDungeonReservation.BlockBounds bounds = reservation.get().blockBounds();
        if (hasWorldGuardOverlap(world, bounds)) {
            Logger.info("Skipped natural modular generation because its footprint overlaps "
                    + "a WorldGuard region.");
            return;
        }

        WFCGenerator.generateNaturally(
                moduleGeneratorsConfigFields,
                startLocation,
                () -> !hasWorldGuardOverlap(world, bounds));
    }

    private static boolean hasWorldGuardOverlap(
            World world,
            NaturalDungeonReservation.BlockBounds bounds) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return false;
        try {
            return WorldGuard.hasRegionOverlap(world, bounds);
        } catch (Throwable throwable) {
            Logger.warn("Could not verify WorldGuard regions for natural modular generation; "
                    + "the paste was cancelled: " + throwable.getMessage());
            return true;
        }
    }
}
