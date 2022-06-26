package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.TerrainAdequacy;
import com.magmaguy.betterstructures.buildingfitter.util.Topology;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class FitSurfaceBuilding extends FitAnything {


    //For commands
    public FitSurfaceBuilding(Chunk chunk, SchematicContainer schematicContainer) {
        this.schematicContainer = schematicContainer;
        this.schematicClipboard = schematicContainer.getClipboard();
        scan(chunk);
    }

    public FitSurfaceBuilding(Chunk chunk) {
        scan(chunk);
    }

    private void scan(Chunk chunk) {
        //Note about the adjustments:
        //The 8 offset on x and y is to center the anchor on the chunk
        Location originalLocation = new Location(chunk.getWorld(), chunk.getX() * 16D, 0, chunk.getZ() * 16D).add(new Vector(8, 0, 8));
        originalLocation.setY(originalLocation.getWorld().getHighestBlockYAt(originalLocation));
        setSchematicFilename(originalLocation, GeneratorConfigFields.StructureType.SURFACE);
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

        if (location == null) {
            //Bukkit.broadcastMessage("Yo your locations are whack!");
            return;
        }

        //Bukkit.broadcastMessage("Fit with score = " + highestScore);

        super.paste(location);
    }

    private void chunkScan(Location originalLocation, int chunkX, int chunkZ) {
        Location iteratedLocation = originalLocation.clone().add(new Vector(chunkX * 16, 0, chunkZ * 16));

        double score = Topology.scan(startingScore, scanStep, schematicClipboard, iteratedLocation, schematicOffset);

        //Continue to the next scan in case of poor fit
        if (score == 0) {
            //Bukkit.getLogger().info("Exited because of scoring of individual points");
            return;
        }

        score = TerrainAdequacy.scan(scanStep, schematicClipboard, iteratedLocation, schematicOffset, score, TerrainAdequacy.ScanType.SURFACE);

        if (score == 0) {
            //Bukkit.getLogger().info("Exited because ground or surface fit was bad");
            return;
        }

        if (score > highestScore) {
            highestScore = score;
            location = iteratedLocation;
        }

        if (score > 50) {
            //Bukkit.broadcastMessage("Perfect fit! " + iteratedLocation.toString());
            location = iteratedLocation;
        }
    }

}
