package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.modules.GridCell;
import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;

public class Module {
    private Module() {
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

    public static void batchPaste(List<GridCell> gridCellList, org.bukkit.World world) {
        World worldEditWorld = BukkitAdapter.adapt(world);
        EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld);
        editSession.setTrackingHistory(false);

        HashSet<Chunk> chunks = new HashSet<>();

        for (GridCell gridCell : gridCellList) {
            if (gridCell == null || gridCell.getModulesContainer() == null ||
                    gridCell.getModulesContainer().getClipboard() == null) {
                continue;
            }

            chunks.add(gridCell.getRealLocation().getChunk());
            processSingleCell(gridCell, editSession);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                chunks.forEach(chunk -> chunk.unload(true));
                editSession.close();
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    private static void processSingleCell(GridCell gridCell, EditSession editSession) {
        Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
        int rotation = gridCell.getModulesContainer().getRotation();
        Location baseLocation = gridCell.getRealLocation().add(-1, 0, -1);
        Location adjustedLocation = adjustLocationForRotation(baseLocation, rotation, clipboard);

        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(new AffineTransform().rotateY(normalizeRotation(rotation)));

        Operation operation = holder
                .createPaste(editSession)
                .ignoreAirBlocks(true)
                .to(BlockVector3.at(adjustedLocation.getX(), adjustedLocation.getY(), adjustedLocation.getZ()))
                .build();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Operations.complete(operation);
                } catch (WorldEditException e) {
                    throw new RuntimeException(e);
                }
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

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
                adjustedLocation.add(0, 0, width);
                break;
            case 180:
                adjustedLocation.add(width, 0, length);
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
}