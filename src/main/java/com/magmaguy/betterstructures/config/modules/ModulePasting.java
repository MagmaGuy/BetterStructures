package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.GridCell;
import com.magmaguy.betterstructures.util.distributedload.WorkloadRunnable;
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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;

import java.util.ArrayList;
import java.util.List;

public final class ModulePasting {
    private ModulePasting() {
    }

    private static List<PasteMe> generatePasteMeList(Clipboard clipboard, Location location, Integer rotation) {
        List<PasteMe> pasteMeList = new ArrayList<>();
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

        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        Clipboard finalTransformedClipboard = transformedClipboard;
        BlockVector3 finalOrigin = origin;
        transformedClipboard.getRegion().forEach(blockPos -> {
            BlockState blockState = finalTransformedClipboard.getBlock(blockPos);
            BlockData blockData = Bukkit.createBlockData(blockState.getAsString());
            if (rotation != 0)
                blockData.rotate(switch (rotation) {
                    case 90 -> StructureRotation.CLOCKWISE_90;
                    case 180 -> StructureRotation.CLOCKWISE_180;
                    case 270 -> StructureRotation.COUNTERCLOCKWISE_90;
                    default -> throw new IllegalStateException("Unexpected value: " + rotation);
                });

            int worldX = baseX + (blockPos.x() - finalOrigin.x());
            int worldY = baseY + (blockPos.y() - finalOrigin.y());
            int worldZ = baseZ + (blockPos.z() - finalOrigin.z());

            Location pasteLocation = new Location(world, worldX, worldY, worldZ);
            pasteMeList.add(new PasteMe(pasteLocation, blockData));
        });
        return pasteMeList;
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
        List<PasteMe> pasteMeList = new ArrayList<>();
        for (GridCell gridCell : gridCellList) {
            if (gridCell == null || gridCell.getModulesContainer() == null) continue;
            Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
            if (clipboard == null) continue;
            pasteMeList.addAll(generatePasteMeList(clipboard, gridCell.getRealLocation(), gridCell.getModulesContainer().getRotation()));
        }

        List<PasteMe> lightEmitters = new ArrayList<>();
        WorkloadRunnable pasteMeRunnable = new WorkloadRunnable(.2, () -> {
            WorkloadRunnable lightRunnable = new WorkloadRunnable(.2, () -> {
            });
            for (PasteMe lightEmitter : lightEmitters)
                lightRunnable.addWorkload(() -> lightEmitter.location.getBlock().setBlockData(lightEmitter.blockData));
            lightRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        });
        for (PasteMe pasteMe : pasteMeList) {
            if (pasteMe.blockData.getLightEmission() > 0)
                lightEmitters.add(pasteMe);
            else
                pasteMeRunnable.addWorkload(() -> {
                    NMSManager.getAdapter().setBlockInNativeDataPalette(
                            pasteMe.location.getWorld(),
                            pasteMe.location.getBlockX(),
                            pasteMe.location.getBlockY(),
                            pasteMe.location.getBlockZ(),
                            pasteMe.blockData,
                            true);
                });
        }
        pasteMeRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
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

    private record PasteMe(Location location, BlockData blockData) {
    }
}