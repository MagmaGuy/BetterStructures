package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.util.distributedload.WorkloadRunnable;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Schematic {
    // Queue to hold pending paste operations
    private static final Queue<PasteBlockOperation> pasteQueue = new ConcurrentLinkedQueue<>();
    private static final Object lock = new Object();
    private static boolean erroredOnce = false;
    private static boolean isDistributedPasting = false;
    private Schematic() {
    }

    /**
     * Loads a schematic from a file
     *
     * @param schematicFile The schematic file to load
     * @return The loaded clipboard or null if loading failed
     */
    public static Clipboard load(File schematicFile) {
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchElementException e) {
            Logger.warn("Failed to get element from schematic " + schematicFile.getName());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Logger.warn("Failed to load schematic " + schematicFile.getName() + " ! 99% of the time, this is because you are not using the correct WorldEdit version for your Minecraft server. You should be downloading WorldEdit from here https://dev.bukkit.org/projects/worldedit . You can check which versions the download links are compatible with by hovering over them.");
            erroredOnce = true;
            if (!erroredOnce) e.printStackTrace();
            else Logger.warn("Hiding stacktrace for this error, as it has already been printed once");
            return null;
        }
        return clipboard;
    }

    /**
     * Pastes a schematic synchronously
     *
     * @param clipboard The WorldEdit clipboard containing the schematic
     * @param location  The location to paste at
     */
    public static void paste(Clipboard clipboard, Location location) {
        World world = BukkitAdapter.adapt(location.getWorld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pastes a schematic using a distributed workload over multiple ticks.
     * If another paste operation is already in progress, this operation
     * will be queued and executed when the current operation completes.
     *
     * @param pasteBlocks  The WorldEdit clipboard containing the schematic
     * @param location   The location to paste at
     * @param onComplete Optional callback to run when paste is complete
     */
    public static void pasteDistributed(List<FitAnything.PasteBlock> pasteBlocks, Location location, Runnable onComplete) {
        // Add this paste operation to the queue
        pasteQueue.add(new PasteBlockOperation(pasteBlocks, location, onComplete));

        // If we're not currently pasting, start processing the queue
            if (!isDistributedPasting) {
                processNextPaste();
            }
    }

//    public static void pasteDistributed(Clipboard clipboard, Location location, Runnable onComplete) {
//        // Add this paste operation to the queue
//        pasteQueue.add(new PasteClipboardOperation(clipboard, location, onComplete));
//
//        // If we're not currently pasting, start processing the queue
//        if (!isDistributedPasting) {
//            processNextPaste();
//        }
//    }

//    /**
//     * Overloaded method without completion callback
//     */
//    public static void pasteDistributed(Clipboard clipboard, Location location) {
//        pasteDistributed(clipboard, location, null);
//    }

    /**
     * Processes the next paste operation in the queue
     */
    private static void processNextPaste() {
            if (pasteQueue.isEmpty()) {
                isDistributedPasting = false;
                return;
            }

            isDistributedPasting = true;
            PasteBlockOperation operation = pasteQueue.poll();

            // Create a workload for this paste operation
            WorkloadRunnable workload = new WorkloadRunnable(DefaultConfig.getPercentageOfTickUsedForPasting(), () -> {
                // Run the completion callback if provided
                if (operation.onComplete != null) {
                    operation.onComplete.run();
                }
                // Process the next paste in the queue
                processNextPaste();
            });

//            // Process clipboard blocks
//            Clipboard clipboard = operation.clipboard;
//            Location location = operation.location;
//            org.bukkit.World world = location.getWorld();
//
//            BlockVector3 origin = clipboard.getOrigin();
//            BlockVector3 minPoint = clipboard.getMinimumPoint();
//            // Adjust origin to start from minimum point for consistent placement
//            origin = BlockVector3.at(minPoint.x(), origin.y(), minPoint.z());
//
//            int baseX = location.getBlockX();
//            int baseY = location.getBlockY();
//            int baseZ = location.getBlockZ();
//
//            BlockVector3 finalOrigin = origin;

        for (FitAnything.PasteBlock pasteBlock : operation.blocks) {
            workload.addWorkload(() -> {
                // Use setBlockData for the slow paste method
                pasteBlock.block().setBlockData(pasteBlock.blockData());
            });
        }
//
//            // Add each block in the clipboard as a separate workload task
//            clipboard.getRegion().forEach(blockPos -> {
//                BaseBlock baseBlock = clipboard.getFullBlock(blockPos);
//                BlockData blockData = Bukkit.createBlockData(baseBlock.toImmutableState().getAsString());
//
//                // Remove this line to allow air blocks to be pasted
//                // if (blockData.getMaterial().isAir()) return;
//
//                int worldX = baseX + (blockPos.x() - finalOrigin.x());
//                int worldY = baseY + (blockPos.y() - finalOrigin.y());
//                int worldZ = baseZ + (blockPos.z() - finalOrigin.z());
//
//                Location pasteLocation = new Location(world, worldX, worldY, worldZ);
//
//
//            });

            // Start the workload
            workload.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    /**
     * Processes the next paste operation in the queue
     */

    /**
     * Represents a single paste operation
     */
    private record PasteClipboardOperation(Clipboard clipboard, Location location, Runnable onComplete) {
    }

    private record PasteBlockOperation(List<FitAnything.PasteBlock> blocks, Location location, Runnable onComplete) {
    }

}