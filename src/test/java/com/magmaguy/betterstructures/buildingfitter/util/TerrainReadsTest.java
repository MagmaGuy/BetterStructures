package com.magmaguy.betterstructures.buildingfitter.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A natural scan must never make the server thread load a chunk (GitHub #71). */
final class TerrainReadsTest {
    private final World world = mock(World.class);

    @Test
    void refusesAndRecordsUnloadedChunksDuringAScan() {
        when(world.isChunkLoaded(0, 0)).thenReturn(true);
        boolean[] readable = new boolean[3];

        Set<Long> misses = TerrainReads.collectMisses(() -> {
            readable[0] = TerrainReads.readable(new Location(world, 5, 64, 5));
            readable[1] = TerrainReads.readable(new Location(world, 17, 64, -1));
            readable[2] = TerrainReads.readable(new Location(world, 6, 10, 6));
            assertTrue(TerrainReads.incomplete());
        });

        assertTrue(readable[0]);
        assertFalse(readable[1]);
        assertTrue(readable[2]);
        assertEquals(Set.of(TerrainReads.key(1, -1)), misses);
        assertFalse(TerrainReads.incomplete());
        verify(world, never()).getChunkAt(1, -1);
    }

    @Test
    void allowsEveryReadOutsideAScan() {
        assertTrue(TerrainReads.readable(new Location(world, 100, 64, 100)));
        assertFalse(TerrainReads.incomplete());
        verify(world, never()).isChunkLoaded(6, 6);
    }

    @Test
    void scansDoNotNest() {
        assertThrows(IllegalStateException.class,
                () -> TerrainReads.collectMisses(() -> TerrainReads.collectMisses(() -> { })));
        assertTrue(TerrainReads.collectMisses(() -> { }).isEmpty());
    }

    @Test
    void chunkKeysRoundTripNegativeCoordinates() {
        long key = TerrainReads.key(-30_000, 12);
        assertEquals(-30_000, TerrainReads.chunkX(key));
        assertEquals(12, TerrainReads.chunkZ(key));
    }
}
