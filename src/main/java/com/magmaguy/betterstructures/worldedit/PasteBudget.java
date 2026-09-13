package com.magmaguy.betterstructures.worldedit;

final class PasteBudget {
    private PasteBudget() { }
    static long nanosPerTick(double percentage) {
        return Math.max((long) (50_000_000D * percentage), 2_000_000L);
    }
}
