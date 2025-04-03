package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;

public class ModularGenerationStatus {

    // A collection to track all boss bars (for proper shutdown)
    private static final HashSet<BossBar> bossBars = new HashSet<>();
    @Getter
    private Player generatingPlayer;
    // Boss bars for each phase
    private BossBar barInitializing;
    private BossBar barReservingChunks;
    private BossBar barArrangingModules;
    private BossBar barPreparingPlacement;
    private BossBar barPlacingFastBlocks;
    private BossBar barPlacingSlowBlocks;
    private BossBar barReadingWorldFeatures;
    private BossBar barPlacingMobs;

    // Progress values (0.0 to 1.0) for each phase
    private double progressInitializing = 0.0;
    private double progressReservingChunks = 0.0;
    private double progressArrangingModules = 0.0;
    private double progressPreparingPlacement = 0.0;
    private double progressPlacingFastBlocks = 0.0;
    private double progressPlacingSlowBlocks = 0.0;
    private double progressReadingWorldFeatures = 0.0;
    private double progressPlacingMobs = 0.0;

    // Boolean statuses for each phase (false = not finished; true = finished)
    private boolean initializingDone = false;
    private boolean reservingChunksDone = false;
    private boolean arrangingModulesDone = false;
    private boolean preparingPlacementDone = false;
    private boolean placingFastBlocksDone = false;
    private boolean placingSlowBlocksDone = false;
    private boolean readingWorldFeaturesDone = false;
    private boolean placingMobsDone = false;
    private boolean ready = false;

    // Start times for each phase
    private long startTimeInitializing;
    private long startTimeReservingChunks;
    private long startTimeArrangingModules;
    private long startTimePreparingPlacement;
    private long startTimePlacingFastBlocks;
    private long startTimePlacingSlowBlocks;
    private long startTimeReadingWorldFeatures;
    private long startTimePlacingMobs;

    // Overall start time for the entire generation process
    private long overallStartTime;

    private BukkitTask bossBarTask;

    public ModularGenerationStatus(Player player) {
        this.generatingPlayer = player;
        overallStartTime = System.currentTimeMillis();
        startInitializing();
        startBossBarTask();
    }

    public ModularGenerationStatus() {
        overallStartTime = System.currentTimeMillis();
        startInitializing();
        startBossBarTask();
    }

