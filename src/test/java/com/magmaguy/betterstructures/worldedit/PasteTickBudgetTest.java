package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The paste loop ends each tick when its time budget runs out. It also used to end after 4,096 steps, which kept a
 * cheap paste far below its budget: a 334,080-position build took 82 ticks for about 150 ms of work.
 */
final class PasteTickBudgetTest {
    private ServerMock server;

    @BeforeEach
    void open() {
        server = MockBukkit.mock();
        MetadataHandler.PLUGIN = MockBukkit.createMockPlugin("PasteTickBudgetFixture");
    }

    @AfterEach
    void close() {
        Schematic.shutdown();
        MetadataHandler.PLUGIN = null;
        MockBukkit.unmock();
    }

    @Test
    void cheapStepsUseTheWholeBudget() {
        CountedPaste paste = new CountedPaste(20_000, 0);
        Schematic.enqueue(paste);
        server.getScheduler().performOneTick();

        assertEquals(20_000, paste.steps);
        assertTrue(paste.completed);
    }

    @Test
    void theTimeBudgetStillEndsTheTick() {
        CountedPaste paste = new CountedPaste(1_000, 1_000_000L);
        Schematic.enqueue(paste);
        server.getScheduler().performOneTick();

        // The default budget is 10 ms and each step takes 1 ms.
        assertTrue(paste.steps >= 9 && paste.steps <= 12, "Steps in one tick: " + paste.steps);
        assertFalse(paste.completed);
    }

    private static final class CountedPaste implements Schematic.PasteOperation {
        private final int total;
        private final long nanosPerStep;
        private int steps;
        private boolean completed;

        private CountedPaste(int total, long nanosPerStep) {
            this.total = total;
            this.nanosPerStep = nanosPerStep;
        }

        @Override public boolean hasNext() { return steps < total; }

        @Override public void pasteNext() {
            long end = System.nanoTime() + nanosPerStep;
            while (System.nanoTime() < end) Thread.onSpinWait();
            steps++;
        }

        @Override public void onComplete() { completed = true; }
    }
}
