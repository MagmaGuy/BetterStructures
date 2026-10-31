package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.ChunkMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A paste reads every position of its clipboard, but a clipboard holds only a few hundred block states.
 * Parsing each position's state string again made string parsing a large share of the paste's server time:
 * one 334,080-position build parsed about 61,000 state strings and 670,000 block ids from 213 states.
 */
final class SchematicPasteStatesTest {
    private static final int WIDTH = 4, HEIGHT = 3, DEPTH = 4;
    private ServerMock server;
    private JavaPlugin plugin;
    private WorldMock world;
    private final Map<String, Integer> parsed = new HashMap<>();
    private final BaseBlock bedrock = block("bedrock", false, true), stone = block("stone", false, true),
            planks = block("oak_planks", false, true), barrier = block("barrier", false, true),
            air = block("air", true, false);
    private int completions;

    @BeforeEach
    void open() {
        server = MockBukkit.mock(new ServerMock() {
            @Override public BlockData createBlockData(String data) {
                parsed.merge(data, 1, Integer::sum);
                return super.createBlockData(data);
            }
        });
        plugin = MockBukkit.createMockPlugin("SchematicPasteStatesFixture");
        MetadataHandler.PLUGIN = plugin;
        world = new LoadedWorld();
        world.setName("schematic-paste-states");
        server.addWorld(world);
    }

    @AfterEach
    void close() {
        Schematic.shutdown();
        PluginChunkHolds.shutdown();
        MetadataHandler.PLUGIN = null;
        MockBukkit.unmock();
    }

    @Test
    void parsesEachClipboardStateOncePerPaste() {
        Location origin = new Location(world, 100, 70, -40);
        world.getBlockAt(origin.clone().add(3, 2, 3)).setType(Material.DIRT);
        world.getBlockAt(origin.clone().add(0, 2, 1)).setType(Material.DIRT);

        paste(origin);

        assertEquals(1, completions);
        assertEquals(Map.of("minecraft:bedrock", 1, "minecraft:stone", 1, "minecraft:oak_planks", 1,
                "minecraft:air", 1), parsed, "Each clipboard state must be parsed once, not once per position");
        for (int x = 0; x < WIDTH; x++)
            for (int z = 0; z < DEPTH; z++) {
                // Bedrock becomes the pedestal: ground under open air, underground beneath a solid block.
                assertEquals(x < 2 ? Material.COBBLESTONE : Material.GRASS_BLOCK, at(origin, x, 0, z));
                assertEquals(x < 2 ? Material.STONE : Material.AIR, at(origin, x, 1, z));
                Material top = z == 0 ? Material.OAK_PLANKS : x == 0 && z == 1 ? Material.DIRT : Material.AIR;
                assertEquals(top, at(origin, x, 2, z), "Barriers keep terrain and clipboard air carves it");
            }
    }

    @Test
    void eachPasteParsesItsStatesAgain() {
        paste(new Location(world, 0, 70, 0));
        paste(new Location(world, 64, 70, 0));

        assertEquals(2, completions);
        assertEquals(2, parsed.get("minecraft:stone"));
        assertEquals(2, parsed.get("minecraft:oak_planks"));
        assertEquals(2, parsed.get("minecraft:bedrock"));
    }

    @Test
    void stairsPasteFromTheirBlockData() {
        Location origin = new Location(world, 8, 70, 8);
        BaseBlock stairs = block("oak_stairs", "[facing=east,half=top,shape=straight,waterlogged=false]", false, true);

        paste(origin, Map.of(BlockVector3.ZERO, stairs), BlockVector3.ONE);

        assertEquals(1, completions);
        assertEquals(1, parsed.get("minecraft:oak_stairs[facing=east,half=top,shape=straight,waterlogged=false]"));
        Stairs placed = (Stairs) world.getBlockAt(origin).getBlockData();
        assertEquals(BlockFace.EAST, placed.getFacing());
        assertEquals(Bisected.Half.TOP, placed.getHalf());
    }