    public static void shutdown() {
        for (BossBar bar : bossBars) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    // Start a repeating task to update all boss bars
    private void startBossBarTask() {
        bossBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllBossBars();
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    // Update all boss bars with the current progress and status.
    private void updateAllBossBars() {
        if (barInitializing != null) {
            String title = "Initializing: " + (initializingDone ? "Done" : String.format("%.0f%%", progressInitializing * 100));
            barInitializing.setTitle(title);
            barInitializing.setProgress(progressInitializing);
        }
        if (barReservingChunks != null) {
            String title = "Reserving Chunks: " + (reservingChunksDone ? "Done" : String.format("%.0f%%", progressReservingChunks * 100));
            barReservingChunks.setTitle(title);
            barReservingChunks.setProgress(progressReservingChunks);
        }
        if (barArrangingModules != null) {
            String title = "Arranging Modules: " + (arrangingModulesDone ? "Done" : String.format("%.0f%%", progressArrangingModules * 100));
            barArrangingModules.setTitle(title);
            barArrangingModules.setProgress(progressArrangingModules);
        }
        if (barPreparingPlacement != null) {
            String title = "Preparing Placement: " + (preparingPlacementDone ? "Done" : String.format("%.0f%%", progressPreparingPlacement * 100));
            barPreparingPlacement.setTitle(title);
            barPreparingPlacement.setProgress(progressPreparingPlacement);
        }
        if (barPlacingFastBlocks != null) {
            String title = "Placing Fast Blocks: " + (placingFastBlocksDone ? "Done" : String.format("%.0f%%", progressPlacingFastBlocks * 100));
            barPlacingFastBlocks.setTitle(title);
            barPlacingFastBlocks.setProgress(progressPlacingFastBlocks);
        }
        if (barPlacingSlowBlocks != null) {
            String title = "Placing Slow Blocks: " + (placingSlowBlocksDone ? "Done" : String.format("%.0f%%", progressPlacingSlowBlocks * 100));
            barPlacingSlowBlocks.setTitle(title);
            barPlacingSlowBlocks.setProgress(progressPlacingSlowBlocks);
        }
        if (barReadingWorldFeatures != null) {
            String title = "Reading World Features: " + (readingWorldFeaturesDone ? "Done" : String.format("%.0f%%", progressReadingWorldFeatures * 100));
            barReadingWorldFeatures.setTitle(title);
            barReadingWorldFeatures.setProgress(progressReadingWorldFeatures);
        }
        if (barPlacingMobs != null) {
            String title = "Placing Mobs: " + (placingMobsDone ? "Done" : String.format("%.0f%%", progressPlacingMobs * 100));
            barPlacingMobs.setTitle(title);
            barPlacingMobs.setProgress(progressPlacingMobs);
        }
    }

    // Creates a BossBar for a specific phase.
    private BossBar createBossBar(String phaseName) {
        BossBar bar = Bukkit.createBossBar(phaseName, BarColor.GREEN, BarStyle.SOLID);
        if (generatingPlayer != null) {
            bar.addPlayer(generatingPlayer);
        }
        bossBars.add(bar);
        return bar;
    }

    // --- Phase Start Methods ---
    public void startInitializing() {
        startTimeInitializing = System.currentTimeMillis();
        progressInitializing = 0.0;
        initializingDone = false;
        if (barInitializing == null) {
            barInitializing = createBossBar("Initializing");
        }
    }

    public void startReservingChunks() {
        startTimeReservingChunks = System.currentTimeMillis();
        progressReservingChunks = 0.0;
        reservingChunksDone = false;
        if (barReservingChunks == null) {
            barReservingChunks = createBossBar("Reserving Chunks");
        }
    }

    public void startArrangingModules() {
        startTimeArrangingModules = System.currentTimeMillis();
        progressArrangingModules = 0.0;
        arrangingModulesDone = false;
        if (barArrangingModules == null) {
            barArrangingModules = createBossBar("Arranging Modules");
        }
    }

    public void startPreparingPlacement() {
        startTimePreparingPlacement = System.currentTimeMillis();
        progressPreparingPlacement = 0.0;
        preparingPlacementDone = false;
        if (barPreparingPlacement == null) {
            barPreparingPlacement = createBossBar("Preparing Placement");
        }
    }

    public void startPlacingFastBlocks() {
        startTimePlacingFastBlocks = System.currentTimeMillis();
        progressPlacingFastBlocks = 0.0;
        placingFastBlocksDone = false;
        if (barPlacingFastBlocks == null) {
            barPlacingFastBlocks = createBossBar("Placing Fast Blocks");
        }
    }

    public void startPlacingSlowBlocks() {
        startTimePlacingSlowBlocks = System.currentTimeMillis();
        progressPlacingSlowBlocks = 0.0;
        placingSlowBlocksDone = false;
        if (barPlacingSlowBlocks == null) {
            barPlacingSlowBlocks = createBossBar("Placing Slow Blocks");
        }
    }

    public void startReadingWorldFeatures() {
        startTimeReadingWorldFeatures = System.currentTimeMillis();
        progressReadingWorldFeatures = 0.0;
        readingWorldFeaturesDone = false;
        if (barReadingWorldFeatures == null) {
            barReadingWorldFeatures = createBossBar("Reading World Features");
        }
    }

    public void startPlacingMobs() {
        startTimePlacingMobs = System.currentTimeMillis();
        progressPlacingMobs = 0.0;
        placingMobsDone = false;
        if (barPlacingMobs == null) {
            barPlacingMobs = createBossBar("Placing Mobs");
        }
    }

    // --- Update Progress Methods for Each Phase ---
    // These methods let you update the progress (a value between 0 and 1) as work is done.
    public void updateProgressInitializing(double progress) {
        progressInitializing = clampProgress(progress);
    }

    public void updateProgressReservingChunks(double progress) {
        progressReservingChunks = clampProgress(progress);
    }

    public void updateProgressArrangingModules(double progress) {
        progressArrangingModules = clampProgress(progress);
    }

    public void updateProgressPreparingPlacement(double progress) {
        progressPreparingPlacement = clampProgress(progress);
    }

    public void updateProgressPlacingFastBlocks(double progress) {
        progressPlacingFastBlocks = clampProgress(progress);
    }

    public void updateProgressPlacingSlowBlocks(double progress) {
        progressPlacingSlowBlocks = clampProgress(progress);
    }

    public void updateProgressReadingWorldFeatures(double progress) {
        progressReadingWorldFeatures = clampProgress(progress);
    }

    public void updateProgressPlacingMobs(double progress) {
        progressPlacingMobs = clampProgress(progress);
    }

    // Ensure progress is between 0 and 1.
    private double clampProgress(double progress) {
        if (progress < 0.0) return 0.0;
        return Math.min(progress, 1.0);
    }

    // --- Phase Finished Methods ---
    // When a phase is finished, toggle its boolean to true, set its progress to 1.0, and log the elapsed time.
    public void finishedInitializing() {
        initializingDone = true;
        progressInitializing = 1.0;
        logPhaseTime("Initializing", startTimeInitializing);
    }

    public void finishedReservingChunks(int chunkCount) {
        reservingChunksDone = true;
        progressReservingChunks = 1.0;
        logPhaseTime("Reserving Chunks", startTimeReservingChunks);
        Logger.sendMessage(generatingPlayer, "Reserved " + chunkCount + " chunks for the map!");
    }

    public void finishedArrangingModules() {
        arrangingModulesDone = true;
        progressArrangingModules = 1.0;
        logPhaseTime("Arranging Modules", startTimeArrangingModules);
    }

    public void finishedPreparingPlacement() {
        preparingPlacementDone = true;
        progressPreparingPlacement = 1.0;
        logPhaseTime("Preparing Placement", startTimePreparingPlacement);
    }

    public void finishedPlacingFastBlocks() {
        placingFastBlocksDone = true;
        progressPlacingFastBlocks = 1.0;
        logPhaseTime("Placing Fast Blocks", startTimePlacingFastBlocks);
    }

    public void finishedPlacingSlowBlocks() {
        placingSlowBlocksDone = true;
        progressPlacingSlowBlocks = 1.0;
        logPhaseTime("Placing Slow Blocks", startTimePlacingSlowBlocks);
    }

    public void finishedReadingWorldFeatures() {
        readingWorldFeaturesDone = true;
        progressReadingWorldFeatures = 1.0;
        logPhaseTime("Reading World Features", startTimeReadingWorldFeatures);
    }

    public void finishedPlacingMobs() {
        placingMobsDone = true;
        progressPlacingMobs = 1.0;
        logPhaseTime("Placing Mobs", startTimePlacingMobs);
    }

    // Call when all phases (or generation overall) are done.
    public void done() {
        bossBarTask.cancel();
        removeAllBossBars();
        ready = true;
    }

    // Log the elapsed time for a phase.
    private void logPhaseTime(String phaseName, long phaseStartTime) {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        long totalElapsed = System.currentTimeMillis() - overallStartTime;
        String message = String.format("[%s] %s completed! Phase time: %s",
                getTimeString(totalElapsed), phaseName, getTimeString(elapsed));
        if (generatingPlayer != null) {
            Logger.sendMessage(generatingPlayer, message);
        }
        Logger.info(message);
    }

    // Remove all boss bars from players.
    private void removeAllBossBars() {
        if (barInitializing != null) barInitializing.removeAll();
        if (barReservingChunks != null) barReservingChunks.removeAll();
        if (barArrangingModules != null) barArrangingModules.removeAll();
        if (barPreparingPlacement != null) barPreparingPlacement.removeAll();
        if (barPlacingFastBlocks != null) barPlacingFastBlocks.removeAll();
        if (barPlacingSlowBlocks != null) barPlacingSlowBlocks.removeAll();
        if (barReadingWorldFeatures != null) barReadingWorldFeatures.removeAll();
        if (barPlacingMobs != null) barPlacingMobs.removeAll();
    }

    // Helper: Formats milliseconds into MM:SS.sss
    private String getTimeString(long timeMillis) {
        long totalSeconds = timeMillis / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long milliseconds = timeMillis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }
}
