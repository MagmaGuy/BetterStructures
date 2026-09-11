package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.SchematicFileUtils;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.betterstructures.worldedit.SchematicConversionLog;
import com.magmaguy.betterstructures.worldedit.SchematicDiskCache;
import com.magmaguy.magmacore.config.CustomConfig;
import com.magmaguy.magmacore.config.ContentFileSelector;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SchematicConfig extends CustomConfig {
    private static final Duration LOAD_SHUTDOWN_TIMEOUT = Duration.ofSeconds(15);
    private static final SchematicLoadCoordinator loadCoordinator =
            new SchematicLoadCoordinator();
    @Getter
    private static final HashMap<String, SchematicConfigField> schematicConfigurations = new HashMap<>();
    //Survives content reloads on purpose: reloading exists to re-read configuration, and the
    //schematic files themselves are usually untouched between one reload and the next.
    private static final SchematicClipboardCache clipboardCache = new SchematicClipboardCache();

    public static void prepareForEnable() {
        loadCoordinator.open();
    }

    /**
     * Prevents new schematic loads, cancels the active generation, and waits for its worker pool and
     * owning initialization thread to leave the load method.
     *
     * @return true when no schematic-loading code remains active
     */
    public static boolean shutdownLoading() {
        return loadCoordinator.shutdownAndAwait(LOAD_SHUTDOWN_TIMEOUT);
    }

    public SchematicConfig() {
        super("schematics", SchematicConfigField.class);
        schematicConfigurations.clear();

        File readMeFile = new File(MetadataHandler.PLUGIN.getDataFolder(), "schematics" + File.separatorChar + "ReadMe.txt");
        if (!readMeFile.exists()) {
            readMeFile.getParentFile().mkdirs();
            MetadataHandler.PLUGIN.saveResource("schematics" + File.separatorChar + "ReadMe.txt", false);
        }

        //Initialize schematics
        long phaseStart = System.nanoTime();
        List<File> schematicFilesList = new ArrayList<>();
        File[] schematicFiles = readMeFile.getParentFile().listFiles();
        if (schematicFiles != null)
            for (File file : schematicFiles) SchematicFileUtils.scanDirectoryForSchematics(file, schematicFilesList);
        // Resolve filename collisions before reading clipboards or generating their configurations.
        schematicFilesList = ContentFileSelector.select(schematicFilesList);

        Map<File, Clipboard> clipboards = loadClipboards(schematicFilesList);
        phaseStart = logPhase("read " + clipboards.size() + " schematic files", phaseStart);

        //Filename -> source file, so the container loop below can look its clipboard up directly.
        //It used to scan the whole clipboard map per configuration, which on a full content install
        //is ~682 configurations x ~682 clipboards of string comparisons for no reason.
        Map<String, File> sourceByFilename = new HashMap<>();
        for (Map.Entry<File, Clipboard> entry : clipboards.entrySet()) {
            sourceByFilename.put(
                    entry.getKey().getName(),
                    entry.getKey());
        }

        for (File file : clipboards.keySet()) {
            String configurationName = SchematicFileUtils.convertFromSchematicFilename(file.getName());
            SchematicConfigField schematicConfigField = new SchematicConfigField(configurationName, true);
            new CustomConfig(relativizeToDataFolder(file.getParentFile()),
                    SchematicConfigField.class, schematicConfigField);
            schematicConfigurations.put(configurationName, schematicConfigField);
        }
        phaseStart = logPhase("load " + schematicConfigurations.size() + " schematic configurations", phaseStart);

        for (SchematicConfigField schematicConfigField : schematicConfigurations.values()) {
            if (!schematicConfigField.isEnabled()) continue;
            String schematicFilename = SchematicFileUtils.convertFromConfigurationFilename(schematicConfigField.getFilename());
            File source = sourceByFilename.get(schematicFilename);
            Clipboard clipboard = source == null ? null : clipboards.get(source);
            new SchematicContainer(
                    clipboard,
                    schematicFilename,
                    schematicConfigField,
                    schematicConfigField.getFilename());
        }
        logPhase("scan schematic contents", phaseStart);
    }

    /**
     * Reports how long a phase of schematic loading took, so the three very different costs hiding
     * inside the single "Schematics" initialization step (reading .schem files, generating/reading
     * their configurations, and scanning their block contents) can be told apart.
     * <p>
     * Enabled by the same -Dmagmacore.inittiming=true used for the per-step initialization profile.
     *
     * @return the timestamp to measure the next phase from
     */
    private static long logPhase(String description, long phaseStartNanos) {
        long now = System.nanoTime();
        if (Boolean.getBoolean("magmacore.inittiming"))
            Logger.info(String.format("  [schematics] %6d ms  %s", (now - phaseStartNanos) / 1_000_000, description));
        return now;
    }

    /**
     * Returns a folder path relative to the plugin's data folder, which is what CustomConfig and
     * ConfigurationEngine.fileCreator expect — they resolve it against getDataFolder().getPath().
     * <p>
     * This used to be done by string-replacing the data folder's ABSOLUTE path out of
     * file.getParent(). Bukkit hands out a RELATIVE data folder ("plugins/BetterStructures"), so
     * the schematic files walked from it also carry relative parents and the replace never matched.
     * The full relative parent was then passed through as if it were a subfolder name, and configs
     * were written to plugins/BetterStructures/plugins/BetterStructures/schematics/... — a path
     * nothing ever reads back.
     * <p>
     * The consequence was not just wasted writes: every shipped DLC schematic config (which does
     * set generatorConfigFilename) was missed, a blank one was generated in the stray tree instead,
     * and so every one of those structures ended up with no generator and never spawned.
     * <p>
     * Both sides are made absolute and normalised here so the relativize works no matter which form
     * the server hands us.
     */
    private static String relativizeToDataFolder(File folder) {
        Path dataFolder = MetadataHandler.PLUGIN.getDataFolder().getAbsoluteFile().toPath().normalize();
        Path target = folder.getAbsoluteFile().toPath().normalize();
        if (target.equals(dataFolder)) return "";
        if (target.startsWith(dataFolder)) return dataFolder.relativize(target).toString();
        //Outside the data folder entirely (symlinked content pack, unusual setup): fall back to the
        //schematics root rather than writing somewhere unpredictable.
        Logger.warn("Schematic folder " + folder.getPath() + " is outside the BetterStructures data folder; " +
                "its configuration will be stored under schematics instead.");
        return "schematics";
    }

    /**
     * Reads every schematic into a clipboard, in parallel.
     * <p>
     * Reading a .schem is not cheap: the shipped files carry an older Minecraft DataVersion, so
     * WorldEdit runs Mojang's DataFixerUpper over every block entity in the file to migrate it to
     * the running server's format. On a full content install that was the single largest block of
     * startup work, and it was being done one file at a time on a single thread. The files are
     * completely independent of one another, so this just spreads them over a small pool.
     * <p>
     * The first file is deliberately submitted and completed on its own before the remaining work
     * is queued. WorldEdit's clipboard readers and block registries lazily initialise shared static
     * state on first use, and racing several threads into that on a cold JVM risks a half-built
     * registry. Once one read has completed, the remaining reads only touch per-file state. A
     * warmup file served from
     * {@link SchematicClipboardCache} does not warm anything, but it cannot be in that cache unless
     * an earlier load already parsed it, so the registries are established either way.
     * <p>
     * Results go into a LinkedHashMap in the (sorted) input order so downstream config generation
     * and log output stay identical between boots.
     */
    private static Map<File, Clipboard> loadClipboards(List<File> schematicFiles) {
        Map<File, Clipboard> clipboards = new LinkedHashMap<>();
        if (schematicFiles.isEmpty()) return clipboards;

        //Half the machine, capped. Thread dumps of this pool show the workers overwhelmingly
        //RUNNABLE rather than blocked, so this scales with cores rather than bottlenecking on a
        //shared lock. Initialization runs alongside the other plugins and the main thread, so it
        //deliberately does not take the whole box.
        int threads = Math.max(
                2,
                Math.min(Runtime.getRuntime().availableProcessors() / 2, 16));
        SchematicLoadCoordinator.ActiveLoad activeLoad =
                loadCoordinator.begin(threads);
        boolean completed = false;
        try {
            activeLoad.checkRunning();
            //Done before anything reads the cache, so a schematic that has been deleted since the
            //last load can never be served out of it.
            int forgotten = clipboardCache.retainOnly(schematicFiles);
            activeLoad.checkRunning();

            SchematicDiskCache diskCache =
                    new SchematicDiskCache(SchematicDiskCache.structureCacheFolder());

            try (SchematicConversionLog.Session conversionLog =
                         SchematicConversionLog.capture()) {
                File warmupFile = schematicFiles.get(0);
                Clipboard warmupClipboard;
                try {
                    warmupClipboard = activeLoad.submit(
                            () -> loadClipboard(
                                    warmupFile,
                                    diskCache,
                                    activeLoad)).get();
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof CancellationException cancellation) {
                        throw cancellation;
                    }
                    throw new IllegalStateException(
                            "Failed to load schematic " + warmupFile.getPath()
                                    + "; refusing to initialize a partial structure registry.",
                            exception.getCause());
                }
                activeLoad.checkRunning();
                if (warmupClipboard == null) {
                    throw new IllegalStateException(
                            "Failed to load schematic " + warmupFile.getPath()
                                    + "; refusing to initialize a partial structure registry.");
                }
                clipboards.put(warmupFile, warmupClipboard);

                Map<File, Clipboard> loaded = new ConcurrentHashMap<>();
                List<String> failures = java.util.Collections.synchronizedList(
                        new ArrayList<>());
                List<File> remaining = schematicFiles.subList(
                        1,
                        schematicFiles.size());
                List<Future<Void>> futures = new ArrayList<>(remaining.size());
                for (File file : remaining) {
                    futures.add(activeLoad.submit(() -> {
                        Clipboard clipboard = loadClipboard(
                                file,
                                diskCache,
                                activeLoad);
                        activeLoad.checkRunning();
                        if (clipboard != null) {
                            loaded.put(file, clipboard);
                        } else {
                            failures.add(file.getPath());
                        }
                        activeLoad.checkRunning();
                        return null;
                    }));
                }
                for (Future<Void> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException exception) {
                        if (exception.getCause() instanceof CancellationException cancellation) {
                            throw cancellation;
                        }
                        failures.add(String.valueOf(exception.getCause()));
                    }
                }
                if (!failures.isEmpty() || loaded.size() != remaining.size()) {
                    String sample = failures.isEmpty()
                            ? "unknown loader failure"
                            : failures.get(0);
                    throw new IllegalStateException(
                            "Failed to load " + Math.max(
                                    failures.size(),
                                    remaining.size() - loaded.size())
                                    + " of " + schematicFiles.size()
                                    + " schematics; refusing to initialize a partial "
                                    + "structure registry. First failure: " + sample);
                }
                //Re-insert in sorted order rather than ConcurrentHashMap order.
                for (File file : remaining) {
                    Clipboard clipboard = loaded.get(file);
                    if (clipboard != null) clipboards.put(file, clipboard);
                }
            }

            activeLoad.checkRunning();
            //The on-disk cache can only have gained entries for files that were actually read, and
            //can only have lost relevance for files that have disappeared. A reload where neither
            //happened has nothing to prune, so it does not re-hash the whole schematic set.
            if (diskCache.filesRead() > 0 || forgotten > 0) {
                diskCache.pruneStaleEntries(schematicFiles, activeLoad::checkRunning);
            }
            activeLoad.checkRunning();
            activeLoad.finish(LOAD_SHUTDOWN_TIMEOUT);
            completed = true;
            return clipboards;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Schematic loading was interrupted; refusing to initialize "
                            + "a partial structure registry.",
                    exception);
        } catch (CancellationException exception) {
            throw new IllegalStateException(
                    "Schematic loading was canceled because BetterStructures is shutting down.",
                    exception);
        } finally {
            if (!completed) {
                activeLoad.abort(LOAD_SHUTDOWN_TIMEOUT);
            }
        }
    }

    /**
     * Reads one schematic, preferring the clipboard the previous load already produced for it.
     * <p>
     * Everything below this — the on-disk converted copy, and failing that WorldEdit and Mojang's
     * data converter — only runs for files the in-memory cache cannot vouch for. The disk cache
     * counts the files it genuinely read, because that is what decides whether it needs pruning
     * afterwards.
     */
    private static Clipboard loadClipboard(File schematicFile,
                                           SchematicDiskCache diskCache,
                                           SchematicLoadCoordinator.ActiveLoad activeLoad) {
        activeLoad.checkRunning();
        Clipboard cached = clipboardCache.get(schematicFile);
        activeLoad.checkRunning();
        if (cached != null) {
            return cached;
        }

        Clipboard clipboard = diskCache.load(schematicFile);
        activeLoad.checkRunning();
        if (clipboard != null) {
            clipboardCache.put(schematicFile, clipboard);
            activeLoad.checkRunning();
        }
        return clipboard;
    }

    public static SchematicConfigField getSchematicConfiguration(String filename) {
        return schematicConfigurations.get(filename);
    }
}
