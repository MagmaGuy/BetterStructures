package com.magmaguy.betterstructures.util;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldEditUtilsTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createsSingleBlockClipboardWithStableOneBlockGeometry() {
        BlockState blockState = mock(BlockState.class);
        BaseBlock baseBlock = mock(BaseBlock.class);
        Location location = new Location(world, 4, 65, -3);

        Clipboard clipboard = WorldEditUtils.createSingleBlockClipboard(location, baseBlock, blockState);

        assertEquals(BlockVector3.at(0, 0, 0), clipboard.getMinimumPoint());
        assertEquals(BlockVector3.at(0, 0, 0), clipboard.getMaximumPoint());
        assertEquals(BlockVector3.at(1, 1, 1), clipboard.getDimensions());
        assertEquals(blockState, clipboard.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(baseBlock, clipboard.getFullBlock(BlockVector3.at(0, 0, 0)));
        assertTrue(clipboard.getEntities().isEmpty());
    }

    @Test
    void readsLegacyWorldEditSignLinesFromNbt() {
        BaseBlock baseBlock = mock(BaseBlock.class);
        when(baseBlock.getNbtData()).thenReturn(new CompoundTag(Map.of(
                "Text1", new StringTag("{\"text\":\"[chest]\"}"),
                "Text2", new StringTag("{\"text\":\"rare\"}"),
                "Text3", new StringTag("{\"text\":\"\"}"),
                "Text4", new StringTag("{\"text\":\"\"}")
        )));

        List<String> lines = WorldEditUtils.getLines(baseBlock);

        assertEquals(List.of("[chest]", "rare"), lines);
    }

    @Test
    void readsModernWorldEditSignLinesFromNbt() {
        BaseBlock baseBlock = mock(BaseBlock.class);
        ListTag messages = new ListTag(StringTag.class, List.of(
                new StringTag("{\"text\":\"[spawn]\"}"),
                new StringTag("{\"text\":\"zombie\"}"),
                new StringTag("\"\""),
                new StringTag("\"\"")
        ));
        CompoundTag frontText = new CompoundTag(Map.of("messages", messages));
        when(baseBlock.getNbtData()).thenReturn(new CompoundTag(Map.of("front_text", frontText)));

        List<String> lines = WorldEditUtils.getLines(baseBlock);

        assertEquals(List.of("[spawn]", "zombie"), lines);
    }
}
