package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.ChestFillEvent;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfigFields;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ModulePasting {
    private static final EnumSet<Material> SIGN_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        //Locale.ROOT: the default-locale lowercase turns "SIGN" into "sıgn" on Turkish locales
        for (Material m : Material.values())
            if (m.toString().toUpperCase(Locale.ROOT).contains("SIGN")) SIGN_MATERIALS.add(m);
    }

    private final List<InterpretedSign> interpretedSigns = new ArrayList<>();
    private final List<ChestPlacement> chestsToPlace = new ArrayList<>();
    private final List<BarrelPlacement> barrelsToFill = new ArrayList<>();
    private final List<EntitySpawn> entitiesToSpawn = new ArrayList<>();
    private final String spawnPoolSuffix;
    private final Location startLocation;
    private final boolean createModularWorld;
    private final List<NbtPlacement> nbtToPlace = new ArrayList<>();
    private record PreparedState(BlockData blockData) { }
    private final Map<BlockState, PreparedState> preparedStates = new java.util.LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<BlockState, PreparedState> eldest) {
            return size() > 4096;
        }
    };
    private ModularWorld modularWorld;
    private boolean missingEliteMobsWarned;
    private final World world;
    private final File worldFolder;
    private final ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;

    public ModulePasting(World world, File worldFolder, Deque<WFCNode> WFCNodeDeque, String spawnPoolSuffix, Location startLocation, ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        //Generators without spawnPoolSuffix must look up "[pool:name]" as name.yml, not namenull.yml
        this.spawnPoolSuffix = spawnPoolSuffix == null ? "" : spawnPoolSuffix;
        this.startLocation = startLocation;
        this.world = world;
        this.worldFolder = worldFolder;
        this.moduleGeneratorsConfigFields = moduleGeneratorsConfigFields;

        // Check debug mode and modular world creation settings from first node
        WFCNode firstNode = WFCNodeDeque.peek();
        this.createModularWorld = firstNode != null && firstNode.getWfcGenerator() != null &&
                firstNode.getWfcGenerator().getModuleGeneratorsConfigFields().isWorldGeneration();

        batchPaste(WFCNodeDeque);

        // Send notification to players
        if (DefaultConfig.isNewBuildingWarn()) {
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
    }

    private static boolean isNbtRichMaterial(Material m) {
        if (m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) return false;
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

            //The clipboard is already rotated; rotating it again here would rotate it twice
            WorldEditUtils.pasteArmorStandsOnlyFromTransformed(transformedClipboard, location);

        } catch (Exception e) {
            Logger.warn("Failed to paste structure: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static int normalizeRotation(int rotation) {
        return (360 - rotation) % 360;
    }

    private Pasteable prepareBlock(BaseBlock baseBlock, Location pasteLocation,
                                   ModulesConfigFields modulesConfigFields, boolean collect) {
        BlockState blockState = baseBlock.toImmutableState();
        PreparedState prepared = preparedStates.computeIfAbsent(blockState, state -> {
            Material material = WorldEditUtils.adaptMaterial(state);
            return new PreparedState(material == null
                    ? null : WorldEditUtils.createBlockDataOrNull(baseBlock));
        });

        BlockData blockData = prepared.blockData();
        if (blockData == null) {
            if (collect) nbtToPlace.add(new NbtPlacement(pasteLocation, baseBlock));
            return null;
        }

        // Handle signs - collect instructions then turn into AIR
        if (SIGN_MATERIALS.contains(blockData.getMaterial())) {
            List<String> lines = collect ? getLines(baseBlock) : List.of();
            if (collect) interpretedSigns.add(new InterpretedSign(pasteLocation, lines));

            // Parse sign content for special markers
            if (collect) for (String line : lines) {
                if (line.contains("[spawn]") && lines.size() > 1) {
                    try {
                        EntityType entityType = EntityType.valueOf(lines.get(1).toUpperCase());
                        entitiesToSpawn.add(new EntitySpawn(entitySpawnLocation(pasteLocation), entityType));
                    } catch (Exception e) {
                        Logger.warn("Invalid entity type in sign: " + lines.get(1));
                    }
                } else if (line.contains("[chest]")) {
                    chestsToPlace.add(new ChestPlacement(pasteLocation, Material.CHEST));
                } else if (line.contains("[trapped_chest]")) {
                    chestsToPlace.add(new ChestPlacement(pasteLocation, Material.TRAPPED_CHEST));
                }
            }

            // Replace sign with air in the paste list so it won't be deferred as NBT-rich
            blockData = Material.AIR.createBlockData();
        }

        // Convert bedrock to stone (unless replacing a solid block)
        if (blockData.getMaterial().equals(Material.BEDROCK)) {
            if (pasteLocation.getBlock().getType().isSolid()) return null;
            blockData = Material.STONE.createBlockData();
        }

        // Defer complex NBT blocks (dispensers, spawners, etc.) for post-processing via BaseBlock
        if (isNbtRichMaterial(blockData.getMaterial())) {
            if (collect) nbtToPlace.add(new NbtPlacement(pasteLocation, baseBlock)); // keep full NBT
            return null; // do NOT add to normal paste list
        }

        if (collect && blockData.getMaterial() == Material.BARREL) {
            barrelsToFill.add(new BarrelPlacement(pasteLocation, modulesConfigFields));
        }

        // Normal placement path
        return new Pasteable(pasteLocation, blockData);

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

    /**
     * Modules pasted into an existing world have no ModularWorld consumer, so their boss signs spawn
     * one-time regional bosses, the same way [elitemobs] signs do in regular structures.
     */
    private void spawnSignBoss(InterpretedSign sign) {
        String bossFilename = bossFilename(sign.text());
        if (bossFilename == null) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("EliteMobs")) {
            if (!missingEliteMobsWarned)
                Logger.warn("Modular structure at " + startLocation.getBlockX() + ", " + startLocation.getBlockY() + ", "
                        + startLocation.getBlockZ() + " has EliteMobs boss signs, but EliteMobs is not enabled. Its bosses were not spawned.");
            missingEliteMobsWarned = true;
            return;
        }
        EliteMobs.Spawn(entitySpawnLocation(sign.location()), bossFilename);
    }

    /**
     * The boss a module sign requests: "[elitemobs]" followed by the boss filename, which may be split
     * across the remaining lines, or "[pool:name]" resolved through spawn_pools/name.yml.
     * Null for signs that do not request a boss, or whose pool cannot supply one.
     */
    static String bossFilename(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.toLowerCase(Locale.ROOT).contains("[elitemobs]")) {
                StringBuilder filename = new StringBuilder();
                for (String part : lines.subList(i + 1, lines.size())) filename.append(part.trim());
                if (filename.isEmpty()) {
                    Logger.warn("Module sign " + lines + " has no EliteMobs boss filename");
                    return null;
                }
                return filename.toString();
            }
            String poolName = SpawnPoolsConfig.extractPoolName(line);
            if (poolName == null) continue;
            String poolFilename = poolName + ".yml";
            SpawnPoolsConfigFields pool = SpawnPoolsConfig.getConfigFields(poolFilename);
            if (pool == null) {
                Logger.warn("Could not find spawn pool " + poolFilename);
                return null;
            }
            String bossFilename = SpawnPoolsConfig.pickBossFilename(pool);
            if (bossFilename == null) Logger.warn("Spawn pool " + poolFilename + " has no entries");
            return bossFilename;
        }
        return null;
    }

    private void batchPaste(Deque<WFCNode> nodes) {
        List<ModuleInput> inputs = new ArrayList<>();
        while (!nodes.isEmpty()) {
            WFCNode node = nodes.poll();
            ModulesContainer module = node.getModulesContainer();
            if (module != null && module.getClipboard() != null)
                inputs.add(new ModuleInput(module.getClipboard(), node.getRealLocation(startLocation).clone(),
                        module.getRotation(), module.getModulesConfigField()));
        }
        com.magmaguy.betterstructures.worldedit.Schematic.enqueue(new ModularPaste(inputs));
    }

    private record ModuleInput(Clipboard clipboard, Location location, int rotation, ModulesConfigFields config) { }

    static boolean skipCell(BlockState state, boolean voidWorld) {
        // Existing worlds need air placements to carve interiors; void worlds already contain air.
        return state.getBlockType().id().equals("minecraft:barrier")
                || voidWorld && state.getBlockType().getMaterial().isAir();
    }

    /** One block or post-processing entry per queue step. No complete transformed clipboard is materialized. */
    private final class ModularPaste implements com.magmaguy.betterstructures.worldedit.Schematic.PasteOperation {
        private final List<ModuleInput> inputs;
        private final com.magmaguy.betterstructures.worldedit.PasteChunkReadiness chunks =
                new com.magmaguy.betterstructures.worldedit.PasteChunkReadiness(world);
        private int phase, moduleIndex, postIndex;
        private ModuleInput module;
        private AffineTransform transform;
        private BlockVector3 minimum;
        private com.sk89q.worldedit.extent.transform.BlockTransformExtent blocks;
        private ModularPasteCursor cursor;
        private java.util.Iterator<? extends com.sk89q.worldedit.entity.Entity> entityCursor;
        private BlockVector3 nextBlock;
        private BaseBlock nextBlockData;
        private boolean skipNextBlock;
        private com.sk89q.worldedit.entity.Entity nextEntity;
        private Location nextLocation;
        private final Map<String, ChestContents> barrelContents = new HashMap<>();
        private boolean closed;
        private boolean cancelled;

        private ModularPaste(List<ModuleInput> inputs) { this.inputs = inputs; }
        public boolean hasNext() { return !closed && phase < 8; }

        private void selectModule() {
            module = inputs.get(moduleIndex);
            transform = new AffineTransform().rotateY(normalizeRotation(module.rotation()));
            cursor = new ModularPasteCursor(module.clipboard().getMinimumPoint(), module.clipboard().getMaximumPoint(),
                    transform, module.location().getBlockX(), module.location().getBlockZ());
            minimum = cursor.transformedMinimum();
            blocks = new com.sk89q.worldedit.extent.transform.BlockTransformExtent(module.clipboard(), transform);
            entityCursor = module.clipboard().getEntities().iterator();
        }

        private Location target(com.sk89q.worldedit.math.Vector3 position) {
            var rotated = transform.apply(position).subtract(minimum.toVector3());
            return module.location().clone().add(rotated.x(), rotated.y(), rotated.z());
        }

        public boolean ready() {
            if (Bukkit.getWorld(world.getUID()) != world) {
                // The owning instance may be cancelled while its blocks are still queued.
                cancelled = true;
                close();
                return true;
            }
            if (phase < 2 || phase == 5) {
                if (moduleIndex >= inputs.size()) return true;
                if (module == null) return true;
                if (phase < 2) {
                    if (nextBlock == null && cursor.hasNext()) {
                        nextBlock = cursor.next();
                        nextBlockData = blocks.getFullBlock(nextBlock);
                        skipNextBlock = skipCell(nextBlockData.toImmutableState(), createModularWorld);
                    }
                    if (nextBlock == null) return true;
                    // Consume skipped cells through pasteNext too: ready must never scan unbounded air.
                    if (skipNextBlock) return true;
                    nextLocation = target(nextBlock.toVector3());
                } else {
                    if (nextEntity == null && entityCursor.hasNext()) nextEntity = entityCursor.next();
                    if (nextEntity == null) return true;
                    nextLocation = target(nextEntity.getLocation().toVector());
                }
            } else {
                nextLocation = switch (phase) {
                    case 2 -> postIndex < nbtToPlace.size() ? nbtToPlace.get(postIndex).location() : null;
                    case 3 -> postIndex < chestsToPlace.size() ? chestsToPlace.get(postIndex).location() : null;
                    case 4 -> postIndex < barrelsToFill.size() ? barrelsToFill.get(postIndex).location() : null;
                    case 6 -> postIndex < entitiesToSpawn.size() ? entitiesToSpawn.get(postIndex).location() : null;
                    case 7 -> postIndex < interpretedSigns.size() ? interpretedSigns.get(postIndex).location() : null;
                    default -> null;
                };
            }
            return nextLocation == null || chunks.ready(nextLocation);
        }

        public void pasteNext() {
            if (closed) return;
            if (phase < 2 || phase == 5) {
                if (moduleIndex >= inputs.size()) { advance(); return; }
                if (module == null) { selectModule(); return; }
                if (phase < 2) {
                    if (nextBlock == null) { moduleIndex++; module = null; return; }
                    nextBlock = null;
                    BaseBlock data = nextBlockData;
                    nextBlockData = null;
                    if (skipNextBlock) return;
                    Pasteable block = prepareBlock(data, nextLocation, module.config(), phase == 0);
                    if (block == null) return;
                    boolean fast = createModularWorld && block.blockData().getLightEmission() == 0
                            && !(block.blockData() instanceof Directional) && !(block.blockData() instanceof Rail)
                            && !(block.blockData() instanceof Sign);
                    if (phase == 0 && fast)
                        NMSManager.getAdapter().setBlockInNativeDataPalette(world, block.location().getBlockX(),
                                block.location().getBlockY(), block.location().getBlockZ(), block.blockData(), false);
                    else if (phase == 1 && !fast) block.location().getBlock().setBlockData(block.blockData(), false);
                } else {
                    if (nextEntity == null) { moduleIndex++; module = null; return; }
                    var origin = module.clipboard().getOrigin().toVector3();
                    var destination = transform.apply(origin).subtract(minimum.toVector3())
                            .add(module.location().getX(), module.location().getY(), module.location().getZ());
                    try {
                        new com.sk89q.worldedit.function.entity.ExtentEntityCopy(origin, BlockAttachedEntityPaste.destination(world),
                                destination, transform).apply(BlockAttachedEntityPaste.rotated(nextEntity, transform));
                    } catch (WorldEditException failure) { throw new IllegalStateException(failure); }
                    nextEntity = null;
                }
                return;
            }
            switch (phase) {
                case 2 -> {
                    if (postIndex >= nbtToPlace.size()) { advance(); return; }
                    NbtPlacement entry = nbtToPlace.get(postIndex++);
                    try (EditSession edit = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                        edit.setTrackingHistory(false);
                        edit.setSideEffectApplier(SideEffectSet.none());
                        edit.setBlock(BlockVector3.at(entry.location().getBlockX(), entry.location().getBlockY(),
                                entry.location().getBlockZ()), entry.baseBlock());
                    } catch (WorldEditException failure) { throw new IllegalStateException(failure); }
                }
                case 3 -> {
                    if (postIndex >= chestsToPlace.size()) { advance(); return; }
                    ChestPlacement entry = chestsToPlace.get(postIndex++);
                    Block block = entry.location().getBlock();
                    block.setType(entry.material(), false);
                    if (block.getState() instanceof Container container)
                        fill(container, moduleGeneratorsConfigFields.getTreasureFile());
                }
                case 4 -> {
                    if (postIndex >= barrelsToFill.size()) { advance(); return; }
                    BarrelPlacement entry = barrelsToFill.get(postIndex++);
                    ModulesConfigFields config = entry.modulesConfigFields();
                    if (!moduleGeneratorsConfigFields.isGenerateLootInBarrels() || config != null && !config.isGenerateLootInBarrels()) return;
                    String treasure = config != null && config.getBarrelTreasureFilename() != null && !config.getBarrelTreasureFilename().isEmpty()
                            ? config.getBarrelTreasureFilename() : moduleGeneratorsConfigFields.getBarrelTreasureFilename();
                    if (entry.location().getBlock().getType() == Material.BARREL
                            && entry.location().getBlock().getState() instanceof Container container) fill(container, treasure);
                }
                case 6 -> {
                    if (postIndex >= entitiesToSpawn.size()) { advance(); return; }
                    EntitySpawn entry = entitiesToSpawn.get(postIndex++);
                    org.bukkit.entity.Entity entity = world.spawnEntity(entry.location(), entry.entityType());
                    if (entity instanceof LivingEntity living) living.setRemoveWhenFarAway(false);
                    entity.setPersistent(true);
                }
                case 7 -> {
                    if (postIndex >= interpretedSigns.size()) { advance(); return; }
                    InterpretedSign sign = interpretedSigns.get(postIndex++);
                    if (!createModularWorld) {
                        spawnSignBoss(sign);
                        return;
                    }
                    if (modularWorld == null) modularWorld = createWorld();
                    modularWorld.addSign(sign);
                    modularWorld.spawnOtherEntitiesAt(sign);
                }
                default -> throw new IllegalStateException("Invalid paste phase");
            }
        }

        private void fill(Container container, String filename) {
            if (filename == null || filename.isEmpty()) return;
            if (!barrelContents.containsKey(filename)) {
                TreasureConfigFields config = TreasureConfig.getConfigFields(filename);
                barrelContents.put(filename, config == null ? null : config.getChestContents());
                if (config == null) Logger.warn("Missing modular treasure configuration " + filename);
            }
            ChestContents contents = barrelContents.get(filename);
            if (contents == null) return;
            contents.rollChestContents(container);
            ChestFillEvent event = new ChestFillEvent(container, filename);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) container.update(true);
        }

        private void advance() {
            phase++; moduleIndex = 0; postIndex = 0; module = null; nextBlock = null; nextBlockData = null; nextEntity = null; nextLocation = null;
        }
        public void onComplete() {
            if (!cancelled && Bukkit.getWorld(world.getUID()) == world && createModularWorld) {
                if (modularWorld == null) modularWorld = createWorld();
                modularWorld.generationFinished();
            }
        }
        private ModularWorld createWorld() {
            int moduleSize = moduleGeneratorsConfigFields.getModuleSizeXZ();
            // The lattice's half-module integer offset leaves odd-width modules centered on half blocks.
            Location center = new Location(world, startLocation.getBlockX() + (moduleSize % 2) * .5,
                    startLocation.getBlockY(), startLocation.getBlockZ() + (moduleSize % 2) * .5);
            double size = (2D * moduleGeneratorsConfigFields.getRadius() - 1) * moduleSize;
            return new ModularWorld(world, worldFolder, List.of(), center, size);
        }
        public void close() {
            closed = true; chunks.close(); inputs.clear();
            preparedStates.clear();
            nbtToPlace.clear(); chestsToPlace.clear(); barrelsToFill.clear(); entitiesToSpawn.clear(); interpretedSigns.clear();
            module = null; cursor = null; entityCursor = null; nextBlock = null; nextBlockData = null; nextEntity = null;
        }
    }

    private record NbtPlacement(Location location, BaseBlock baseBlock) {
    }

    private record ChestPlacement(Location location, Material material) {
    }

    private record BarrelPlacement(Location location, ModulesConfigFields modulesConfigFields) {
    }

    private record EntitySpawn(Location location, EntityType entityType) {
    }

    public record InterpretedSign(Location location, List<String> text) {
    }

    static Location entitySpawnLocation(Location marker) {
        // Marker coordinates address a block; entity centers must clear the neighboring blocks.
        return marker.clone().add(.5, 0, .5);
    }

    private record Pasteable(Location location, BlockData blockData) {
    }
}
