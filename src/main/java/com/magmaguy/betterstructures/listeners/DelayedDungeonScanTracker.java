package com.magmaguy.betterstructures.listeners;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks one-tick dungeon scans separately from full new-chunk scans.
 *
 * <p>BetterStructures cancels all of its Bukkit tasks when a content reload starts. A dungeon
 * scan that was delayed by one tick must therefore survive that cancellation without causing the
 * already-completed surface and underground scans to run a second time.</p>
 */
final class DelayedDungeonScanTracker<K> {
    private final Set<K> pending = new LinkedHashSet<>();
    private final Set<K> deferred = new LinkedHashSet<>();

    boolean markPending(K key) {
        return pending.add(key);
    }

    boolean takePending(K key) {
        return pending.remove(key);
    }

    void defer(K key) {
        pending.remove(key);
        deferred.add(key);
    }

    void deferAllPending() {
        deferred.addAll(pending);
        pending.clear();
    }

    boolean isDeferred(K key) {
        return deferred.contains(key);
    }

    void removeDeferred(K key) {
        deferred.remove(key);
    }

    boolean hasDeferred() {
        return !deferred.isEmpty();
    }

    List<K> deferredSnapshot() {
        return List.copyOf(deferred);
    }

    void clear() {
        pending.clear();
        deferred.clear();
    }
}
