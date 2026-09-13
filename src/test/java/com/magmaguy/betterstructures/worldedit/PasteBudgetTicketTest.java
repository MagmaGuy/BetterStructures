package com.magmaguy.betterstructures.worldedit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression coverage for the configured paste budget used by the main-thread loop. */
final class PasteBudgetTicketTest {
    @Test
    void lowConfigurationStillHasBoundedTwoMillisecondBudget() {
        assertEquals(2_000_000L, PasteBudget.nanosPerTick(0.01));
    }

    @Test
    void normalConfigurationUsesConfiguredFractionOfFiftyMillisecondTick() {
        assertEquals(10_000_000L, PasteBudget.nanosPerTick(0.20));
    }
}
