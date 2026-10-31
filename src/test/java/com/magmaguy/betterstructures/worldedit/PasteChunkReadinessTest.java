package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.util.PluginChunkHolds;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A paste walks its blocks layer by layer, so it comes back to the same chunks over and over. Releasing
 * each chunk as the paste moved on let it unload with nobody near, and the next layer loaded it again:
 * one 60x87x64 build loaded chunks 85,955 times and took four minutes to paste.
 */
final class PasteChunkReadinessTest {
    private final World world = mock(World.class);
    private final Map<Long, Chunk> chunks = new HashMap<>();
    // MockBukkit does not implement plugin chunk tickets; these chunks record theirs here.
    private final Set<Long> tickets = new HashSet<>();
    private int ticketsAdded;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        when(world.getUID()).thenReturn(id);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getChunkAt(anyInt(), anyInt())).thenAnswer(call -> chunk(call.getArgument(0), call.getArgument(1)));
        MockBukkit.mock(new ServerMock() {
            @Override
            public World getWorld(UUID uid) {
                return uid.equals(id) ? world : null;
            }
        });
        MetadataHandler.PLUGIN = MockBukkit.createMockPlugin("PasteChunkReadinessFixture");
    }

    @AfterEach
    void tearDown() {
        PluginChunkHolds.shutdown();
        MetadataHandler.PLUGIN = null;
        MockBukkit.unmock();
    }

    @Test
    void keepsEveryChunkItReturnsToHeldUntilClosed() {
        PasteChunkReadiness readiness = new PasteChunkReadiness(world);

        for (int layer = 0; layer < 10; layer++) {
            assertTrue(readiness.ready(new Location(world, 5, layer, 5)));
            assertTrue(readiness.ready(new Location(world, 20, layer, 5)));
        }

        assertEquals(Set.of(key(0, 0), key(1, 0)), tickets);
        assertEquals(2, ticketsAdded);
        readiness.close();
        assertTrue(tickets.isEmpty());
    }

    @Test
    void releasesTheLeastRecentlyUsedChunkPastItsLimit() {
        PasteChunkReadiness readiness = new PasteChunkReadiness(world);
        int limit = PasteChunkReadiness.MAX_HELD_CHUNKS;

        for (int x = 0; x < limit; x++) assertTrue(readiness.ready(new Location(world, x * 16 + 1, 64, 1)));
        // Chunk 0 is used again, so chunk 1 is now the least recently used.
        assertTrue(readiness.ready(new Location(world, 1, 64, 1)));
        assertTrue(readiness.ready(new Location(world, limit * 16 + 1, 64, 1)));

        assertEquals(limit, tickets.size());
        assertTrue(tickets.containsAll(List.of(key(0, 0), key(limit, 0))));
        assertFalse(tickets.contains(key(1, 0)));
        readiness.close();
        assertTrue(tickets.isEmpty());
    }

    private Chunk chunk(int x, int z) {
        return chunks.computeIfAbsent(key(x, z), key -> {
            Chunk chunk = mock(Chunk.class);
            when(chunk.getX()).thenReturn(x);
            when(chunk.getZ()).thenReturn(z);
            when(chunk.getWorld()).thenReturn(world);
            when(chunk.isLoaded()).thenReturn(true);
            doAnswer(call -> {
                ticketsAdded++;
                return tickets.add(key);
            }).when(chunk).addPluginChunkTicket(any());
            doAnswer(call -> tickets.remove(key)).when(chunk).removePluginChunkTicket(any());
            return chunk;
        });
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
