package com.magmaguy.betterstructures.modules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalDungeonReservationTest {

    @Test
    void appliesSpawnProtectionToTheWholeFootprintRatherThanOnlyItsOrigin() {
        assertTrue(NaturalDungeonReservation.tryCreate(120, 0, 5, 32, 100).isEmpty());
    }

    @Test
    void acceptsAFootprintOutsideSpawnProtection() {
        assertTrue(NaturalDungeonReservation.tryCreate(4800, 4800, 5, 32, 100).isPresent());
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
