package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAirBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitLiquidBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitSurfaceBuilding;
import com.magmaguy.betterstructures.buildingfitter.FitUndergroundShallowBuilding;
import com.magmaguy.betterstructures.buildingfitter.util.FitUndergroundDeepBuilding;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.SimplexNoise;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Random;

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
        Bukkit.getScheduler().runTaskAsynchronously(MetadataHandler.PLUGIN, bukkitTask -> {
            if (!ValidWorldsConfig.getValidWorlds().get(event.getWorld())) return;
            if (random == null) {
                random = new Random(event.getChunk().getWorld().getSeed());
                surfaceOffset = random.nextInt(1, 1000000);
                shallowUndergroundOffset = random.nextInt(1, 100000);
                deepUndergroundOffset = random.nextInt(1, 10000);
                airOffset = random.nextInt(1, 1000);
                liquidOffset = random.nextInt(1, 100);
            }
            surfaceScanner(event.getChunk());
            shallowUndergroundScanner(event.getChunk());
            deepUndergroundScanner(event.getChunk());
            skyScanner(event.getChunk());
            liquidSurfaceScanner(event.getChunk());
        } );
    }

    private void surfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SURFACE).isEmpty()) return;
        if (!seededSimplexRandomization(chunk, 0.95, surfaceOffset)) return;
        new FitSurfaceBuilding(chunk);
    }

    private void shallowUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW).isEmpty())
            return;
        if (!seededSimplexRandomization(chunk, 0.95, shallowUndergroundOffset)) return;
        FitUndergroundShallowBuilding.fit(chunk);
    }

    private void deepUndergroundScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.UNDERGROUND_DEEP).isEmpty())
            return;
        if (!seededSimplexRandomization(chunk, 0.95, deepUndergroundOffset)) return;
        FitUndergroundDeepBuilding.fit(chunk);
    }

    private void skyScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.SKY).isEmpty()) return;
        if (!seededSimplexRandomization(chunk, 0.99, airOffset)) return;
        new FitAirBuilding(chunk);
    }

    private void liquidSurfaceScanner(Chunk chunk) {
        if (SchematicContainer.getSchematics().get(GeneratorConfigFields.StructureType.LIQUID_SURFACE).isEmpty())
            return;
        if (!seededSimplexRandomization(chunk, 0.99, liquidOffset))
            return;
        new FitLiquidBuilding(chunk);
    }


    private boolean seededSimplexRandomization(Chunk chunk, double strictness, int offset) {
        return (SimplexNoise.noise(chunk.getX() + (double) offset, chunk.getZ() + (double) offset) > strictness);
    }

}
