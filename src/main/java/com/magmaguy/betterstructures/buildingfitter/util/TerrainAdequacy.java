package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public class TerrainAdequacy {
    public enum ScanType {
        SURFACE,
        UNDERGROUND,
        AIR,
        LIQUID
    }

    public static double scan(int scanStep, Clipboard schematicClipboard, Location iteratedLocation, Vector schematicOffset, ScanType scanType) {
        int width = schematicClipboard.getDimensions().getX();
        int depth = schematicClipboard.getDimensions().getZ();
        int height = schematicClipboard.getDimensions().getY();

        int totalCount = 0;
        int negativeCount = 0;

        for (int x = 0; x < width; x += scanStep) {
            for (int y = 0; y < height; y += scanStep) {
                for (int z = 0; z < depth; z += scanStep) {
                    Material schematicMaterialAtPosition = BukkitAdapter.adapt(schematicClipboard.getBlock(BlockVector3.at(x, y, z)).getBlockType());
                    Location projectedLocation = LocationProjector.project(iteratedLocation, new Vector(x, y, z), schematicOffset);
                    if (!isBlockAdequate(projectedLocation, schematicMaterialAtPosition, iteratedLocation.getBlockY() - 1, scanType))
                        negativeCount++;
                    totalCount++;
                }
            }
        }

        double score = 100 - negativeCount * 100D / (double) totalCount;

        return score;
    }

    private static boolean isBlockAdequate(Location projectedWorldLocation, Material schematicBlockMaterial, int floorHeight, ScanType scanType) {
        int floorYValue = projectedWorldLocation.getBlockY();
        if (projectedWorldLocation.getBlock().getType().equals(Material.VOID_AIR)) return false;
        switch (scanType) {
            case SURFACE:
                if (floorYValue > floorHeight)
                    //for air level
                    return SurfaceMaterials.ignorable(projectedWorldLocation.getBlock().getType()) || !schematicBlockMaterial.isAir();
                else
                    //for underground level
                    return !projectedWorldLocation.getBlock().getType().isAir();
            case AIR:
                return projectedWorldLocation.getBlock().getType().isAir();
            case UNDERGROUND:
                return projectedWorldLocation.getBlock().getType().isSolid();
            case LIQUID:
                if (floorYValue > floorHeight) {
                    //for air level
                    return projectedWorldLocation.getBlock().getType().isAir();
                } else {
                    //for underwater level
                    return projectedWorldLocation.getBlock().isLiquid();
                }
            default:
                return false;
        }

    }
}
