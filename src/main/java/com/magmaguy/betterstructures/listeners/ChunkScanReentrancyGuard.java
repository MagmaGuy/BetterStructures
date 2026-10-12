package com.magmaguy.betterstructures.listeners;

/**
 * Suppresses nested chunk scans caused by terrain lookups or block placement.
 * A nested new-chunk event is a side effect of BetterStructures' own scan; replaying
 * it later can recursively fan out across every neighboring chunk.
 */
final class ChunkScanReentrancyGuard {

    private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

    boolean runIfIdle(Runnable scan) {
        if (active.get()) return false;

        active.set(true);
        try {
            scan.run();
            return true;
        } finally {
            active.remove();
        }
    }
}
