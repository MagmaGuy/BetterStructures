package com.magmaguy.betterstructures.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedDungeonScanTrackerTest {

    @Test
    void preservesPendingScansWhenReloadCancelsTheirBukkitTasks() {
        DelayedDungeonScanTracker<String> tracker = new DelayedDungeonScanTracker<>();

        assertTrue(tracker.markPending("world:4,7"));
        tracker.deferAllPending();

        assertFalse(tracker.takePending("world:4,7"));
        assertTrue(tracker.isDeferred("world:4,7"));

        tracker.removeDeferred("world:4,7");
        assertTrue(tracker.markPending("world:4,7"));
        assertTrue(tracker.takePending("world:4,7"));
    }

    @Test
    void deduplicatesPendingAndDeferredCoordinates() {
        DelayedDungeonScanTracker<String> tracker = new DelayedDungeonScanTracker<>();

        assertTrue(tracker.markPending("world:1,2"));
        assertFalse(tracker.markPending("world:1,2"));
        tracker.defer("world:1,2");
        tracker.defer("world:1,2");

        assertTrue(tracker.hasDeferred());
        assertEquals(1, tracker.deferredSnapshot().size());
    }
}
