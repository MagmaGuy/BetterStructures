package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.WorldFolderResolver;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.joml.Vector3i;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.magmaguy.betterstructures.modules.ModulesContainer.pickWeightedRandomModule;

public class WFCGenerator {
    private static final Set<WFCGenerator> ACTIVE_GENERATORS = ConcurrentHashMap.newKeySet();
    @Getter
    private ModuleGeneratorsConfigFields moduleGeneratorsConfigFields;

    @Getter
    private WFCLattice spatialGrid;
    private Player player;
    private String startingModule;
    @Getter
    private World world;
    @Getter
    private Location startLocation;
    private volatile boolean isGenerating;
    private volatile boolean isCancelled;
    private File worldFolder;
    private String worldName;
    private int rollbackCounter = 0;
    private BossBar progressBar;
    private int totalNodes = 0;
    private volatile int completedNodes = 0;
    private volatile String pendingProgressMessage;
    private final AtomicBoolean progressUpdateScheduled = new AtomicBoolean();
    private final AtomicBoolean cleanedUp = new AtomicBoolean();

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Player player) {
        this.player = Objects.requireNonNull(player, "player");
        this.startLocation = player.getLocation();
        initialize(moduleGeneratorsConfigFields);
    }

    public WFCGenerator(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields, Location startLocation) {
        this.startLocation = Objects.requireNonNull(startLocation, "startLocation");
        initialize(moduleGeneratorsConfigFields);
    }

    public static void generateFromConfig(ModuleGeneratorsConfigFields generatorsConfigFields, Player player) {
        Objects.requireNonNull(generatorsConfigFields, "generatorsConfigFields");
        Objects.requireNonNull(player, "player");
        runOnPrimaryThread(() -> new WFCGenerator(generatorsConfigFields, player));
    }

    public static void shutdown() {
        List.copyOf(ACTIVE_GENERATORS).forEach(WFCGenerator::cancel);
        ACTIVE_GENERATORS.clear();
    }

    static String selectAvailableWorldName(String filename, Predicate<String> folderExists) {
        String baseWorldName = Objects.requireNonNull(filename, "filename")
                .replaceFirst("(?i)\\.yml$", "");
        Objects.requireNonNull(folderExists, "folderExists");
        for (int suffix = 0; suffix < Integer.MAX_VALUE; suffix++) {
            String candidate = baseWorldName + "_" + suffix;
            if (!folderExists.test(candidate)) return candidate;
        }
        throw new IllegalStateException("Could not find an available world name for " + filename);
    }

    static void forEachPasteCoordinate(int radius, int minY, int maxY, Consumer<Vector3i> consumer) {
        // Inclusive bounds on purpose: they sweep the full populated lattice ([-radius, +radius],
        // see WFCLattice.initializeLattice). The pre-extraction loop excluded only the +x/+z
        // planes; either way those planes hold boundary nodes carrying the 'nothing' container
        // (null clipboard) that ModulePasting filters out, so this is consistency, not a behavior
        // change.
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                for (int y = minY; y <= maxY; y++)
                    consumer.accept(new Vector3i(x, y, z));
    }

    private void initializeProgressBar() {
        if (player != null) {
            progressBar = Bukkit.createBossBar("Generating Structure...", BarColor.BLUE, BarStyle.SOLID);
            progressBar.addPlayer(player);
            progressBar.setProgress(0.0);
        }
    }

    private void updateProgressBar(String message) {
        pendingProgressMessage = message;
        if (progressBar == null || !progressUpdateScheduled.compareAndSet(false, true)) return;
        runOnPrimaryThread(() -> {
            String appliedMessage = pendingProgressMessage;
            if (progressBar != null && totalNodes > 0) {
                double progress = (double) completedNodes / totalNodes;
                progressBar.setProgress(Math.max(0.0, Math.min(progress, 1.0)));
                progressBar.setTitle(appliedMessage);
            }
            progressUpdateScheduled.set(false);
            if (!Objects.equals(appliedMessage, pendingProgressMessage))
                updateProgressBar(pendingProgressMessage);
        });
    }

    private void removeProgressBar() {
        if (progressBar != null) {
            progressBar.removeAll();
            progressBar = null;
        }
    }

    private void initialize(ModuleGeneratorsConfigFields moduleGeneratorsConfigFields) {
        this.moduleGeneratorsConfigFields = Objects.requireNonNull(moduleGeneratorsConfigFields, "moduleGeneratorsConfigFields");
        List<String> startModules = moduleGeneratorsConfigFields.getStartModules();
        if (startModules.isEmpty()) {
            if (player != null) player.sendMessage("No start modules exist, you need to install or make modules first!");
            Logger.warn("No start modules exist, you need to install or make modules first!");
            return;
        }
        startingModule = startModules.get(ThreadLocalRandom.current().nextInt(startModules.size())) + "_rotation_0";
        spatialGrid = new WFCLattice(moduleGeneratorsConfigFields.getRadius(),
                moduleGeneratorsConfigFields.getModuleSizeXZ(), moduleGeneratorsConfigFields.getModuleSizeY(),
                moduleGeneratorsConfigFields.getMinChunkY(), moduleGeneratorsConfigFields.getMaxChunkY());
        int radius = moduleGeneratorsConfigFields.getRadius();
        int minY = moduleGeneratorsConfigFields.getMinChunkY();
        int maxY = moduleGeneratorsConfigFields.getMaxChunkY();
        // Only interior nodes ever collapse: the +/-radius x/z planes (and the y planes outside
        // [minY, maxY]) are boundary nodes pre-collapsed to 'nothing' that never enter the entropy
        // queue, so counting them would keep the progress bar from ever reaching 100%.
        totalNodes = (radius * 2 - 1) * (radius * 2 - 1) * (maxY - minY + 1);
        ACTIVE_GENERATORS.add(this);

        try {
            initializeProgressBar();
            initializeWorldAndLattice();
        } catch (RuntimeException exception) {
            Logger.warn("Failed to initialize modular generation: " + exception.getMessage());
            exception.printStackTrace();
            cleanup();
        }
    }

    private void initializeWorldAndLattice() {
        updateProgressBar("Initializing lattice...");
        if (moduleGeneratorsConfigFields.isWorldGeneration()) {
            worldName = selectAvailableWorldName(moduleGeneratorsConfigFields.getFilename(), WorldFolderResolver::folderExists);
            world = WorldInitializer.generateWorld(worldName, player);
            startLocation = new Location(world, 0, 0, 0);
        } else {
            world = Objects.requireNonNull(startLocation.getWorld(), "startLocation world");
            worldName = world.getName();
        }
        worldFolder = WorldFolderResolver.resolve(worldName);
        spatialGrid.initializeLattice(world, this);
        startArrangingModules();
    }

    private void startArrangingModules() {
        updateProgressBar("Starting generation...");
        Bukkit.getScheduler().runTaskAsynchronously(MetadataHandler.PLUGIN, () -> start(startingModule));
    }

    private void start(String startingModule) {
        if (isCancelled || isGenerating) return;
        isGenerating = true;
        boolean completed = false;
        try {
            updateProgressBar("Collapsing initial node...");
            WFCNode startChunk = createStartChunk(startingModule);
            if (startChunk == null) {
                isCancelled = true;
                return;
            }
            completed = generateFast();
        } catch (RuntimeException exception) {
            Logger.warn("Error during generation: " + exception.getMessage());
            exception.printStackTrace();
            isCancelled = true;
        } finally {
            isGenerating = false;
            boolean generationCompleted = completed && !isCancelled;
            runOnPrimaryThread(generationCompleted ? this::finishGeneration : this::cleanup);
        }
    }

    private WFCNode createStartChunk(String startingModule) {
        WFCNode startCell = spatialGrid.getNodeMap().get(new Vector3i());
        if (startCell == null) {
            Logger.warn("The configured lattice does not contain the origin node! Cancelling!");
            return null;
        }

        ModulesContainer modulesContainer = ModulesContainer.getModulesContainers().get(startingModule);
        if (modulesContainer == null) {
            Logger.warn("Starting module was null! Cancelling!");
            return null;
        }

        paste(startCell, modulesContainer);
        completedNodes++;
        return startCell;
    }

    private boolean generateFast() {
        updateProgressBar("Propagating constraints...");
        while (!isCancelled) {
            WFCNode nextCell = spatialGrid.getLowestEntropyNode();
            if (nextCell == null) return true;
            generateNextChunk(nextCell);
        }
        return false;
    }

    private void paste(WFCNode gridCell, ModulesContainer modulesContainer) {
        // Record the decision for backtracking
        spatialGrid.recordCollapseDecision(gridCell, modulesContainer);

        gridCell.setModulesContainer(modulesContainer);
        gridCell.getOrientedNeighbors().values().forEach(spatialGrid::updateNodeEntropy);
    }

    private void generateNextChunk(WFCNode gridCell) {
        HashSet<ModulesContainer> validOptions = gridCell.getValidOptions();
        if (validOptions == null || validOptions.isEmpty()) {
            updateProgressBar("Backtracking...");
            rollbackChunk();
            return;
        }

        ModulesContainer modulesContainer = pickWeightedRandomModule(validOptions, gridCell);
        if (modulesContainer == null) {
            updateProgressBar("Backtracking...");
            rollbackChunk();
            return;
        }

        paste(gridCell, modulesContainer);
        completedNodes++;
        updateProgressBar("Generating... (" + completedNodes + "/" + totalNodes + ")");
    }

    private void rollbackChunk() {
        // Use proper backtracking instead of just resetting
        if (spatialGrid.backtrack()) {
            completedNodes = Math.max(0, completedNodes - 1);
            updateProgressBar("Backtracking... (" + spatialGrid.getBacktrackDepth() + " decisions remaining)");
        } else {
            updateProgressBar("Generation failed - no decisions to backtrack");
            isCancelled = true;
            return;
        }

        rollbackCounter++;
        if (rollbackCounter > 1000) {
            updateProgressBar("Generation failed - exceeded backtrack limit");
            Logger.warn("Exceeded backtrack limit!");
            isCancelled = true;
        }
    }

    private void finishGeneration() {
        if (isCancelled) {
            cleanup();
            return;
        }
        updateProgressBar("Generation complete!");
        if (player != null) {
            player.sendMessage("Done assembling!");
            player.sendMessage("It will take a moment to paste the structure, and will require relogging.");
        }
        instantPaste();
        cleanup();
    }

    private void cleanup() {
        if (!cleanedUp.compareAndSet(false, true)) return;
        if (spatialGrid != null) spatialGrid.clearAllData();
        ACTIVE_GENERATORS.remove(this);
        removeProgressBar();
    }

    /**
     * Cancels the generation process.
     */
    public void cancel() {
        isCancelled = true;
        runOnPrimaryThread(() -> {
            ACTIVE_GENERATORS.remove(this);
            removeProgressBar();
            if (!isGenerating) cleanup();
        });
    }

    private void instantPaste() {
        // This guarantees that the paste order is grouped by chunk, making pasting faster down the line.
        Deque<WFCNode> orderedPasteDeque = new ArrayDeque<>();
        forEachPasteCoordinate(spatialGrid.getLatticeRadius(), spatialGrid.getMinYLevel(), spatialGrid.getMaxYLevel(), coordinate -> {
            WFCNode cell = spatialGrid.getNodeMap().remove(coordinate);
            if (cell != null) orderedPasteDeque.add(cell);
        });

        new ModulePasting(world, worldFolder, orderedPasteDeque, moduleGeneratorsConfigFields.getSpawnPoolSuffix(), startLocation, moduleGeneratorsConfigFields);
    }

    private static void runOnPrimaryThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) runnable.run();
        else if (MetadataHandler.PLUGIN != null && MetadataHandler.PLUGIN.isEnabled())
            Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, runnable);
    }
}
