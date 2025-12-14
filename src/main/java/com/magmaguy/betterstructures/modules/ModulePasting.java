package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.ChestFillEvent;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import com.magmaguy.magmacore.util.WorkloadRunnable;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ModulePasting {
    private final List<InterpretedSign> interpretedSigns = new ArrayList<>();
    private final List<ChestPlacement> chestsToPlace = new ArrayList<>();
    private final List<EntitySpawn> entitiesToSpawn = new ArrayList<>();
    private final String spawnPoolSuffix;
    private final Location startLocation;
    private final boolean createModularWorld;
    private final List<NbtPlacement> nbtToPlace = new ArrayList<>();
    private ModularWorld modularWorld;
    private final World world;
    private final File worldFolder;
    private final ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;

    public ModulePasting(World world, File worldFolder, Deque<WFCNode> WFCNodeDeque, String spawnPoolSuffix, Location startLocation, ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        this.spawnPoolSuffix = spawnPoolSuffix;
        this.startLocation = startLocation;
        this.world = world;
        this.worldFolder = worldFolder;
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;

        // Check debug mode and modular world creation settings from first node
        WFCNode firstNode = WFCNodeDeque.peek();
        this.createModularWorld = firstNode != null && firstNode.getWfcGenerator() != null &&
                firstNode.getWfcGenerator().getModuleGeneratorsConfigFields().isWorldGeneration();

        batchPaste(WFCNodeDeque, interpretedSigns);

        createModularWorld(world, worldFolder);

        // Send notification to players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("betterstructures.warn")) {
                player.spigot().sendMessage(
                        SpigotMessage.commandHoverMessage(
                                "[BetterStructures] New dungeon started generating! Do not stop your server now. Click to teleport. Do \"/betterstructures silent\" to stop getting warnings!",
                                "Click to teleport to " + startLocation.getWorld().getName() + ", " +
                                        startLocation.getBlockX() + ", " + startLocation.getBlockY() + ", " + startLocation.getBlockZ(),
                                "/betterstructures teleport " + startLocation.getWorld().getName() + " " +
                                        startLocation.getBlockX() + " " + startLocation.getBlockY() + " " + startLocation.getBlockZ())
                );
            }
        }
    }

    private static boolean isNbtRichMaterial(Material m) {
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST) return false;
        if (m.name().endsWith("_SIGN") || m.name().endsWith("_WALL_SIGN") || m.name().endsWith("_HANGING_SIGN"))
            return false;

        return switch (m) {
            case SPAWNER,
                 DISPENSER, DROPPER, HOPPER,
                 BEACON, LECTERN, JUKEBOX,
                 COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, CHAIN_COMMAND_BLOCK,
                 PLAYER_HEAD, PLAYER_WALL_HEAD,
                 SCULK_CATALYST, SCULK_SHRIEKER -> true;
            default -> false;
        };
    }

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        if (rotation == null) {
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

            pasteArmorStands(transformedClipboard, location, rotation);

        } catch (Exception e) {
            Logger.warn("Failed to paste structure: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static int normalizeRotation(int rotation) {
        return (360 - rotation) % 360;
    }

    public static void pasteArmorStands(Clipboard clipboard, Location location, Integer rotation) {
        if (rotation == null) rotation = 0;

        AffineTransform transform = new AffineTransform().rotateY(normalizeRotation(rotation));
        Clipboard transformedClipboard;
        try {
            transformedClipboard = clipboard.transform(transform);
        } catch (WorldEditException e) {
            Logger.warn("Failed to transform clipboard for entities: " + e.getMessage());
            return;
        }

        WorldEditUtils.pasteArmorStandsOnlyFromTransformed(transformedClipboard, location);
    }

    private List<Pasteable> generatePasteMeList(Clipboard clipboard,
                                                Location worldPasteOriginLocation,
                                                Integer rotation,
                                                List<InterpretedSign> interpretedSigns) {
        List<Pasteable> pasteableList = new ArrayList<>();

        // Apply rotation transformation
        AffineTransform transform = new AffineTransform().rotateY(normalizeRotation(rotation));
        Clipboard transformedClipboard;
        try {
            transformedClipboard = clipboard.transform(transform);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        // Get the minimum point of the transformed clipboard to use as reference
        BlockVector3 minPoint = transformedClipboard.getMinimumPoint();

        World world = worldPasteOriginLocation.getWorld();
        int baseX = worldPasteOriginLocation.getBlockX();
        int baseY = worldPasteOriginLocation.getBlockY();
        int baseZ = worldPasteOriginLocation.getBlockZ();

        // Process each block in the transformed clipboard
        transformedClipboard.getRegion().forEach(blockPos -> {
            BaseBlock baseBlock = transformedClipboard.getFullBlock(blockPos);
            BlockData blockData = Bukkit.createBlockData(baseBlock.toImmutableState().getAsString());

            // Skip barriers
            if (blockData.getMaterial().equals(Material.BARRIER)) return;

            // Calculate world coordinates relative to the minimum point
            int worldX = baseX + (blockPos.x() - minPoint.x());
            int worldY = baseY + (blockPos.y() - minPoint.y());
            int worldZ = baseZ + (blockPos.z() - minPoint.z());

            Location pasteLocation = new Location(world, worldX, worldY, worldZ);

            // Handle signs - collect instructions then turn into AIR
            if (blockData.getMaterial().toString().toLowerCase().contains("sign")) {
                List<String> lines = getLines(baseBlock);
                interpretedSigns.add(new InterpretedSign(pasteLocation, lines));

                // Parse sign content for special markers
                for (String line : lines) {
                    if (line.contains("[spawn]") && lines.size() > 1) {
                        try {
                            EntityType entityType = EntityType.valueOf(lines.get(1).toUpperCase());
                            entitiesToSpawn.add(new EntitySpawn(pasteLocation, entityType));
                        } catch (Exception e) {
                            Logger.warn("Invalid entity type in sign: " + lines.get(1));
                        }
                    } else if (line.contains("[chest]")) {
                        chestsToPlace.add(new ChestPlacement(pasteLocation, Material.CHEST, rotation));
                    } else if (line.contains("[trapped_chest]")) {
                        chestsToPlace.add(new ChestPlacement(pasteLocation, Material.TRAPPED_CHEST, rotation));
                    }
                }

                // Replace sign with air in the paste list so it won't be deferred as NBT-rich
                blockData = Material.AIR.createBlockData();
            }

            // Convert bedrock to stone (unless replacing a solid block)
            if (blockData.getMaterial().equals(Material.BEDROCK)) {
                if (pasteLocation.getBlock().getType().isSolid()) return;
                blockData = Material.STONE.createBlockData();
            }

            // Defer complex NBT blocks (dispensers, spawners, etc.) for post-processing via BaseBlock
            if (isNbtRichMaterial(blockData.getMaterial())) {
                nbtToPlace.add(new NbtPlacement(pasteLocation, baseBlock)); // keep full NBT
                return; // do NOT add to normal paste list
            }

            // Normal placement path
            pasteableList.add(new Pasteable(pasteLocation, blockData));
        });

        return pasteableList;
    }

    private List<String> getLines(BaseBlock baseBlock) {
        List<String> strings = new ArrayList<>();
        for (String line : WorldEditUtils.getLines(baseBlock)) {
            if (line != null && !line.isBlank() && line.contains("[pool:"))
                strings.add(line.replace("]", spawnPoolSuffix + "]"));
            else strings.add(line);
        }
        return strings;
    }

    public List<InterpretedSign> batchPaste(Deque<WFCNode> WFCNodeDeque, List<InterpretedSign> interpretedSigns) {
        List<Pasteable> pasteableList = new ArrayList<>();

        // Collect entity paste info while processing blocks
        List<EntityPasteInfo> entityPasteInfos = new ArrayList<>();

        while (!WFCNodeDeque.isEmpty()) {
            WFCNode WFCNode = WFCNodeDeque.poll();
            if (WFCNode == null || WFCNode.getModulesContainer() == null) continue;
            Clipboard clipboard = WFCNode.getModulesContainer().getClipboard();
            if (clipboard == null) continue;

            // Process blocks
            pasteableList.addAll(generatePasteMeList(clipboard, WFCNode.getRealLocation(startLocation),
                    WFCNode.getModulesContainer().getRotation(), interpretedSigns));

            // Store entity paste info for later - WITH TRANSFORMED CLIPBOARD
            AffineTransform transform = new AffineTransform().rotateY(normalizeRotation(WFCNode.getModulesContainer().getRotation()));
            try {
                Clipboard transformedClipboard = clipboard.transform(transform);
                entityPasteInfos.add(new EntityPasteInfo(transformedClipboard, WFCNode.getRealLocation(startLocation),
                        WFCNode.getModulesContainer().getRotation()));
            } catch (WorldEditException e) {
                Logger.warn("Failed to transform clipboard for entities: " + e.getMessage());
            }
        }

        List<Pasteable> slowBlocks = new ArrayList<>();
        WorkloadRunnable pasteMeRunnable = new WorkloadRunnable(.1, () -> {
            WorkloadRunnable vanillaPlacementRunnable = new WorkloadRunnable(.1, () -> {
                postPasteProcessing(entityPasteInfos);
            });

            for (Pasteable slowBlock : slowBlocks)
                vanillaPlacementRunnable.addWorkload(() -> {
                    slowBlock.location.getBlock().setBlockData(slowBlock.blockData, false);
                });
            vanillaPlacementRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
        });

        List<InterpretedSign> freshlyInterpretedSigns = new ArrayList<>();

        // Enable fast path only for world-based generation
        final boolean fastPathEnabled = this.createModularWorld;

        for (Pasteable pasteable : pasteableList) {
            if (!fastPathEnabled) {
                // Not world-based generation: force slow placement for EVERYTHING
                slowBlocks.add(pasteable);
                continue;
            }

            // World-based generation: keep original split between fast/slow
            if (pasteable.blockData.getLightEmission() > 0
                    || pasteable.blockData instanceof Directional
                    || pasteable.blockData instanceof Rail
                    || pasteable.blockData instanceof Sign) {
                slowBlocks.add(pasteable);
            } else {
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
        }

        pasteMeRunnable.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

        return freshlyInterpretedSigns;
    }

    private void postPasteProcessing(List<EntityPasteInfo> entityPasteInfos) {
        if (createModularWorld) {
            createModularWorld(world, worldFolder);
            modularWorld.spawnOtherEntities();
        }

        // 1) Paste deferred NBT-rich blocks (dispenser, spawner, etc.) with WE so NBT is preserved
        if (!nbtToPlace.isEmpty()) {
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
                editSession.setTrackingHistory(false);
                editSession.setSideEffectApplier(SideEffectSet.none());

                for (NbtPlacement np : nbtToPlace) {
                    BlockVector3 wp = BlockVector3.at(
                            np.location().getBlockX(),
                            np.location().getBlockY(),
                            np.location().getBlockZ()
                    );
                    try {
                        editSession.setBlock(wp, np.baseBlock()); // BaseBlock carries NBT
                    } catch (WorldEditException e) {
                        Logger.warn("Failed to set NBT block at " + np.location() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.warn("Failed NBT post-paste session: " + e.getMessage());
            }
        }

        // 2) Paste entities from schematics (armor stands, etc.)
        pasteArmorStandsForBatch(entityPasteInfos);

        for (ChestPlacement chestPlacement : chestsToPlace) {
            Block block = chestPlacement.location.getBlock();
            block.setType(chestPlacement.material);

            if (block.getBlockData() instanceof Chest chest) {
                block.setBlockData(chest, false);

                String treasureFilename = moduleGeneratorsConfigFields.getTreasureFile();
                TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(treasureFilename);
                if (treasureConfigFields != null) {
                    ChestContents chestContents = new ChestContents(treasureConfigFields);
                    Container container = (Container) block.getState();
                    chestContents.rollChestContents(container);
                    ChestFillEvent chestFillEvent = new ChestFillEvent(container, treasureFilename);
                    Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
                    if (!chestFillEvent.isCancelled())
                        container.update(true);
                }
            }
        }

        // 4) Spawn entities last
        for (EntitySpawn entitySpawn : entitiesToSpawn) {
            try {
                LivingEntity entity = (LivingEntity) world.spawnEntity(entitySpawn.location, entitySpawn.entityType);
                entity.setRemoveWhenFarAway(false);
                entity.setPersistent(true);
            } catch (Exception e) {
                Logger.warn("Failed to spawn entity of type " + entitySpawn.entityType + " at " + entitySpawn.location);
            }
        }
    }

    // Helper method to paste entities for all collected clipboards
    private void pasteArmorStandsForBatch(List<EntityPasteInfo> entityPasteInfos) {
        for (EntityPasteInfo info : entityPasteInfos) {
            try {
                WorldEditUtils.pasteArmorStandsOnlyFromTransformed(info.clipboard, info.location);
            } catch (Exception e) {
                Logger.warn("Failed to paste entities for batch operation at " + info.location + ": " + e.getMessage());
            }
        }
    }

    private void createModularWorld(World world, File worldFolder) {
        modularWorld = new ModularWorld(world, worldFolder, interpretedSigns);
    }

    private record NbtPlacement(Location location, BaseBlock baseBlock) {
    }

    // Record to hold entity paste information - now with transformed clipboard
    private record EntityPasteInfo(Clipboard clipboard, Location location, Integer rotation) {
    }

    private record ChestPlacement(Location location, Material material, Integer rotation) {
    }

    private record EntitySpawn(Location location, EntityType entityType) {
    }

    public record InterpretedSign(Location location, List<String> text) {
    }

    private record Pasteable(Location location, BlockData blockData) {
    }
}