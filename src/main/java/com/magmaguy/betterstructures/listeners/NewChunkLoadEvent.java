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
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class NewChunkLoadEvent implements Listener {

    private static final Set<LoadingChunkKey> loadingChunks = new HashSet<>();
    // Terrain lookups must run after the chunk-load callback has returned. The
    // same queue also retains scans across content reloads. Store coordinates
    // rather than Chunk/World references so deferred work cannot pin worlds.
    private static final Set<LoadingChunkKey> deferredNewChunks = new LinkedHashSet<>();
    private static final DelayedDungeonScanTracker<LoadingChunkKey> delayedDungeonScans =
            new DelayedDungeonScanTracker<>();
    private static final Map<LoadingChunkKey, Long> recentlyGeneratedChunks = new LinkedHashMap<>();
    private static final long RECENT_NEW_CHUNK_NANOS = TimeUnit.MINUTES.toNanos(2);
    private static final int MAX_RECENT_NEW_CHUNKS = 65_536;
    private static final ChunkScanReentrancyGuard chunkScanReentrancyGuard = new ChunkScanReentrancyGuard();
    private static final int MAX_DEFERRED_SCANS_PER_DRAIN = 32;
    private static final long MAX_DEFERRED_SCAN_NANOS = 5_000_000L;
    private static BukkitTask deferredDrainTask;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        LoadingChunkKey loadingChunkKey = LoadingChunkKey.from(chunk);
        if (event.isNewChunk()) rememberGeneratedChunk(loadingChunkKey);
        boolean deferred = deferredNewChunks.contains(loadingChunkKey);
        boolean deferredDungeon = delayedDungeonScans.isDeferred(loadingChunkKey);

        if (BetterStructures.isReloading()) {
            if (event.isNewChunk()) deferredNewChunks.add(loadingChunkKey);
            return;
        }
        // A deferred chunk may have unloaded before its scan could run. Its
        // later load is no longer reported as "new", but it still needs the one
        // generation scan that was postponed by the reload gate.
        if (!event.isNewChunk() && !deferred) {
            if (deferredDungeon) {
                delayedDungeonScans.removeDeferred(loadingChunkKey);
                scheduleDungeonScanner(loadingChunkKey);
            }
            return;
        }

        if (chunkScanReentrancyGuard.isActive()) {
            // A terrain read can load neighboring chunks. Do not enqueue those
            // side effects: replaying them would recursively expand generation.
            // Previously queued work still needs a drain now that it is loaded.
            if (deferred) scheduleDeferredDrain();
            return;
        }
        deferredNewChunks.add(loadingChunkKey);
        scheduleDeferredDrain();
    }

    private static void scanNewChunk(Chunk chunk, LoadingChunkKey loadingChunkKey) {
        if (!ValidWorldsConfig.isValidWorld(chunk.getWorld())) return;
        if (loadingChunks.contains(loadingChunkKey)) return;
        //In some cases the same chunk gets loaded (at least at an event level) several times, this prevents the plugin from doing multiple scans and placing multiple builds, enhancing performance
        loadingChunks.add(loadingChunkKey);
        new BukkitRunnable() {
            @Override
            public void run() {
                loadingChunks.remove(loadingChunkKey);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20L);

        surfaceScanner(chunk);
        shallowUndergroundScanner(chunk);
        deepUndergroundScanner(chunk);
        skyScanner(chunk);
        liquidSurfaceScanner(chunk);
        scheduleDungeonScanner(loadingChunkKey);
    }

    public static void prepareForContentReload() {
        cancelDeferredDrain();
        delayedDungeonScans.deferAllPending();
        loadingChunks.clear();
    }

    /**
     * Schedules a bounded replay of deferred chunks that are still loaded
     * without force-loading worlds or chunks. Unloaded coordinates stay queued
     * and are consumed by their next ordinary ChunkLoadEvent.
     */
    public static void replayDeferredNewChunks() {
        scheduleDeferredDrain();
    }

    private static void scheduleDeferredDrain() {
        if (BetterStructures.isReloading()
                || (deferredNewChunks.isEmpty() && !delayedDungeonScans.hasDeferred())
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
        if (BetterStructures.isReloading()
                || (deferredNewChunks.isEmpty() && !delayedDungeonScans.hasDeferred())
                || MetadataHandler.PLUGIN == null || !MetadataHandler.PLUGIN.isEnabled()) return;

        long started = System.nanoTime();
        Set<LoadingChunkKey> attempted = new HashSet<>();
        int attempts = 0;
        for (LoadingChunkKey loadingChunkKey : new ArrayList<>(deferredNewChunks)) {
            if (attempts >= MAX_DEFERRED_SCANS_PER_DRAIN
                    || (attempts > 0 && System.nanoTime() - started >= MAX_DEFERRED_SCAN_NANOS)) break;
            World world = Bukkit.getWorld(loadingChunkKey.worldId());
            if (world == null || !world.isChunkLoaded(
                    loadingChunkKey.x(), loadingChunkKey.z())) continue;

            attempted.add(loadingChunkKey);
            attempts++;
            Chunk chunk = world.getChunkAt(loadingChunkKey.x(), loadingChunkKey.z());
            try {
                boolean scanned = chunkScanReentrancyGuard.runIfIdle(
                        () -> scanNewChunk(chunk, loadingChunkKey));
                if (scanned) {
                    deferredNewChunks.remove(loadingChunkKey);
                    delayedDungeonScans.removeDeferred(loadingChunkKey);
                } else {
                    scheduleDeferredDrain();
                    return;
                }
            } catch (Throwable throwable) {
                MetadataHandler.PLUGIN.getLogger().warning(
                        "Failed to replay deferred new-chunk scan for "
                                + world.getName() + " " + loadingChunkKey.x()
                                + "," + loadingChunkKey.z() + ": "
                                + throwable.getMessage());
                throwable.printStackTrace();
            }
        }

        for (LoadingChunkKey loadingChunkKey : delayedDungeonScans.deferredSnapshot()) {
            if (attempts >= MAX_DEFERRED_SCANS_PER_DRAIN) break;
            // A full deferred scan includes its dungeon scan and must run first.
            if (deferredNewChunks.contains(loadingChunkKey)) continue;
            World world = Bukkit.getWorld(loadingChunkKey.worldId());
            if (world == null || !world.isChunkLoaded(
                    loadingChunkKey.x(), loadingChunkKey.z())) continue;

            attempted.add(loadingChunkKey);
            attempts++;
            delayedDungeonScans.removeDeferred(loadingChunkKey);
            scheduleDungeonScanner(loadingChunkKey);
        }

        // A scan can synchronously load a key that was unloaded when this
        // snapshot reached it, and a large reload can exceed the per-tick cap.
        // Continue only when an unattempted queued key is already loaded; keys
        // that remain unloaded wait for their next normal load event.
        for (LoadingChunkKey loadingChunkKey : deferredNewChunks) {
            if (attempted.contains(loadingChunkKey)) continue;
            World world = Bukkit.getWorld(loadingChunkKey.worldId());
            if (world != null && world.isChunkLoaded(
                    loadingChunkKey.x(), loadingChunkKey.z())) {
                scheduleDeferredDrain();
                return;
            }
        }
        for (LoadingChunkKey loadingChunkKey : delayedDungeonScans.deferredSnapshot()) {
            if (attempted.contains(loadingChunkKey)
                    || deferredNewChunks.contains(loadingChunkKey)) continue;
            World world = Bukkit.getWorld(loadingChunkKey.worldId());
            if (world != null && world.isChunkLoaded(
                    loadingChunkKey.x(), loadingChunkKey.z())) {
                scheduleDeferredDrain();
                return;
            }
        }
    }

    private static void cancelDeferredDrain() {
        if (deferredDrainTask == null) return;
        deferredDrainTask.cancel();
        deferredDrainTask = null;
    }

    public static void discardDeferredNewChunks() {
        cancelDeferredDrain();
        deferredNewChunks.clear();
        delayedDungeonScans.clear();
    }

    public static void shutdown() {
        cancelDeferredDrain();
        loadingChunks.clear();
        deferredNewChunks.clear();
        delayedDungeonScans.clear();
        recentlyGeneratedChunks.clear();
    }

    private record LoadingChunkKey(UUID worldId, int x, int z) {
        private static LoadingChunkKey from(Chunk chunk) {
            return new LoadingChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private static void rememberGeneratedChunk(LoadingChunkKey key) {
        purgeExpiredGeneratedChunks();
        recentlyGeneratedChunks.put(key, System.nanoTime());
        while (recentlyGeneratedChunks.size() > MAX_RECENT_NEW_CHUNKS) {
            var iterator = recentlyGeneratedChunks.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    private static void purgeExpiredGeneratedChunks() {
        long cutoff = System.nanoTime() - RECENT_NEW_CHUNK_NANOS;
        recentlyGeneratedChunks.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    private static boolean wasRecentlyGenerated(UUID worldId, NaturalDungeonReservation.ChunkCoordinate coordinate) {
        purgeExpiredGeneratedChunks();
        return recentlyGeneratedChunks.containsKey(
                new LoadingChunkKey(worldId, coordinate.x(), coordinate.z()));
    }

    private static void scheduleDungeonScanner(LoadingChunkKey loadingChunkKey) {
        if (MetadataHandler.PLUGIN == null || !MetadataHandler.PLUGIN.isEnabled()) return;
        if (!delayedDungeonScans.markPending(loadingChunkKey)) return;
        Bukkit.getScheduler().runTaskLater(MetadataHandler.PLUGIN, () -> {
            // A reload moves pending keys to the deferred set before cancelling Bukkit tasks.
            // If a task survives that cancellation, it no longer owns this key.
            if (!delayedDungeonScans.takePending(loadingChunkKey)) return;
            if (BetterStructures.isReloading()) {
                delayedDungeonScans.defer(loadingChunkKey);
                return;
            }
            World world = Bukkit.getWorld(loadingChunkKey.worldId());
            if (world == null || !world.isChunkLoaded(
                    loadingChunkKey.x(), loadingChunkKey.z())) {
                delayedDungeonScans.defer(loadingChunkKey);
                return;
            }
            dungeonScanner(world.getChunkAt(loadingChunkKey.x(), loadingChunkKey.z()));
        }, 1L);
    }

    /**
     * Determines if the given chunk is a valid structure position based on
     * a diamond grid pattern with seeded random offsets.
     *
     * @param chunk The chunk to check
     * @param structureType The type of structure
     * @param gridDistance The distance between grid points
     * @param maxOffset The maximum random offset from grid points
     * @return True if this chunk should have a structure
     */
    private static boolean isValidStructurePosition(Chunk chunk, GeneratorConfigFields.StructureType structureType,
                                                    int gridDistance, int maxOffset) {
        int x = chunk.getX();
        int z = chunk.getZ();

        // Check spawn protection radius (2D distance from 0,0 in blocks)
        int spawnProtectionRadius = DefaultConfig.getSpawnProtectionRadius();
        if (spawnProtectionRadius > 0) {
            int blockX = x * 16 + 8;
            int blockZ = z * 16 + 8;
            if ((long) blockX * blockX + (long) blockZ * blockZ < (long) spawnProtectionRadius * spawnProtectionRadius) {
                return false;
            }
        }

        long worldSeed = chunk.getWorld().getSeed();

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

    private static void surfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SURFACE).isEmpty()) return;
        // Get config values directly instead of using static finals
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SURFACE,
                DefaultConfig.getDistanceSurface(), DefaultConfig.getMaxOffsetSurface())) return;
        new FitSurfaceBuilding(chunk);
    }

    private static void shallowUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW,
                DefaultConfig.getDistanceShallow(), DefaultConfig.getMaxOffsetShallow())) return;
        FitUndergroundShallowBuilding.fit(chunk);
    }

    private static void deepUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_DEEP).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP,
                DefaultConfig.getDistanceDeep(), DefaultConfig.getMaxOffsetDeep())) return;
        FitUndergroundDeepBuilding.fit(chunk);
    }

    private static void skyScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SKY).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SKY,
                DefaultConfig.getDistanceSky(), DefaultConfig.getMaxOffsetSky())) return;
        new FitAirBuilding(chunk);
    }

    private static void liquidSurfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.LIQUID_SURFACE).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.LIQUID_SURFACE,
                DefaultConfig.getDistanceLiquid(), DefaultConfig.getMaxOffsetLiquid())) return;
        new FitLiquidBuilding(chunk);
    }

    private static void dungeonScanner(Chunk chunk) {
        if (ModuleGeneratorsConfig.getModuleGenerators().isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.DUNGEON,
                DefaultConfig.getDistanceDungeon(), DefaultConfig.getMaxOffsetDungeon())) return;
        List<ModuleGeneratorsConfigFields> validatedGenerators = new ArrayList<>();
        for (ModuleGeneratorsConfigFields moduleGeneratorsConfigFields : ModuleGeneratorsConfig.getModuleGenerators().values()){
            if (!moduleGeneratorsConfigFields.isEnabled()) continue;
            if (moduleGeneratorsConfigFields.getValidWorlds() != null && !moduleGeneratorsConfigFields.getValidWorlds().isEmpty() && !moduleGeneratorsConfigFields.getValidWorlds().contains(chunk.getWorld().getName())) continue;
            if (moduleGeneratorsConfigFields.getValidWorldEnvironments() != null && !moduleGeneratorsConfigFields.getValidWorldEnvironments().isEmpty() && !moduleGeneratorsConfigFields.getValidWorldEnvironments().contains(chunk.getWorld().getEnvironment())) continue;
            validatedGenerators.add(moduleGeneratorsConfigFields);
        }
        if (validatedGenerators.isEmpty()) return;
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = validatedGenerators.get(ThreadLocalRandom.current().nextInt(0, validatedGenerators.size()));
        var startLocation = chunk.getBlock(
                8, moduleGeneratorsConfigFields.getCenterModuleAltitude(), 8).getLocation();
        UUID worldId = chunk.getWorld().getUID();
        var reservation = NaturalDungeonReservation.tryCreate(
                startLocation.getBlockX(),
                startLocation.getBlockZ(),
                moduleGeneratorsConfigFields.getRadius(),
                moduleGeneratorsConfigFields.getModuleSizeXZ(),
                DefaultConfig.getSpawnProtectionRadius(),
                coordinate -> chunk.getWorld().isChunkGenerated(coordinate.x(), coordinate.z()),
                coordinate -> wasRecentlyGenerated(worldId, coordinate));
        if (reservation.isEmpty()) {
            Logger.info("Skipped natural modular generation because its full footprint overlaps "
                    + "spawn protection or a chunk that was not just generated.");
            return;
        }

        NaturalDungeonReservation naturalReservation = reservation.get();
        if (hasWorldGuardOverlap(chunk.getWorld(), naturalReservation.blockBounds())) {
            Logger.info("Skipped natural modular generation because its full footprint overlaps "
                    + "a WorldGuard region.");
            return;
        }

        WFCGenerator.generateNaturally(
                moduleGeneratorsConfigFields,
                startLocation,
                () -> naturalReservation.remainsSafe(
                        coordinate -> chunk.getWorld().isChunkGenerated(coordinate.x(), coordinate.z()))
                        && !hasWorldGuardOverlap(chunk.getWorld(), naturalReservation.blockBounds()));
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