    @Test
    void onlyBlockEntityDataPastesThroughWorldEdit() {
        assertFalse(Schematic.pastesThroughWorldEdit(block("oak_stairs", false, true), Material.OAK_STAIRS),
                "Stairs carry no block-entity data");
        assertFalse(Schematic.pastesThroughWorldEdit(block("chest", false, true), Material.CHEST),
                "A chest saved without block-entity data has nothing for WorldEdit to add");
        assertTrue(Schematic.pastesThroughWorldEdit(withData(block("chest", false, true)), Material.CHEST));
        assertTrue(Schematic.pastesThroughWorldEdit(withData(block("white_wall_banner", false, true)),
                Material.WHITE_WALL_BANNER), "Banner patterns are block-entity data");
        assertFalse(Schematic.pastesThroughWorldEdit(withData(block("end_gateway", false, false)), Material.END_GATEWAY),
                "An end gateway's saved exit points into the world the build was made in");
    }

    private void paste(Location origin) {
        Map<BlockVector3, BaseBlock> blocks = new HashMap<>();
        for (int x = 0; x < WIDTH; x++)
            for (int z = 0; z < DEPTH; z++) {
                blocks.put(BlockVector3.at(x, 0, z), bedrock);
                blocks.put(BlockVector3.at(x, 1, z), x < 2 ? stone : air);
                blocks.put(BlockVector3.at(x, 2, z), z == 0 ? planks : z == 1 ? barrier : air);
            }
        paste(origin, blocks, BlockVector3.at(WIDTH, HEIGHT, DEPTH));
    }

    private void paste(Location origin, Map<BlockVector3, BaseBlock> blocks, BlockVector3 dimensions) {
        Clipboard clipboard = mock(Clipboard.class);
        when(clipboard.getDimensions()).thenReturn(dimensions);
        when(clipboard.getMinimumPoint()).thenReturn(BlockVector3.ZERO);
        when(clipboard.getFullBlock(any())).thenAnswer(call -> {
            BaseBlock value = blocks.get(call.<BlockVector3>getArgument(0));
            if (value == null) throw new AssertionError("Read outside the clipboard: " + call.getArgument(0));
            return value;
        });
        // A clipboard reads air outside its region, as WorldEdit's BlockArrayClipboard does.
        when(clipboard.getBlock(any())).thenAnswer(call ->
                blocks.getOrDefault(call.<BlockVector3>getArgument(0), air).toImmutableState());
        Schematic.pasteSchematic(clipboard, origin, new Vector(), ground -> ground ? Material.GRASS_BLOCK : Material.COBBLESTONE,
                () -> completions++);
        for (int tick = 0; tick < 100 && server.getScheduler().getPendingTasks().stream()
                .anyMatch(task -> task.getOwner() == plugin); tick++) server.getScheduler().performOneTick();
        assertTrue(server.getScheduler().getPendingTasks().stream().noneMatch(task -> task.getOwner() == plugin),
                "The paste queue did not drain");
    }

    private Material at(Location origin, int x, int y, int z) {
        return world.getBlockAt(origin.clone().add(x, y, z)).getType();
    }

    private static BaseBlock block(String id, boolean isAir, boolean solid) {
        return block(id, "", isAir, solid);
    }

    private static BaseBlock block(String id, String properties, boolean isAir, boolean solid) {
        BlockMaterial material = mock(BlockMaterial.class);
        when(material.isAir()).thenReturn(isAir);
        when(material.isSolid()).thenReturn(solid);
        BlockType type = mock(BlockType.class);
        when(type.id()).thenReturn("minecraft:" + id);
        when(type.getMaterial()).thenReturn(material);
        when(type.getProperties()).thenReturn(List.of());
        BlockState state = mock(BlockState.class);
        when(state.getBlockType()).thenReturn(type);
        when(state.getAsString()).thenReturn("minecraft:" + id + properties);
        BaseBlock block = mock(BaseBlock.class);
        when(block.getBlockType()).thenReturn(type);
        when(block.toImmutableState()).thenReturn(state);
        return block;
    }

    private static BaseBlock withData(BaseBlock block) {
        when(block.getNbtReference()).thenReturn(LazyReference.computed(LinCompoundTag.of(Map.of())));
        return block;
    }

    /** Every chunk is loaded; MockBukkit's chunks do not implement plugin chunk tickets. */
    private static final class LoadedWorld extends WorldMock {
        @Override public boolean isChunkLoaded(int x, int z) { return true; }

        @Override public ChunkMock getChunkAt(int x, int z) {
            ChunkMock chunk = mock(ChunkMock.class);
            when(chunk.getX()).thenReturn(x);
            when(chunk.getZ()).thenReturn(z);
            when(chunk.getWorld()).thenReturn(this);
            when(chunk.isLoaded()).thenReturn(true);
            return chunk;
        }
    }
}
