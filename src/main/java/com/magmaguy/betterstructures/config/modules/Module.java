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
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Module {
    private Module() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void testPaste(Clipboard clipboard, Location location, Integer rotation) {
        AffineTransform transform = new AffineTransform().rotateY(normalizeRotation(rotation));
        Clipboard transformedClipboard = null;
        try {
            transformedClipboard = clipboard.transform(transform);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        BlockVector3 origin = transformedClipboard.getOrigin();
        BlockVector3 minPoint = transformedClipboard.getMinimumPoint();
        origin = BlockVector3.at(minPoint.x(), origin.y(), minPoint.z());

        List<LightEmitter> lightEmitters = new ArrayList<>();
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        Clipboard finalTransformedClipboard = transformedClipboard;
        BlockVector3 finalOrigin = origin;
        transformedClipboard.getRegion().forEach(blockPos -> {
            BlockState blockState = finalTransformedClipboard.getBlock(blockPos);
            BlockData blockData = Bukkit.createBlockData(blockState.getAsString());

            int worldX = baseX + (blockPos.x() - finalOrigin.x());
            int worldY = baseY + (blockPos.y() - finalOrigin.y());
            int worldZ = baseZ + (blockPos.z() - finalOrigin.z());

            Location pasteLocation = new Location(world, worldX, worldY, worldZ);

            if (blockData.getLightEmission() > 0) {
                lightEmitters.add(new LightEmitter(pasteLocation, blockData));
            } else {
                NMSManager.getAdapter().setBlockInNativeDataPalette(world, worldX, worldY, worldZ, blockData, true);
            }
        });

        scheduleLightUpdates(lightEmitters);
    }

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        if (rotation == null) {
            Logger.debug("Rotation was null during paste operation");
            return;
        }

        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(location.getWorld());
        Location adjustedLocation = adjustLocationForRotation(location, rotation, clipboard);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
            editSession.setTrackingHistory(false);
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(createRotationTransform(rotation));

            Operation operation = holder.createPaste(editSession)
                    .to(BlockVector3.at(adjustedLocation.getX(), adjustedLocation.getY(), adjustedLocation.getZ()))
                    .build();

            editSession.setSideEffectApplier(SideEffectSet.none());
            Operations.complete(operation);
        } catch (WorldEditException e) {
            Logger.warn("Failed to paste structure: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void batchPaste(List<GridCell> gridCellList) {
//        Set<Chunk> affectedChunks = new HashSet<>();

        for (GridCell gridCell : gridCellList) {
            if (gridCell == null || gridCell.getModulesContainer() == null) continue;

            Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
            if (clipboard == null) continue;

//            affectedChunks.add(gridCell.getRealLocation().getChunk());
            testPaste(clipboard, gridCell.getRealLocation(), gridCell.getModulesContainer().getRotation());
        }

//        scheduleChunkUnload(affectedChunks);
    }

    private static AffineTransform createRotationTransform(int rotation) {
        return new AffineTransform().rotateY(normalizeRotation(rotation));
    }

    private static Location adjustLocationForRotation(Location location, int rotation, Clipboard clipboard) {
        Location adjustedLocation = location.clone();
        BlockVector3 dimensions = clipboard.getDimensions();

        switch (normalizeRotation(rotation)) {
            case 90 -> adjustedLocation.add(0, 0, -dimensions.x());
            case 180 -> adjustedLocation.add(-dimensions.x(), 0, dimensions.z());
            case 270 -> adjustedLocation.add(dimensions.z(), 0, 0);
            case 0 -> {
            } // No adjustment needed
            default -> Logger.warn("Invalid rotation angle: " + rotation);
        }

        return adjustedLocation;
    }

    private static int normalizeRotation(int rotation) {
        return (360 - rotation) % 360;
    }

    private static void scheduleLightUpdates(List<LightEmitter> lightEmitters) {
        new BukkitRunnable() {
            @Override
            public void run() {
                lightEmitters.forEach(emitter -> emitter.location.getBlock().setBlockData(emitter.blockData));
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 1);
    }

    private static void scheduleChunkUnload(Set<Chunk> chunks) {
                chunks.forEach(chunk -> chunk.unload(true));
    }

    private record LightEmitter(Location location, BlockData blockData) {
    }
}