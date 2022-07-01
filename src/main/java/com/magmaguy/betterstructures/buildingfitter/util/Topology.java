package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;

public class Topology {
    public static double scan(double startingScore, int scanStep, Clipboard schematicClipboard, Location iteratedLocation, Vector schematicOffset) {
        //if (schematicOffset == null) Bukkit.getLogger().info("oops the schematic offset is null");
        double score = startingScore;
        int width = schematicClipboard.getDimensions().getX();
        int depth = schematicClipboard.getDimensions().getZ();

        ArrayList<Integer> heights = new ArrayList<>();

        //Scans the topology to find a mesh of the highest locations for the entirety of the x and z axi. Also does the water / lava scan
        score = scanHighestLocations(width, depth, scanStep, iteratedLocation, schematicOffset, heights, score);
        if (score == 0) return 0;

        //Detects extreme height differences which would immediately disqualify this scan
        score = scanExtremeHeightDifferences(heights, score);
        if (score == 0) return 0;

        //Finds and sets the average height on the topology
        int averageFloorLevel = setToAverageHeight(heights, iteratedLocation);

        //Scores the variation in terrain height
        score = scoreTerrainHeightVariation(heights, averageFloorLevel, score);

        return score;
    }

    private static double scanHighestLocations(int width, int depth, int scanStep, Location iteratedLocation, Vector schematicOffset, ArrayList<Integer> heights, double score) {
        int totalPointAmount = (int) Math.floor(Math.floor(width / (double) scanStep) * Math.floor(depth / (double) scanStep));
        for (int x = 0; x < width; x += scanStep) {
            for (int z = 0; z < depth; z += scanStep) {
                Location projectedLocation = LocationProjector.project(iteratedLocation, new Vector(x, 0, z), schematicOffset);
                projectedLocation = getHighestBlockAt(projectedLocation);
                if (projectedLocation == null) {
                    return 0;
                }
                int safeGuard = 0;
                while (SurfaceMaterials.ignorable(projectedLocation.getBlock().getType())) {
                    if (projectedLocation.getBlock().getType().equals(Material.VOID_AIR)) return 0;
                    projectedLocation.setY(projectedLocation.getY() - 1);
                    safeGuard++;
                    if (safeGuard > 50) {
                        Bukkit.getLogger().warning("Busted the 50 block cap for the tree scanner!");
                        break;
                    }
                }
                switch (projectedLocation.getBlock().getType()) {
                    case WATER:
                    case LAVA:
                        score -= 50 / (double) totalPointAmount;
                }
                if (score < 75)
                    return 0;
                heights.add(projectedLocation.getBlockY());
            }
        }
        return score;
    }

    private static Location getHighestBlockAt(Location location) {
        if (!location.getWorld().getEnvironment().equals(World.Environment.NETHER))
            return location.getWorld().getHighestBlockAt(location).getLocation();
        else {
            //This is middle point for the height in the Nether
            location.setY(63);
            //The nether has specific topology
            if (SurfaceMaterials.ignorable(location.getBlock().getType())) {
                //Basically air for all intents and purposes, scan down
                for (int y = (int) location.getY(); y > 30; y--) {
                    location.setY(y);
                    if (validNetherSurface(location))
                        return location;
                }
            } else {
                //Solid, scan up
                for (int y = (int) location.getY(); y < 100; y++) {
                    location.setY(y);
                    if (validNetherSurface(location))
                        return location;
                }
            }
            return null;
        }
    }

    private static boolean validNetherSurface(Location location) {
        //See if current block is solid and if the one above it is air or similar to air
        if (!(!SurfaceMaterials.ignorable(location.getBlock().getType()) &&
                SurfaceMaterials.ignorable(location.getBlock().getLocation().add(new Vector(0, 1, 0)).getBlock().getType())))
            return false;
        //Scan 10 blocks vertically to make sure they're all air
        for (int i = 1; i < 11; i++) {
            if (SurfaceMaterials.ignorable(location.clone().add(new Vector(0, i, 0)).getBlock().getType()))
                continue;
            return false;
        }
        return true;
    }

    //Checks for extreme height differences and establishes the mesh of heights to be checked later
    private static double scanExtremeHeightDifferences(ArrayList<Integer> heights, double score) {
        //Sort to make math on points faster
        Collections.sort(heights);
        //Check difference between lowest and highest points
        if (Math.abs(heights.get(0) - heights.get(heights.size() - 1)) >= 10) {
            //The schematicOffset between the lowest and highest blocks is too great
            Bukkit.getLogger().info("Exited because of extreme height difference");
            return 0;
        }

        return score;
    }

    //Determines and sets the average height for the paste
    private static int setToAverageHeight(ArrayList<Integer> heights, Location iteratedLocation) {
        //Find the average height
        int averageFloorLevel = 0;
        for (Integer integer : heights) {
            averageFloorLevel += integer;
        }
        averageFloorLevel /= heights.size();

        //Set the new average height, adds 1 because the Y value for the iterated location is actually the first air block above the ground
        iteratedLocation.setY(averageFloorLevel + 1D);
        return averageFloorLevel;
    }

    //Scores the terrain variation, less extreme is better
    private static double scoreTerrainHeightVariation(ArrayList<Integer> heights, int averageFloorLevel, double score) {
        //Score the difference between the average height and the heights of each individual location
        for (Integer integer : heights) {
            int difference = Math.abs(averageFloorLevel - integer);
            if (difference < 3) continue;
            //Max impact is 50% of the starting score divided by each point in the search
            double maxImpact = score / 2D / heights.size();
            //Calculate the score of this specific point, exponential formula
            double currentHeightScore = (1 - Math.pow(difference, 2) * 4 / 100) * maxImpact;
            score -= currentHeightScore;
            if (score < 85)
                return 0;
        }
        return score;
    }
}
