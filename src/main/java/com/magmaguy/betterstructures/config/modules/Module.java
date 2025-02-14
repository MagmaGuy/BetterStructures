package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.GridCell;
import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.internal.util.ClipboardTransformBaker;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Module {
    private Module() {
    }

    public static void testPaste(Clipboard clipboard, Location location, Integer rotation) {
        org.bukkit.World world = location.getWorld();

//        rotation = 0;

        // Apply rotation to the clipboard
        try {
            clipboard = ClipboardTransformBaker.bakeTransform(clipboard, new AffineTransform().rotateY(normalizeRotation(rotation)));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        // Get the origin and minimum point of the transformed clipboard
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 clipboardMin = clipboard.getMinimumPoint();
        origin = new BlockVector3(clipboardMin.x(), origin.y(), clipboardMin.z());
        BlockVector3 clipboardDimensions = clipboard.getDimensions();

        List<LightEmitters> lightEmitters = new ArrayList<>();
        HashSet<Chunk> chunks = new HashSet<>();

        for (int x = 0; x < clipboardDimensions.getX(); x++) {
            for (int y = 0; y < clipboardDimensions.getY(); y++) {
                for (int z = 0; z < clipboardDimensions.getZ(); z++) {
                    // Current block position in the clipboard
                    BlockVector3 blockPos = clipboardMin.add(x, y, z);

                    // Calculate world coordinates adjusted for the origin
                    int worldX = location.getBlockX() + (blockPos.getX() - origin.getX());
                    int worldY = location.getBlockY() + (blockPos.getY() - origin.getY());
                    int worldZ = location.getBlockZ() + (blockPos.getZ() - origin.getZ());

                    BlockState blockState = clipboard.getBlock(blockPos);
                    BlockData blockData = Bukkit.createBlockData(blockState.getAsString());

                    Location pasteLoc = new Location(world, worldX, worldY, worldZ);
                    chunks.add(pasteLoc.getChunk());

                    if (blockData.getLightEmission() > 0)
                        lightEmitters.add(new LightEmitters(pasteLoc, blockData));
                    else
                        NMSManager.getAdapter().setBlockInNativeDataPalette(world, worldX, worldY, worldZ, blockData, true);
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                lightEmitters.forEach(emitter -> {
                    emitter.location.getBlock().setBlockData(emitter.blockData);
                });
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20);
    }

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        if (rotation == null) {
            Logger.debug("rotation was null at the time of the paste, this will not do");
            return;
        }

        World world = BukkitAdapter.adapt(location.getWorld());
        Location adjustedLocation = adjustLocationForRotation(location, rotation, clipboard);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            editSession.setTrackingHistory(false);
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(new AffineTransform().rotateY(normalizeRotation(rotation)));

            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(adjustedLocation.getX(), adjustedLocation.getY(), adjustedLocation.getZ()))
                    .build();
            editSession.setSideEffectApplier(SideEffectSet.none());
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public static void batchPaste(List<GridCell> gridCellList) {
        HashSet<Chunk> chunks = new HashSet<>();

        for (GridCell gridCell : gridCellList) {
            if (gridCell == null || gridCell.getModulesContainer() == null ||
                    gridCell.getModulesContainer().getClipboard() == null) {
                continue;
            }

            chunks.add(gridCell.getRealLocation().getChunk());

            Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
            int rotation = gridCell.getModulesContainer().getRotation();
//            Location location = gridCell.getRealLocation().add(-1, 0, -1);

//            Location baseLocation = gridCell.getRealLocation().add(-1, 0, -1);
            Location baseLocation = gridCell.getRealLocation();
            Location adjustedLocation = adjustLocationForRotation(baseLocation, rotation, clipboard);

//            testPaste(clipboard, adjustedLocation, rotation);
            testPaste(clipboard, baseLocation, rotation);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                chunks.forEach(chunk -> chunk.unload(true));
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

//    private static void processSingleCell(GridCell gridCell, EditSession editSession) {
//        Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
//        int rotation = gridCell.getModulesContainer().getRotation();
//        Location baseLocation = gridCell.getRealLocation().add(-1, 0, -1);
//        Location adjustedLocation = adjustLocationForRotation(baseLocation, rotation, clipboard);
//
//        ClipboardHolder holder = new ClipboardHolder(clipboard);
//        holder.setTransform(new AffineTransform().rotateY(normalizeRotation(rotation)));
//
//        Operation operation = holder
//                .createPaste(editSession)
//                .ignoreAirBlocks(true)
//                .to(BlockVector3.at(adjustedLocation.getX(), adjustedLocation.getY(), adjustedLocation.getZ()))
//                .build();
//
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                try {
//                    Operations.complete(operation);
//                } catch (WorldEditException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }.runTask(MetadataHandler.PLUGIN);
//    }

    /**
     * Adjusts the paste location based on rotation and clipboard dimensions.
     * Accounts for variable chunk sizes.
     */
    private static Location adjustLocationForRotation(Location location, int rotation, Clipboard clipboard) {
        Location adjustedLocation = location.clone();

        // Get clipboard dimensions
        BlockVector3 dimensions = clipboard.getDimensions();
        int width = dimensions.getX();
        int length = dimensions.getZ();

        // Normalize rotation to match WorldEdit's coordinate system
        rotation = normalizeRotation(rotation);

        // Adjust position based on rotation and dimensions
        switch (rotation) {
            case 0:
                // No adjustment needed for 0 degrees
                break;
            case 90:
                adjustedLocation.add(0, 0, -width);
                break;
            case 180:
                adjustedLocation.add(-width, 0, length);
                break;
            case 270:
                adjustedLocation.add(length, 0, 0);
                break;
            default:
                Logger.warn("Invalid rotation angle: " + rotation);
        }

        return adjustedLocation;
    }

    /**
     * Normalizes rotation angles to match WorldEdit's coordinate system.
     * WorldEdit uses counterclockwise rotation while our system uses clockwise.
     */
    private static int normalizeRotation(int rotation) {
        // Convert clockwise to counterclockwise
        return (360 - rotation) % 360;
    }

    private record LightEmitters(Location location, BlockData blockData) {
    }
}