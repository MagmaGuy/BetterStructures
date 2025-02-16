package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.modules.GridCell;
import com.magmaguy.betterstructures.util.WorldEditUtils;
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
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Sign;

import java.util.ArrayList;
import java.util.List;

public final class ModulePasting {
    private final List<InterpretedSign> interpretedSigns = new ArrayList<>();

    public ModulePasting(World world, List<GridCell> gridCellList) {
        batchPaste(gridCellList, interpretedSigns);
        createModularWorld(world);
    }

    private static List<Pasteable> generatePasteMeList(Clipboard clipboard, Location worldPasteOriginLocation, Integer rotation, List<InterpretedSign> interpretedSigns) {
        List<Pasteable> pasteableList = new ArrayList<>();
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

        World world = worldPasteOriginLocation.getWorld();
        int baseX = worldPasteOriginLocation.getBlockX();
        int baseY = worldPasteOriginLocation.getBlockY();
        int baseZ = worldPasteOriginLocation.getBlockZ();

        Clipboard finalTransformedClipboard = transformedClipboard;
        BlockVector3 finalOrigin = origin;
        transformedClipboard.getRegion().forEach(blockPos -> {
//            BlockState blockState = finalTransformedClipboard.getBlock(blockPos);
//            BlockData blockData = Bukkit.createBlockData(blockState.getAsString());

            BaseBlock baseBlock = finalTransformedClipboard.getFullBlock(blockPos);
            BlockData blockData = Bukkit.createBlockData(baseBlock.toImmutableState().getAsString());

            int worldX = baseX + (blockPos.x() - finalOrigin.x());
            int worldY = baseY + (blockPos.y() - finalOrigin.y());
            int worldZ = baseZ + (blockPos.z() - finalOrigin.z());

            Location pasteLocation = new Location(world, worldX, worldY, worldZ);
            pasteableList.add(new Pasteable(pasteLocation, blockData));

            if (blockData.getMaterial().toString().toLowerCase().contains("sign")) {
                interpretedSigns.add(new InterpretedSign(pasteLocation, WorldEditUtils.getLines(baseBlock)));
            }
        });
        return pasteableList;
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

    public static List<InterpretedSign> batchPaste(List<GridCell> gridCellList, List<InterpretedSign> interpretedSigns) {
        List<Pasteable> pasteableList = new ArrayList<>();
        for (GridCell gridCell : gridCellList) {
            if (gridCell == null || gridCell.getModulesContainer() == null) continue;
            Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
            if (clipboard == null) continue;
            pasteableList.addAll(generatePasteMeList(clipboard, gridCell.getRealLocation(), gridCell.getModulesContainer().getRotation(), interpretedSigns));
        }

        List<Pasteable> lightEmitters = new ArrayList<>();
        WorkloadRunnable pasteMeRunnable = new WorkloadRunnable(.2, () -> {
            WorkloadRunnable lightRunnable = new WorkloadRunnable(.2, () -> {
            });
            for (Pasteable lightEmitter : lightEmitters)
                lightRunnable.addWorkload(() -> lightEmitter.location.getBlock().setBlockData(lightEmitter.blockData));
            lightRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        });

        List<InterpretedSign> freshlyInterpretedSigns = new ArrayList<>();

        for (Pasteable pasteable : pasteableList) {
            //check for signs first
            if (pasteable.blockData.getLightEmission() > 0 || pasteable.blockData instanceof Directional || pasteable.blockData instanceof Rail || pasteable.blockData instanceof Sign)
                lightEmitters.add(pasteable);
            else
                pasteMeRunnable.addWorkload(() -> {
                    NMSManager.getAdapter().setBlockInNativeDataPalette(
                            pasteable.location.getWorld(),
                            pasteable.location.getBlockX(),
                            pasteable.location.getBlockY(),
                            pasteable.location.getBlockZ(),
                            pasteable.blockData,
                            true);
                });
        }
        pasteMeRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        return freshlyInterpretedSigns;
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

    private void createModularWorld(World world) {
        new ModularWorld(world, interpretedSigns);
    }

    public record InterpretedSign(Location location, List<String> text) {
    }

    private record Pasteable(Location location, BlockData blockData) {
    }
}