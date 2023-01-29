package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.TerrainAdequacy;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public class FitLiquidBuilding extends FitAnything {

    //For commands
    public FitLiquidBuilding(Chunk chunk, SchematicContainer schematicContainer) {
        super.structureType = GeneratorConfigFields.StructureType.LIQUID_SURFACE;
        this.schematicContainer = schematicContainer;
        this.schematicClipboard = schematicContainer.getClipboard();
        scan(chunk);
    }

    public FitLiquidBuilding(Chunk chunk) {
        super.structureType = GeneratorConfigFields.StructureType.LIQUID_SURFACE;
        scan(chunk);
    }

    private void scan(Chunk chunk) {
        //Note about the adjustments:
        //The 8 offset on x and y is to center the anchor on the chunk
        Location originalLocation = new Location(chunk.getWorld(), chunk.getX() * 16D, 0, chunk.getZ() * 16D).add(new Vector(8, 0, 8));
        //This gets the location of the highest solid block
        originalLocation.setY(originalLocation.getWorld().getHighestBlockYAt(originalLocation));

        switch (chunk.getWorld().getEnvironment()) {
            case CUSTOM:
            case NORMAL:
                if (!originalLocation.getBlock().isLiquid()) return;
                break;
            case NETHER:
                int netherLavaOceanHeight = 31;
                originalLocation.setY(netherLavaOceanHeight);
                if (originalLocation.getBlock().getType() != Material.LAVA) {
                    return;
                }
                for (int i = 1; i < 20; i++)
                    if (!originalLocation.clone().add(new Vector(0, i, 0)).getBlock().getType().isAir()) {
                        return;
                    }
        }

        setSchematicFilename(originalLocation, GeneratorConfigFields.StructureType.LIQUID_SURFACE);
        if (schematicClipboard == null) {
            //Bukkit.getLogger().info("Did not spawn structure in biome " + originalLocation.getBlock().getBiome() + " because no valid schematics exist for it.");
            return;
        }
        schematicOffset = WorldEditUtils.getSchematicOffset(schematicClipboard);

        chunkScan(originalLocation, 0, 0);
        if (highestScore < 90)
            for (int chunkX = -searchRadius; chunkX < searchRadius + 1; chunkX++) {
                for (int chunkZ = -searchRadius; chunkZ < searchRadius + 1; chunkZ++) {
                    if (chunkX == 0 && chunkZ == 0) continue;
                    chunkScan(originalLocation, chunkX, chunkZ);
                    if (highestScore >= 90) break;
                }
                if (highestScore >= 90) break;
            }

        if (location == null) {
            //Bukkit.broadcastMessage("Yo your locations are whack!");
            return;
        }

        super.paste(location);
    }

    private void chunkScan(Location originalLocation, int chunkX, int chunkZ) {
        Location iteratedLocation = originalLocation.clone().add(new Vector(chunkX * 16, 1, chunkZ * 16));
        double newScore = TerrainAdequacy.scan(scanStep, schematicClipboard, iteratedLocation, schematicOffset, TerrainAdequacy.ScanType.LIQUID);
        if (newScore < 90) return;
        if (newScore == startingScore) {
            highestScore = newScore;
            location = iteratedLocation;
        }
    }

}
