package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.TerrainAdequacy;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class FitUndergroundBuilding extends FitAnything {

    private int lowestY;
    private int highestY;

    //For commands
    public FitUndergroundBuilding(Chunk chunk, SchematicContainer schematicContainer, int lowestY, int highestY) {
        this.lowestY = lowestY;
        this.highestY = highestY;
        this.schematicContainer = schematicContainer;
        this.schematicClipboard = schematicContainer.getClipboard();
        scan(chunk);
    }

    public FitUndergroundBuilding(Chunk chunk, int lowestY, int highestY) {
        this.lowestY = lowestY;
        this.highestY = highestY;
        scan(chunk);
    }

    private void scan(Chunk chunk) {
        //Note about the adjustments:
        //The 8 offset on x and y is to center the anchor on the chunk
        Location originalLocation = new Location(chunk.getWorld(), chunk.getX() * 16D, 0, chunk.getZ() * 16D).add(new Vector(8, 0, 8));
        originalLocation.setY(ThreadLocalRandom.current().nextInt(lowestY, highestY));
        setSchematicFilename(originalLocation, GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW);
        if (schematicClipboard == null) {
            //Bukkit.getLogger().info("Did not spawn structure in biome " + originalLocation.getBlock().getBiome() + " because no valid schematics exist for it.");
            return;
        }

        schematicOffset = WorldEditUtils.getSchematicOffset(schematicClipboard);
        for (int chunkX = -searchRadius; chunkX < searchRadius + 1; chunkX += 4) {
            for (int chunkZ = -searchRadius; chunkZ < searchRadius + 1; chunkZ += 4) {
                chunkScan(originalLocation, chunkX, chunkZ);
            }
        }

        if (location == null)
            return;

        paste(location);
    }

    private void chunkScan(Location originalLocation, int chunkX, int chunkZ) {
        Location iteratedLocation = originalLocation.clone().add(new Vector(chunkX * 16, originalLocation.getY(), chunkZ * 16));
        double score = TerrainAdequacy.scan(scanStep, schematicClipboard, iteratedLocation, schematicOffset, super.startingScore, TerrainAdequacy.ScanType.SURFACE);
        if (score < 80) {
            //Bukkit.getLogger().info("Did not spawn underground building because there was too much air");
            return;
        }

        if (score > highestScore) {
            highestScore = score;
            location = iteratedLocation;
        }

        if (score > 90) {
            //Bukkit.getLogger().info("Perfect fit! " + iteratedLocation);
            location = iteratedLocation;
        }
    }
}
