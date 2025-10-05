package com.magmaguy.betterstructures.listeners;

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
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NewChunkLoadEvent implements Listener {

    private static HashSet<Chunk> loadingChunks = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        if (loadingChunks.contains(event.getChunk())) return;
        //In some cases the same chunk gets loaded (at least at an event level) several times, this prevents the plugin from doing multiple scans and placing multiple builds, enhancing performance
        loadingChunks.add(event.getChunk());
        new BukkitRunnable() {
            @Override
            public void run() {
                loadingChunks.remove(event.getChunk());
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20L);
        if (!ValidWorldsConfig.isValidWorld(event.getWorld())) return;

        surfaceScanner(event.getChunk());
        shallowUndergroundScanner(event.getChunk());
        deepUndergroundScanner(event.getChunk());
        skyScanner(event.getChunk());
        liquidSurfaceScanner(event.getChunk());
        dungeonScanner(event.getChunk());
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
    private boolean isValidStructurePosition(Chunk chunk, GeneratorConfigFields.StructureType structureType,
                                             int gridDistance, int maxOffset) {
        int x = chunk.getX();
        int z = chunk.getZ();
        long worldSeed = chunk.getWorld().getSeed();

        // Create a unique seed for each structure type
        long typeSeed = worldSeed + structureType.name().hashCode() * 7919; // Use a prime number for better distribution

        // Check all nearby grid cells that could have a structure landing on this chunk
        for (int gridX = (x - maxOffset) / gridDistance - 1; gridX <= (x + maxOffset) / gridDistance + 1; gridX++) {
            for (int gridZ = (z - maxOffset) / gridDistance - 1; gridZ <= (z + maxOffset) / gridDistance + 1; gridZ++) {
                // Base position of this grid cell
                int baseX = gridX * gridDistance;
                int baseZ = gridZ * gridDistance;

                // Apply diamond pattern offset (shift every other row by gridDistance/2)
                if (gridZ % 2 != 0) {
                    baseX += gridDistance / 2;
                }

                // Create a seeded random for this specific grid cell
                Random cellRandom = new Random(typeSeed ^ (((long)baseX << 32) | (baseZ & 0xFFFFFFFFL)));

                // Generate the random offset for structure in this grid cell
                int offsetX = maxOffset > 0 ? cellRandom.nextInt(maxOffset * 2 + 1) - maxOffset : 0;
                int offsetZ = maxOffset > 0 ? cellRandom.nextInt(maxOffset * 2 + 1) - maxOffset : 0;

                // Final structure position for this grid cell
                int structureX = baseX + offsetX;
                int structureZ = baseZ + offsetZ;

                // If this chunk matches the structure position
                if (x == structureX && z == structureZ) {
                    return true;
                }
            }
        }

        return false;
    }

    private void surfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SURFACE).isEmpty()) return;
        // Get config values directly instead of using static finals
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SURFACE,
                DefaultConfig.getDistanceSurface(), DefaultConfig.getMaxOffsetSurface())) return;
        new FitSurfaceBuilding(chunk);
    }

    private void shallowUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW,
                DefaultConfig.getDistanceShallow(), DefaultConfig.getMaxOffsetShallow())) return;
        FitUndergroundShallowBuilding.fit(chunk);
    }

    private void deepUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_DEEP).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.UNDERGROUND_DEEP,
                DefaultConfig.getDistanceDeep(), DefaultConfig.getMaxOffsetDeep())) return;
        FitUndergroundDeepBuilding.fit(chunk);
    }

    private void skyScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SKY).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.SKY,
                DefaultConfig.getDistanceSky(), DefaultConfig.getMaxOffsetSky())) return;
        new FitAirBuilding(chunk);
    }

    private void liquidSurfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.LIQUID_SURFACE).isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.LIQUID_SURFACE,
                DefaultConfig.getDistanceLiquid(), DefaultConfig.getMaxOffsetLiquid())) return;
        new FitLiquidBuilding(chunk);
    }

    private void dungeonScanner(Chunk chunk) {
        if (ModuleGeneratorsConfig.getModuleGenerators().isEmpty()) return;
        if (!isValidStructurePosition(chunk, GeneratorConfigFields.StructureType.DUNGEON,
                DefaultConfig.getDistanceDungeon(), DefaultConfig.getMaxOffsetDungeon())) return;
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = ModuleGeneratorsConfig.getModuleGenerators().values().stream().toList().get(ThreadLocalRandom.current().nextInt(0, ModuleGeneratorsConfig.getModuleGenerators().size()));
        new WFCGenerator(moduleGeneratorsConfigFields, chunk.getBlock(8,0,8).getLocation());
    }
}