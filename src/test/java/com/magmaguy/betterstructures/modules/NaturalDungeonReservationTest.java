package com.magmaguy.betterstructures.modules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalDungeonReservationTest {

    @Test
    void rejectsAFrameWhoseFullFootprintReachesAnOldBaseChunk() {
        NaturalDungeonReservation.ChunkCoordinate origin =
                new NaturalDungeonReservation.ChunkCoordinate(9, 0);
        NaturalDungeonReservation.ChunkCoordinate base =
                new NaturalDungeonReservation.ChunkCoordinate(0, 0);
        Set<NaturalDungeonReservation.ChunkCoordinate> generated = Set.of(origin, base);

        assertTrue(NaturalDungeonReservation.tryCreate(
                9 * 16 + 8,
                8,
                5,
                32,
                0,
                generated::contains,
                origin::equals).isEmpty());
    }

    @Test
    void reservesNewAndUngeneratedChunksThenRejectsLateGeneration() {
        NaturalDungeonReservation.ChunkCoordinate origin =
                new NaturalDungeonReservation.ChunkCoordinate(9, 0);
        Set<NaturalDungeonReservation.ChunkCoordinate> generated = new HashSet<>();
        generated.add(origin);

        NaturalDungeonReservation reservation = NaturalDungeonReservation.tryCreate(
                9 * 16 + 8,
                8,
                5,
                32,
                0,
                generated::contains,
                origin::equals).orElseThrow();

        assertTrue(reservation.remainsSafe(generated::contains));
        generated.add(new NaturalDungeonReservation.ChunkCoordinate(0, 0));
        assertFalse(reservation.remainsSafe(generated::contains));
    }

    @Test
    void appliesSpawnProtectionToTheWholeFootprintRatherThanOnlyItsOrigin() {
        assertTrue(NaturalDungeonReservation.tryCreate(
                120,
                0,
                5,
                32,
                100,
                coordinate -> false,
                coordinate -> false).isEmpty());
    }

    @Test
    void calculatesTheReportedNineteenChunkFootprint() {
        NaturalDungeonReservation.BlockBounds bounds =
                NaturalDungeonReservation.blockBounds(9 * 16 + 8, 8, 5, 32);

        assertTrue(bounds.minBlockX() == 8 && bounds.maxBlockX() == 295);
        assertTrue(bounds.minChunkX() == 0 && bounds.maxChunkX() == 18);
        assertTrue(bounds.maxBlockX() - bounds.minBlockX() + 1 == 288);
    }
}
