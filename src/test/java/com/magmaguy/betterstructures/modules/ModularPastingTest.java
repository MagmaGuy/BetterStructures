package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.ChunkMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real modular queue, scheduler, chunk gate, Bukkit writes, completion event and cancellation. */
class ModularPastingTest {
    @TempDir Path folder;
    private ServerMock server;
    private JavaPlugin plugin;
    private CountingWorld world;
    private final List<WorldGenerationFinishEvent> completions = new ArrayList<>();
    private final List<LogRecord> warnings = new ArrayList<>();
    private Handler logCapture;

    @BeforeEach
    void open() {
        server = MockBukkit.mock(new ServerMock() {
            @Override public BlockData createBlockData(String data) {
                BlockData block = super.createBlockData(data);
                if (block.getMaterial() != Material.GLOWSTONE && block.getMaterial() != Material.SEA_LANTERN) return block;
                // MockBukkit omits emission metadata; these genuine luminous blocks use the Bukkit paste phase.
                BlockData luminous = spy(block);
                doReturn(15).when(luminous).getLightEmission();
                return luminous;
            }
        });
        plugin = MockBukkit.createMockPlugin("ModularPasteFixture");
        MetadataHandler.PLUGIN = plugin;
        world = new CountingWorld();
        world.setName("modular-paste-fixture");
        server.addWorld(world);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler public void finished(WorldGenerationFinishEvent event) { completions.add(event); }
        }, plugin);
        logCapture = new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        server.getLogger().addHandler(logCapture);
    }

    @AfterEach
    void close() {
        Schematic.shutdown();
        if (logCapture != null) server.getLogger().removeHandler(logCapture);
        MetadataHandler.PLUGIN = null;
        MockBukkit.unmock();
    }

    @ParameterizedTest(name = "{displayName} rotation={0}")
    @CsvSource({"0,0,0,1,0,0,1", "90,1,0,1,1,0,0", "180,1,1,0,1,1,0", "270,0,1,0,0,1,1"})
    void naturalPasteWritesRotatedBlocksAndCarvesAir(int rotation,
            int airX, int airZ, int glowX, int glowZ, int barrierX, int barrierZ) {
        Location origin = new Location(world, -17, 70, 31);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++)
            world.getBlockAt(origin.getBlockX() + x, 70, origin.getBlockZ() + z).setType(Material.DIRT);
        Map<BlockVector3, BaseBlock> blocks = Map.of(
                BlockVector3.at(3, 4, 5), block("air", true),
                BlockVector3.at(4, 4, 5), block("glowstone", false),
                BlockVector3.at(3, 4, 6), block("barrier", false),
                BlockVector3.at(4, 4, 6), block("sea_lantern", false));
        enqueue(false, origin, rotation, BlockVector3.at(3, 4, 5), BlockVector3.at(4, 4, 6), blocks);

        drain();

        assertEquals(Material.AIR, at(origin, airX, airZ));
        assertEquals(Material.GLOWSTONE, at(origin, glowX, glowZ));
        assertEquals(Material.DIRT, at(origin, barrierX, barrierZ), "Barrier markers must not overwrite terrain");
        assertEquals(1, countMaterial(origin, Material.SEA_LANTERN),
                "The fourth source cell must reach its distinct rotated destination");
        assertTrue(completions.isEmpty(), "Natural-world pastes must not announce a generated instance");
        assertTrue(warnings.isEmpty(), () -> "Unexpected paste warnings: " + warnings);
    }

    @Test
    void voidPasteSkipsAirAndBarrierChunkRequestsAndCompletesOnce() {
        Location origin = new Location(world, 0, 70, 0);
        BaseBlock air = block("air", true), barrier = block("barrier", false);
        Map<BlockVector3, BaseBlock> blocks = new HashMap<>();
        for (int x = 0; x <= 32; x++) blocks.put(BlockVector3.at(x, 0, 0), x < 16 ? air : barrier);
        blocks.put(BlockVector3.at(32, 0, 0), block("glowstone", false));
        world.getBlockAt(0, 70, 0).setType(Material.DIRT);
        enqueue(true, origin, 0, BlockVector3.ZERO, BlockVector3.at(32, 0, 0), blocks);

        drain();
        server.getScheduler().performTicks(3);

        assertEquals(Material.DIRT, world.getBlockAt(0, 70, 0).getType(), "Void-world air must not be written");
        assertEquals(Material.GLOWSTONE, world.getBlockAt(32, 70, 0).getType());
        // The written chunk, then the eight round it, which a write beside a chunk border reads.
        assertEquals(List.of("2,0", "1,-1", "1,0", "1,1", "2,-1", "2,1", "3,-1", "3,0", "3,1"), world.requestedChunks,
                "Empty and barrier-only chunks must not be readied for themselves");
        assertEquals(1, completions.size());
        assertSame(world, completions.getFirst().getModularWorld().getWorld());
        assertEquals(new Location(world, 8, 62, 8), completions.getFirst().getModularWorld().getCenter());
        assertEquals(48D, completions.getFirst().getModularWorld().getHorizontalSize());
        assertTrue(warnings.isEmpty(), () -> "Unexpected paste warnings: " + warnings);
    }

    @Test
    void unloadingAnOwnedWorldCancelsQueuedPasteWithoutFailureOrCompletion() {
        enqueue(true, new Location(world, 0, 70, 0), 0, BlockVector3.ZERO, BlockVector3.ZERO,
                Map.of(BlockVector3.ZERO, block("glowstone", false)));
        assertTrue(server.unloadWorld(world, false));

        drain();

        assertTrue(completions.isEmpty(), "A cancelled world's partial paste must never announce completion");
        assertTrue(warnings.isEmpty(), () -> "Expected world cancellation logged a paste failure: " + warnings);
        assertTrue(world.requestedChunks.isEmpty());
    }

    @ParameterizedTest(name = "{displayName} moduleWidth={0}")
    @CsvSource({"32,-2,47,160", "33,-1.5,47.5,165"})
    void completionRetainsTheTranslatedGeneratorGeometry(int moduleWidth, double centerX, double centerZ, double size) {
        enqueue(true, new Location(world, -17.8, 70.4, 31.75), 0, BlockVector3.ZERO, BlockVector3.ZERO,
                Map.of(BlockVector3.ZERO, block("glowstone", false)), 3, moduleWidth);

        drain();

        assertEquals(1, completions.size());
        ModularWorld generated = completions.getFirst().getModularWorld();
        assertEquals(new Location(world, centerX, 62, centerZ), generated.getCenter());
        assertEquals(size, generated.getHorizontalSize());
    }

    @ParameterizedTest(name = "{displayName} rotation={0}")
    @ValueSource(ints = {0, 90, 180, 270})
    void markerSpawnKeepsItsFeetAtAuthoredHeightAndItsBodyClearOfAnAdjacentWall(int rotation) {
        Location marker = new Location(world, -120, 70, 41);
        world.getBlockAt(-121, 70, 41).setType(Material.STONE);
        world.getBlockAt(-121, 71, 41).setType(Material.STONE);
        BaseBlock sign = block("oak_sign", false);
        when(sign.getNbtData()).thenReturn(new com.sk89q.jnbt.CompoundTag(Map.of(
                "Text1", new com.sk89q.jnbt.StringTag("{\"text\":\"[spawn]\"}"),
                "Text2", new com.sk89q.jnbt.StringTag("{\"text\":\"VINDICATOR\"}"),
                "Text3", new com.sk89q.jnbt.StringTag("{\"text\":\"\"}"),
                "Text4", new com.sk89q.jnbt.StringTag("{\"text\":\"\"}"))));
        enqueue(false, marker, rotation, BlockVector3.ZERO, BlockVector3.ZERO, Map.of(BlockVector3.ZERO, sign));

        drain();

        var entities = world.getEntities().stream().filter(entity -> entity.getType() == EntityType.VINDICATOR).toList();
        assertEquals(1, entities.size());
        var entity = entities.getFirst();
        assertEquals(new Location(world, -119.5, 70, 41.5), entity.getLocation());
        BoundingBox wall = new BoundingBox(-121, 70, 41, -120, 72, 42);
        assertFalse(entity.getBoundingBox().overlaps(wall), "The real entity bounding box must clear the adjacent solid wall");
        assertTrue(entity.getBoundingBox().clone().shift(-.5, 0, -.5).overlaps(wall),
                "The original block-corner spawn intersects this same wall");
        assertEquals(Material.AIR, marker.getBlock().getType());
        assertTrue(warnings.isEmpty(), () -> "Unexpected paste warnings: " + warnings);
    }

    private void enqueue(boolean generatedWorld, Location origin, int rotation, BlockVector3 low, BlockVector3 high,
                         Map<BlockVector3, BaseBlock> source) {
        enqueue(generatedWorld, origin, rotation, low, high, source, 2, 16);
    }

    private void enqueue(boolean generatedWorld, Location origin, int rotation, BlockVector3 low, BlockVector3 high,
                         Map<BlockVector3, BaseBlock> source, int radius, int moduleSize) {
        Clipboard clipboard = mock(Clipboard.class);
        when(clipboard.getMinimumPoint()).thenReturn(low);
        when(clipboard.getMaximumPoint()).thenReturn(high);
        when(clipboard.getOrigin()).thenReturn(low);
        when(clipboard.getEntities()).thenReturn(List.of());
        when(clipboard.getFullBlock(any())).thenAnswer(call -> {
            BaseBlock value = source.get(call.getArgument(0));
            if (value == null) throw new AssertionError("Read outside source clipboard: " + call.getArgument(0));
            return value;
        });
        ModuleGeneratorsConfigFields config = mock(ModuleGeneratorsConfigFields.class);
        when(config.isWorldGeneration()).thenReturn(generatedWorld);
        when(config.getRadius()).thenReturn(radius);
        when(config.getModuleSizeXZ()).thenReturn(moduleSize);
        WFCGenerator generator = mock(WFCGenerator.class);
        when(generator.getModuleGeneratorsConfigFields()).thenReturn(config);
        ModulesContainer module = mock(ModulesContainer.class);
        when(module.getClipboard()).thenReturn(clipboard);
        when(module.getRotation()).thenReturn(rotation);
        WFCLattice lattice = new WFCLattice(radius, moduleSize, 16, 0, 0);
        WFCNode node = new WFCNode(new Vector3i(), world, lattice, new HashMap<>(), generator);
        node.setModulesContainer(module);
        // Lattice origin cells begin half a module west/north and half a module above the generator origin.
        Location generatorOrigin = origin.clone().add(moduleSize / 2, -8, moduleSize / 2);
        new ModulePasting(world, folder.toFile(), new ArrayDeque<>(List.of(node)), "", generatorOrigin, config);
    }

    private void drain() {
        for (int tick = 0; tick < 100 && server.getScheduler().getPendingTasks().stream()
                .anyMatch(task -> task.getOwner() == plugin); tick++) server.getScheduler().performOneTick();
        assertTrue(server.getScheduler().getPendingTasks().stream().noneMatch(task -> task.getOwner() == plugin),
                "The production paste queue did not drain");
    }

    private Material at(Location origin, int x, int z) {
        return world.getBlockAt(origin.getBlockX() + x, origin.getBlockY(), origin.getBlockZ() + z).getType();
    }

    private long countMaterial(Location origin, Material material) {
        long count = 0;
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) if (at(origin, x, z) == material) count++;
        return count;
    }

    private static BaseBlock block(String id, boolean air) {
        BlockMaterial material = mock(BlockMaterial.class);
        when(material.isAir()).thenReturn(air);
        BlockType type = mock(BlockType.class);
        when(type.id()).thenReturn("minecraft:" + id);
        when(type.getMaterial()).thenReturn(material);
        when(type.getProperties()).thenReturn(List.of());
        BlockState state = mock(BlockState.class);
        when(state.getBlockType()).thenReturn(type);
        when(state.getAsString()).thenReturn("minecraft:" + id);
        BaseBlock block = mock(BaseBlock.class);
        when(block.getBlockType()).thenReturn(type);
        when(block.toImmutableState()).thenReturn(state);
        return block;
    }

    private static final class CountingWorld extends WorldMock {
        private final List<String> requestedChunks = new ArrayList<>();

        @Override public org.bukkit.entity.Entity spawnEntity(Location location, EntityType type) {
            var entity = super.spawnEntity(location, type);
            if (!(entity instanceof org.bukkit.entity.LivingEntity living)) return entity;
            // MockBukkit supports the real entity geometry but omits this despawn-policy setter.
            var supported = spy(living);
            doNothing().when(supported).setRemoveWhenFarAway(false);
            return supported;
        }

        @Override public boolean isChunkLoaded(int x, int z) {
            requestedChunks.add(x + "," + z);
            return true;
        }

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
