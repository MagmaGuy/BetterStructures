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
import com.magmaguy.betterstructures.schematics.SchematicContainer;
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
    private static Random random = null;
    private static int surfaceOffset;
    private static int shallowUndergroundOffset;
    private static int deepUndergroundOffset;
    private static int airOffset;
    private static int liquidOffset;

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
        if (random == null) {
            random = new Random();
        }
        surfaceScanner(event.getChunk());
        shallowUndergroundScanner(event.getChunk());
        deepUndergroundScanner(event.getChunk());
        skyScanner(event.getChunk());
        liquidSurfaceScanner(event.getChunk());

    }

    private void surfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SURFACE).isEmpty()) return;
        //if (!seededSimplexRandomization(chunk, .99, surfaceOffset, DefaultConfig.getSurfaceStructureRarityMultiplier())) return;
        if (ThreadLocalRandom.current().nextDouble() > DefaultConfig.getLandStructuresPerThousandChunks() / 1000D)
            return;
        new FitSurfaceBuilding(chunk);
    }

    private void shallowUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW).isEmpty())
            return;
        //if (!seededSimplexRandomization(chunk, .99, shallowUndergroundOffset, DefaultConfig.getShallowUndergroundStructureRarityMultiplier())) return;
        if (ThreadLocalRandom.current().nextDouble() > DefaultConfig.getShallowUndergroundStructuresPerThousandChunks() / 1000D)
            return;
        FitUndergroundShallowBuilding.fit(chunk);
    }

    private void deepUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_DEEP).isEmpty())
            return;
        //if (!seededSimplexRandomization(chunk, .99, deepUndergroundOffset, DefaultConfig.getDeepUndergroundStructureRarityMultiplier())) return;
        if (ThreadLocalRandom.current().nextDouble() > DefaultConfig.getDeepUndergroundStructuresPerThousandChunks() / 1000D)
            return;
        FitUndergroundDeepBuilding.fit(chunk);
    }

    private void skyScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SKY).isEmpty()) return;
        //if (!seededSimplexRandomization(chunk, .99, airOffset, DefaultConfig.getAirStructureRarityMultiplier())) return;
        if (ThreadLocalRandom.current().nextDouble() > DefaultConfig.getAirStructuresPerThousandChunks() / 1000D)
            return;
        new FitAirBuilding(chunk);
    }

    private void liquidSurfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.LIQUID_SURFACE).isEmpty())
            return;
        // if (!seededSimplexRandomization(chunk, .99, liquidOffset, DefaultConfig.getLiquidSurfaceStructureRarityMultiplier())) return;
        if (ThreadLocalRandom.current().nextDouble() > DefaultConfig.getOceanStructuresPerThousandChunks() / 1000D)
            return;
        new FitLiquidBuilding(chunk);
    }

/* this has issues when you try to integrate customization options
    private boolean seededSimplexRandomization(Chunk chunk, double strictness, int offset, double rarityMultiplier) {
        return (SimplexNoise.noise(rarityMultiplier * chunk.getX() + (double) offset, rarityMultiplier * chunk.getZ() + (double) offset) > strictness);
    }
 */
}
