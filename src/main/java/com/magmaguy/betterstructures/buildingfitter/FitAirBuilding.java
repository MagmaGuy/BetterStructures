package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.TerrainAdequacy;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class FitAirBuilding extends FitAnything {

    public FitAirBuilding(Chunk chunk, SchematicContainer schematicContainer) {
        super(schematicContainer);
        super.structureType = GeneratorConfigFields.StructureType.SKY;
        this.schematicContainer = schematicContainer;
        this.schematicClipboard = schematicContainer.getClipboard();
        scan(chunk);
    }

    public FitAirBuilding(Chunk chunk) {
        super.structureType = GeneratorConfigFields.StructureType.SKY;
        scan(chunk);
    }

    private void scan(Chunk chunk) {
        //The 8 offset on x and y is to center the anchor on the chunk, the system adds 100 blocks
        int altitude = 0;
        switch (chunk.getWorld().getEnvironment()) {
            case NORMAL:
            case CUSTOM:
                altitude = ThreadLocalRandom.current().nextInt(DefaultConfig.getNormalCustomAirBuildingMinAltitude(), DefaultConfig.getNormalCustomAirBuildingMaxAltitude() + 1);
                break;
            case NETHER:
                //this is dealt with later
                altitude = 0;
                break;
            case THE_END:
                altitude = ThreadLocalRandom.current().nextInt(DefaultConfig.getEndAirBuildMinAltitude(), DefaultConfig.getEndAirBuildMinAltitude() + 1);
                break;
        }
        Location originalLocation = chunk.getWorld().getHighestBlockAt(chunk.getX() * 16 + 8, chunk.getZ() * 16 + 8).getLocation().add(new Vector(0, altitude, 0));

        switch (chunk.getWorld().getEnvironment()) {
            case CUSTOM:
            case NORMAL:
                break;
            case NETHER:
                boolean streak = false;
                int lowestY = 45;
                int highestY = 100;
                int lowPoint = 0;
                int highPoint = 0;
                int tolerance = 3;
                for (int y = lowestY; y < highestY; y++) {
                    Location currentLocation = originalLocation.clone();
                    currentLocation.setY(y);
                    if (currentLocation.getBlock().getType().isAir()) {
                        if (streak) {
                            highPoint = y;
                        } else {
                            lowPoint = y;
                            streak = true;
                        }
                    } else {
                        if (currentLocation.getBlock().getType() == Material.VOID_AIR ||
                                currentLocation.getBlock().getType() == Material.BEDROCK ||
                                tolerance == 0) {
                            if (streak) {
                                streak = false;
                                if (highPoint - lowPoint >= 40)
                                    break;
                                if (currentLocation.getBlock().getType() == Material.VOID_AIR ||
                                        currentLocation.getBlock().getType() == Material.BEDROCK)
                                    return;
                                tolerance = 3;
                            }
                        } else {
                            if (streak) {
                                tolerance--;
                                highPoint = y;
                            }
                        }
                    }
                }
                if (highPoint - lowPoint < 20) {
                    //Case in which no ground was found which could be used as a valid underground surface
                    return;
                }
                if (highPoint - lowPoint > 30)
                    originalLocation.setY(ThreadLocalRandom.current().nextInt(lowPoint + 1, highPoint - 20));
                else
                    originalLocation.setY(lowPoint + 1D);

            case THE_END:
                //todo: might want to add specific handling for this one
                break;
        }

        randomizeSchematicContainer(originalLocation, GeneratorConfigFields.StructureType.SKY);
        if (schematicClipboard == null) {
            //Bukkit.getLogger().info("Did not spawn structure in biome " + originalLocation.getBlock().getBiome() + " because no valid schematics exist for it.");
            return;
        }
        schematicOffset = WorldEditUtils.getSchematicOffset(schematicClipboard);

        chunkScan(originalLocation, 0, 0);
        if (location == null)
            for (int chunkX = -searchRadius; chunkX < searchRadius + 1; chunkX++) {
                for (int chunkZ = -searchRadius; chunkZ < searchRadius + 1; chunkZ++) {
                    if (chunkX == 0 && chunkZ == 0) continue;
                    chunkScan(originalLocation, chunkX, chunkZ);
                    if (location != null) break;
                }
                if (location != null) break;
            }
        if (location == null) {
            //Bukkit.broadcastMessage("Yo your locations are whack!");
            return;
        }

        paste(location);
    }

    private void chunkScan(Location originalLocation, int chunkX, int chunkZ) {
        Location iteratedLocation = originalLocation.clone().add(new Vector(chunkX * 16, 0, chunkZ * 16));
        double newScore = TerrainAdequacy.scan(scanStep, schematicClipboard, iteratedLocation, schematicOffset, TerrainAdequacy.ScanType.AIR);
        if (newScore == startingScore) location = iteratedLocation;
    }
}
