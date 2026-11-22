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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    /**
     * A block-entity write reads two blocks out, so a write beside a chunk border loaded the next chunk on
     * the server thread. When Paper had left that chunk stuck partway through generation, the server froze.
     */
    @Test
    void waitsForTheChunksRoundTheBlockItWrites() {
        Set<Long> loaded = loadedRound(0, 0);
        loaded.remove(key(1, 0));
        Map<Long, CompletableFuture<Chunk>> requests = requests(loaded);
        PasteChunkReadiness readiness = new PasteChunkReadiness(world);

        assertFalse(readiness.ready(14, 8), "The chunk east of the block is not loaded yet");
        assertFalse(readiness.ready(14, 8));
        assertEquals(Set.of(key(1, 0)), requests.keySet(), "Only the missing chunk is requested, and once");
        assertEquals(Set.of(key(0, 0)), tickets, "The block's own chunk is held while it waits");

        loaded.add(key(1, 0));
        requests.get(key(1, 0)).complete(chunk(1, 0));
        assertTrue(readiness.ready(14, 8));
        assertEquals(Set.of(key(0, 0)), tickets, "The chunks round it are waited for, not held");
        readiness.close();
        assertTrue(tickets.isEmpty());
    }

    @Test
    void givesUpOnAChunkThatNeverLoads() {
        Set<Long> loaded = loadedRound(0, 0);
        loaded.remove(key(1, 0));
        requests(loaded);
        long timeout = PasteChunkReadiness.loadTimeoutNanos;
        PasteChunkReadiness readiness = new PasteChunkReadiness(world);
        try {
            PasteChunkReadiness.loadTimeoutNanos = -1;
            assertFalse(readiness.ready(14, 8));
            PasteChunkReadiness.LoadTimeout gaveUp =
                    assertThrows(PasteChunkReadiness.LoadTimeout.class, () -> readiness.ready(14, 8));
            assertTrue(gaveUp.getMessage().startsWith("chunk 1, 0 in "), gaveUp.getMessage());
        } finally {
            PasteChunkReadiness.loadTimeoutNanos = timeout;
            readiness.close();
        }
        assertTrue(tickets.isEmpty());
    }

    @Test
    void readiesAWholeFootprintBeforeTheFirstWrite() {
        Set<Long> loaded = loadedRound(0, 0);
        loaded.addAll(loadedRound(1, 0));
        loaded.remove(key(2, 1));
        Map<Long, CompletableFuture<Chunk>> requests = requests(loaded);
        PasteChunkReadiness readiness = new PasteChunkReadiness(world);

        assertFalse(readiness.readyAll(3, 3, 20, 12), "A chunk beside the far end of the footprint is missing");
        assertEquals(Set.of(key(2, 1)), requests.keySet());

        loaded.add(key(2, 1));
        requests.get(key(2, 1)).complete(chunk(2, 1));
        assertTrue(readiness.readyAll(3, 3, 20, 12));
        assertEquals(Set.of(key(0, 0), key(1, 0)), tickets);
        readiness.close();
    }

    private static Set<Long> loadedRound(int x, int z) {
        Set<Long> loaded = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) loaded.add(key(x + dx, z + dz));
        return loaded;
    }

    /** Only these chunks are loaded; every asynchronous request for another is recorded and left open. */
    private Map<Long, CompletableFuture<Chunk>> requests(Set<Long> loaded) {
        Map<Long, CompletableFuture<Chunk>> requests = new HashMap<>();
        when(world.isChunkLoaded(anyInt(), anyInt()))
                .thenAnswer(call -> loaded.contains(key(call.getArgument(0), call.getArgument(1))));
        when(world.getChunkAtAsync(anyInt(), anyInt(), anyBoolean())).thenAnswer(call -> {
            CompletableFuture<Chunk> request = new CompletableFuture<>();
            assertNull(requests.put(key(call.getArgument(0), call.getArgument(1)), request), "Requested twice");
            return request;
        });
        return requests;
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
