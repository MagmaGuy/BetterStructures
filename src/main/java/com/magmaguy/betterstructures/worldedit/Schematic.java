package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.WorkloadRunnable;
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
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class Schematic {
    // Queue to hold pending paste operations
    private static final Queue<PasteBlockOperation> pasteQueue = new ConcurrentLinkedQueue<>();
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
     * Creates a list of paste blocks from a schematic
     *
     * @param schematicClipboard The clipboard containing the schematic
     * @param location The location to paste at
     * @param schematicOffset The offset of the schematic
     * @param pedestalMaterialProvider Function that provides pedestal material based on whether it's a surface block
     * @return List of paste blocks
     */
    private static List<PasteBlock> createPasteBlocks(
            Clipboard schematicClipboard,
            Location location,
            Vector schematicOffset,
            Function<Boolean, Material> pedestalMaterialProvider) {

        List<PasteBlock> pasteBlocks = new ArrayList<>();

        // Iterate through the schematic and create PasteBlock objects
        Location adjustedLocation = location.clone().add(schematicOffset);
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int y = 0; y < schematicClipboard.getDimensions().y(); y++)
                for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                    BlockVector3 adjustedClipboardLocation = BlockVector3.at(
                            x + schematicClipboard.getMinimumPoint().x(),
                            y + schematicClipboard.getMinimumPoint().y(),
                            z + schematicClipboard.getMinimumPoint().z());
                    BaseBlock baseBlock = schematicClipboard.getFullBlock(adjustedClipboardLocation);
                    BlockState blockState = baseBlock.toImmutableState();
                    BlockData blockData = Bukkit.createBlockData(baseBlock.toImmutableState().getAsString());
                    Material material = BukkitAdapter.adapt(baseBlock.getBlockType());
                    Block worldBlock = adjustedLocation.clone().add(new Vector(x, y, z)).getBlock();
                    String materialString = material.toString().toUpperCase(Locale.ROOT);
                    boolean isGround = !BukkitAdapter.adapt(schematicClipboard.getBlock(
                            BlockVector3.at(adjustedClipboardLocation.x(),
                                    adjustedClipboardLocation.y() + 1,
                                    adjustedClipboardLocation.z())).getBlockType()).isSolid();

                    if (material == Material.BARRIER) {
                        // special behavior: do not replace barriers, so do nothing
                    } else if (materialString.endsWith("SIGN") ||
                            materialString.endsWith("STAIRS") ||
                            materialString.endsWith("BOX") ||
                            materialString.endsWith("CHEST_BOAT") ||
                            materialString.equals("BEACON") ||
                            materialString.endsWith("FURNACE") ||
                            materialString.equals("CALIBRATED_SCULK_SENSOR") ||
                            materialString.equals("CAMPFIRE") ||
                            materialString.equals("CARTOGRAPHY_TABLE") ||
                            materialString.equals("CAULDRON") ||
                            materialString.contains("COMMAND_BLOCK") ||
                            materialString.endsWith("ANVIL") ||
                            materialString.equals("CRAFTER") ||
                            materialString.equals("ITEM_FRAME") ||
                            materialString.equals("DISPENSER") ||
                            materialString.equals("DROPPER") ||
                            materialString.equals("ENCHANTING_TABLE") ||
                            materialString.equals("BARREL") ||
                            materialString.equals("CHEST") ||
                            materialString.equals("ENDER_CHEST") ||
                            materialString.equals("TRAPPED_CHEST") ||
                            materialString.equals("FLETCHING_TABLE") ||
                            materialString.equals("FURNACE_MINECART") ||
                            materialString.equals("GRINDSTONE") ||
                            materialString.equals("HOPPER") ||
                            materialString.equals("HOPPER_MINECART") ||
                            materialString.equals("JUKEBOX") ||
                            materialString.equals("LEVER") ||
                            materialString.equals("LOOM") ||
                            materialString.equals("LODESTONE") ||
                            materialString.startsWith("POTTED") ||
                            materialString.startsWith("SCULK") ||
                            materialString.equals("POWERED_RAIL") ||
                            materialString.equals("SMOKER") ||
                            materialString.equals("STONECUTTER") ||
                            materialString.equals("SOUL_CAMPFIRE") ||
                            materialString.contains("SPAWNER")) {
                        // tricky metadata has to be done via worldedit
                        pasteBlocks.add(new PasteBlock(worldBlock, null,
                                WorldEditUtils.createSingleBlockClipboard(adjustedLocation, baseBlock, blockState)));
                    } else if (material == Material.BEDROCK) {
                        // special behavior: if it's not solid, replace with solid filler block
                        if (!worldBlock.getType().isSolid()) {
                            Material pedestalMaterial = pedestalMaterialProvider.apply(isGround);
                            worldBlock.setType(pedestalMaterial);
                            pasteBlocks.add(new PasteBlock(worldBlock, pedestalMaterial.createBlockData(), null));
                        }
                    } else {
                        pasteBlocks.add(new PasteBlock(worldBlock, blockData, null));
                    }
                }

        return pasteBlocks;
    }

    /**
     * Pastes a schematic using the provided pedestal material provider
     *
     * @param schematicClipboard The clipboard containing the schematic
     * @param location The location to paste at
     * @param schematicOffset The offset of the schematic
     * @param pedestalMaterialProvider Function that provides pedestal material based on whether it's a surface block
     * @param onComplete Callback to run when paste is complete
     */
    public static void pasteSchematic(
            Clipboard schematicClipboard,
            Location location,
            Vector schematicOffset,
            Function<Boolean, Material> pedestalMaterialProvider,
            Runnable onComplete) {

        List<PasteBlock> pasteBlocks = createPasteBlocks(
                schematicClipboard,
                location,
                schematicOffset,
                pedestalMaterialProvider);

        pasteDistributed(pasteBlocks, location, onComplete);
    }

    /**
     * Pastes a schematic using a distributed workload over multiple ticks.
     * If another paste operation is already in progress, this operation
     * will be queued and executed when the current operation completes.
     *
     * @param pasteBlocks List of blocks to paste
     * @param location    The location to paste at
     * @param onComplete  Optional callback to run when paste is complete
     */
    public static void pasteDistributed(List<PasteBlock> pasteBlocks, Location location, Runnable onComplete) {
        // Add this paste operation to the queue
        pasteQueue.add(new PasteBlockOperation(pasteBlocks, location, onComplete));

        // If we're not currently pasting, start processing the queue
        if (!isDistributedPasting) {
            processNextPaste();
        }
    }

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

        for (PasteBlock pasteBlock : operation.blocks) {
            workload.addWorkload(() -> {
                if (pasteBlock.blockData() != null) {
                    pasteBlock.block().setBlockData(pasteBlock.blockData());
                } else if (pasteBlock.clipboard() != null) {
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(pasteBlock.block().getLocation().getWorld()))) {
                        Operation worldeditPaste = new ClipboardHolder(pasteBlock.clipboard())
                                .createPaste(editSession)
                                .to(BlockVector3.at(pasteBlock.block().getX(), pasteBlock.block().getY(), pasteBlock.block().getZ()))
                                // configure here
                                .build();
                        Operations.complete(worldeditPaste);
                    } catch (WorldEditException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        // Start the workload
        workload.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    /**
     * Represents a single paste operation
     */
    private record PasteBlockOperation(List<PasteBlock> blocks, Location location, Runnable onComplete) {
    }

    public record PasteBlock(Block block, BlockData blockData, Clipboard clipboard) {
    }
}