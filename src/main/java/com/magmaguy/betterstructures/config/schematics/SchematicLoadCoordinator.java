package com.magmaguy.betterstructures.config.schematics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Owns the worker pool used by one schematic-load generation.
 *
 * <p>Closing the lifecycle prevents a late initialization worker from creating a new pool. An
 * already active load is canceled and can be joined before WorldEdit is disabled.
 */
final class SchematicLoadCoordinator {
    private final Object lifecycleLock = new Object();
    private boolean acceptingLoads;
    private ActiveLoad activeLoad;

    void open() {
        synchronized (lifecycleLock) {
            clearQuiescentLoad();
            if (activeLoad != null) {
                throw new IllegalStateException(
                        "The previous schematic loader is still shutting down.");
            }
            acceptingLoads = true;
        }
    }

    ActiveLoad begin(int parallelism) {
        synchronized (lifecycleLock) {
            clearQuiescentLoad();
            if (!acceptingLoads) {
                throw new CancellationException(
                        "Schematic loading is closed because BetterStructures is shutting down.");
            }
            if (activeLoad != null) {
                throw new IllegalStateException("A schematic load is already running.");
            }
            activeLoad = new ActiveLoad(parallelism, Thread.currentThread());
            return activeLoad;
        }
    }

    boolean shutdownAndAwait(Duration timeout) {
        ActiveLoad load;
        synchronized (lifecycleLock) {
            acceptingLoads = false;
            clearQuiescentLoad();
            load = activeLoad;
        }
        if (load == null) return true;

        load.cancel();
        boolean stopped = load.awaitQuiescence(timeout);
        if (stopped) release(load);
        return stopped;
    }

    private void release(ActiveLoad load) {
        synchronized (lifecycleLock) {
            if (activeLoad == load && load.isQuiescent()) activeLoad = null;
        }
    }

    private void clearQuiescentLoad() {
        if (activeLoad != null && activeLoad.isQuiescent()) activeLoad = null;
    }

    final class ActiveLoad {
        private final ExecutorService executor;
        private final Thread ownerThread;
        private final CountDownLatch ownerFinished = new CountDownLatch(1);
        private final List<Future<?>> futures = new ArrayList<>();
        private boolean acceptingTasks = true;
        private boolean cancellationRequested;

        private ActiveLoad(int parallelism, Thread ownerThread) {
            if (parallelism < 1) throw new IllegalArgumentException("parallelism must be positive");
            this.ownerThread = ownerThread;
            this.executor = Executors.newFixedThreadPool(parallelism, runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "BetterStructures-SchematicLoader");
                thread.setDaemon(true);
                return thread;
            });
        }

        synchronized <T> Future<T> submit(Callable<T> task) {
            checkRunning();
            if (!acceptingTasks) {
                throw new IllegalStateException(
                        "This schematic load no longer accepts tasks.");
            }

            Future<T> future = executor.submit(() -> {
                checkRunning();
                T result = task.call();
                checkRunning();
                return result;
            });
            // submit and tracking share the same lock as cancel(), so shutdown cannot leave a
            // successfully queued future outside the cancellation set.
            futures.add(future);
            return future;
        }

        synchronized void checkRunning() {
            if (cancellationRequested || Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Schematic loading was canceled.");
            }
        }

        void finish(Duration timeout) {
            synchronized (this) {
                checkRunning();
                acceptingTasks = false;
                executor.shutdown();
            }

            boolean stopped = awaitExecutor(timeout);
            markOwnerFinished();
            if (!stopped) {
                cancel();
                throw new IllegalStateException(
                        "Timed out waiting for the schematic loader to finish.");
            }
            release(this);
        }

        void abort(Duration timeout) {
            cancel();
            markOwnerFinished();
            awaitExecutor(timeout);
            release(this);
        }

        private void cancel() {
            boolean interruptOwner;
            synchronized (this) {
                cancellationRequested = true;
                acceptingTasks = false;
                for (Future<?> future : futures) future.cancel(true);
                executor.shutdownNow();
                interruptOwner = ownerFinished.getCount() != 0
                        && ownerThread != Thread.currentThread();
            }
            if (interruptOwner) ownerThread.interrupt();
        }

        private boolean awaitQuiescence(Duration timeout) {
            long deadline = deadline(timeout);
            if (!awaitExecutor(remaining(deadline))) return false;
            if (!awaitOwner(remaining(deadline))) return false;
            return isQuiescent();
        }

        private boolean awaitExecutor(Duration timeout) {
            try {
                return executor.awaitTermination(
                        Math.max(0L, timeout.toNanos()),
                        TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private boolean awaitOwner(Duration timeout) {
            try {
                return ownerFinished.await(
                        Math.max(0L, timeout.toNanos()),
                        TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private void markOwnerFinished() {
            ownerFinished.countDown();
        }

        private boolean isQuiescent() {
            return executor.isTerminated() && ownerFinished.getCount() == 0;
        }
    }

    private static long deadline(Duration timeout) {
        long now = System.nanoTime();
        long nanos = Math.max(0L, timeout.toNanos());
        if (Long.MAX_VALUE - now < nanos) return Long.MAX_VALUE;
        return now + nanos;
    }

    private static Duration remaining(long deadline) {
        return Duration.ofNanos(Math.max(0L, deadline - System.nanoTime()));
    }
}
