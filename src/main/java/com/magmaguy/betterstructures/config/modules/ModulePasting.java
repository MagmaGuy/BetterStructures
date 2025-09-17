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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Sign;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ModulePasting {
    private final List<InterpretedSign> interpretedSigns = new ArrayList<>();
    private ModularGenerationStatus modularGenerationStatus = null;
    private ModularWorld modularWorld;
    private String spawnPoolSuffix;
    private Location startLocation;

    public ModulePasting(World world, File worldFolder, Deque<GridCell> gridCellDeque, ModularGenerationStatus modularGenerationStatus, String spawnPoolSuffix, Location startLocation) {
        this.modularGenerationStatus = modularGenerationStatus;
        this.spawnPoolSuffix = spawnPoolSuffix;
        this.startLocation = startLocation;
        Logger.debug("Created ModulePasting instance for world " + world.getName() + " with " + gridCellDeque.size() + " blocks to paste");
        batchPaste(gridCellDeque, interpretedSigns);
        createModularWorld(world, worldFolder);
    }

    private List<Pasteable> generatePasteMeList(Clipboard clipboard, Location worldPasteOriginLocation, Integer rotation, List<InterpretedSign> interpretedSigns) {
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

            BaseBlock baseBlock = finalTransformedClipboard.getFullBlock(blockPos);
            BlockData blockData = Bukkit.createBlockData(baseBlock.toImmutableState().getAsString());
            if (blockData.getMaterial().isAir()) return;

            int worldX = baseX + (blockPos.x() - finalOrigin.x());
            int worldY = baseY + (blockPos.y() - finalOrigin.y());
            int worldZ = baseZ + (blockPos.z() - finalOrigin.z());

            Location pasteLocation = new Location(world, worldX, worldY, worldZ);
            pasteableList.add(new Pasteable(pasteLocation, blockData));

            if (blockData.getMaterial().toString().toLowerCase().contains("sign")) {
                interpretedSigns.add(new InterpretedSign(pasteLocation, getLines(baseBlock)));
            }
        });
        return pasteableList;
    }

    private List<String> getLines(BaseBlock baseBlock){
        List<String> strings = new ArrayList<>();
        for (String line : WorldEditUtils.getLines(baseBlock)) {
            if (line != null && !line.isBlank() && line.contains("[pool:"))
                strings.add(line.replace("]",spawnPoolSuffix+"]"));
            else strings.add(line);
        }
        return strings;
    }

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        if (rotation == null) {
            Logger.debug("Rotation was null during paste operation");
            return;
        }

        // Transform the clipboard using the same approach as batch paste
        AffineTransform transform = new AffineTransform().rotateY(normalizeRotation(rotation));
        Clipboard transformedClipboard;
        try {
            transformedClipboard = clipboard.transform(transform);
        } catch (WorldEditException e) {
            Logger.warn("Failed to transform clipboard: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // Get dimensions and calculate proper center
        BlockVector3 minPoint = transformedClipboard.getMinimumPoint();

        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        // Create edit session for actual placement
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
            editSession.setTrackingHistory(false);
            editSession.setSideEffectApplier(SideEffectSet.none());

            // Process each block using calculated center point as reference
            transformedClipboard.getRegion().forEach(blockPos -> {
                try {
                    BaseBlock baseBlock = transformedClipboard.getFullBlock(blockPos);

                    // Skip air blocks
                    if (baseBlock.getBlockType().getMaterial().isAir()) return;

                    // Calculate world coordinates relative to center point
                    int worldX = baseX + (blockPos.x() - minPoint.x());
                    int worldY = baseY + (blockPos.y() - minPoint.y());
                    int worldZ = baseZ + (blockPos.z() - minPoint.z());

                    // Place the block
                    BlockVector3 worldPos = BlockVector3.at(worldX, worldY, worldZ);
                    editSession.setBlock(worldPos, baseBlock);

                } catch (WorldEditException e) {
                    Logger.warn("Failed to place block at " + blockPos + ": " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Logger.warn("Failed to paste structure: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static int normalizeRotation(int rotation) {
        return (360 - rotation) % 360;
    }

    public List<InterpretedSign> batchPaste(Deque<GridCell> gridCellDeque, List<InterpretedSign> interpretedSigns) {
        List<Pasteable> pasteableList = new ArrayList<>();

        AtomicInteger finishedFastBlocks = new AtomicInteger();
        AtomicInteger finishedSlowBlocks = new AtomicInteger();

        while (!gridCellDeque.isEmpty()) {
            GridCell gridCell = gridCellDeque.poll();
            if (gridCell == null || gridCell.getModulesContainer() == null) continue;
            Clipboard clipboard = gridCell.getModulesContainer().getClipboard();
            if (clipboard == null) continue;
            pasteableList.addAll(generatePasteMeList(clipboard, gridCell.getRealLocation(startLocation), gridCell.getModulesContainer().getRotation(), interpretedSigns));
        }

        List<Pasteable> slowBlocks = new ArrayList<>();
        WorkloadRunnable pasteMeRunnable = new WorkloadRunnable(.1, () -> {
            modularGenerationStatus.finishedPlacingFastBlocks();
            modularGenerationStatus.startPlacingSlowBlocks();
            WorkloadRunnable vanillaPlacementRunnable = new WorkloadRunnable(.1, () -> {
                modularGenerationStatus.finishedPlacingSlowBlocks();
                modularWorld.spawnOtherEntities();
            });
            int slowBlockCounter = slowBlocks.size();
            for (Pasteable slowBlock : slowBlocks)
                vanillaPlacementRunnable.addWorkload(() -> {
                    slowBlock.location.getBlock().setBlockData(slowBlock.blockData, false);
                    finishedSlowBlocks.getAndIncrement();
                    modularGenerationStatus.updateProgressPlacingSlowBlocks((double) finishedSlowBlocks.get() / (double) slowBlockCounter);
                });
            vanillaPlacementRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        });

        List<InterpretedSign> freshlyInterpretedSigns = new ArrayList<>();
        int fastBlockCounter = pasteableList.size();

        for (Pasteable pasteable : pasteableList) {
            //check for signs first
            if (pasteable.blockData.getLightEmission() > 0 || pasteable.blockData instanceof Directional || pasteable.blockData instanceof Rail || pasteable.blockData instanceof Sign)
                slowBlocks.add(pasteable);
            else
                pasteMeRunnable.addWorkload(() -> {
                    NMSManager.getAdapter().setBlockInNativeDataPalette(
                            pasteable.location.getWorld(),
                            pasteable.location.getBlockX(),
                            pasteable.location.getBlockY(),
                            pasteable.location.getBlockZ(),
                            pasteable.blockData,
                            true);
                    finishedFastBlocks.getAndIncrement();
                    modularGenerationStatus.updateProgressPlacingFastBlocks((double) finishedFastBlocks.get() / (double) fastBlockCounter);
                });
        }

        modularGenerationStatus.finishedPreparingPlacement();
        modularGenerationStatus.startPlacingFastBlocks();

        pasteMeRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        return freshlyInterpretedSigns;
    }

    private void createModularWorld(World world, File worldFolder) {
        modularWorld = new ModularWorld(world, worldFolder, interpretedSigns, modularGenerationStatus);
    }

    public record InterpretedSign(Location location, List<String> text) {
    }

    private record Pasteable(Location location, BlockData blockData) {
    }
}