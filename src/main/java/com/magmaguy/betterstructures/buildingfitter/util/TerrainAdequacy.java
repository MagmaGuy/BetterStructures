package com.magmaguy.betterstructures.buildingfitter.util;

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
        AIR
    }

    public static double scan(int scanStep, Clipboard schematicClipboard, Location iteratedLocation, Vector schematicOffset, double score, ScanType scanType) {
        int width = schematicClipboard.getDimensions().getX();
        int depth = schematicClipboard.getDimensions().getZ();
        int height = schematicClipboard.getDimensions().getY();
        //Check if surface air blocks are getting replaced with air blocks and if underground block are solid
        int totalCount = (int) (Math.floor(width / 3D) + Math.floor(height / 3D) + Math.floor(depth / 3D));

        for (int x = 0; x < width; x += scanStep) {
            for (int y = 0; y < height; y += scanStep) {
                for (int z = 0; z < depth; z += scanStep) {
                    Material schematicMaterialAtPosition = BukkitAdapter.adapt(schematicClipboard.getBlock(BlockVector3.at(x, y, z)).getBlockType());
                    Location projectedLocation = LocationProjector.project(iteratedLocation, new Vector(x, y, z), schematicOffset);
                    if (!isBlockAdequate(projectedLocation, schematicMaterialAtPosition, iteratedLocation.getBlockY() - 1, scanType))
                        score -= 50 / (double) totalCount;
                    if (score < 30)
                        return 0;
                }
            }
        }
        return score;
    }

    private static boolean isBlockAdequate(Location projectedWorldLocation, Material schematicBlockMaterial, int floorHeight, ScanType scanType) {
        int floorYValue = projectedWorldLocation.getBlockY();
        if (projectedWorldLocation.getBlock().getType().equals(Material.VOID_AIR)) return false;
        switch (scanType) {
            case SURFACE:
                if (floorYValue > floorHeight)
                    //for air level
                    return !projectedWorldLocation.getBlock().getType().isAir() || schematicBlockMaterial.isAir();
                else
                    //for underground level
                    return !projectedWorldLocation.getBlock().getType().isAir();
            case AIR:
                return projectedWorldLocation.getBlock().getType().isAir();
            case UNDERGROUND:
                return !projectedWorldLocation.getBlock().getType().isAir();
            default:
                return false;
        }

    }
}
