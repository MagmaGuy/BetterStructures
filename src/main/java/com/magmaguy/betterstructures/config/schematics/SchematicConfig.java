package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.SchematicFileUtils;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.betterstructures.worldedit.SchematicConversionLog;
import com.magmaguy.magmacore.config.CustomConfig;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        //Sorted so config generation order, and therefore console output, is stable between boots.
        schematicFilesList.sort(Comparator.comparing(File::getAbsolutePath));

        Map<File, Clipboard> clipboards = loadClipboards(schematicFilesList);
        phaseStart = logPhase("read " + clipboards.size() + " schematic files", phaseStart);

        //Filename -> source file, so the container loop below can look its clipboard up directly.
        //It used to scan the whole clipboard map per configuration, which on a full content install
        //is ~682 configurations x ~682 clipboards of string comparisons for no reason.
        Map<String, File> sourceByFilename = new HashMap<>();
        for (Map.Entry<File, Clipboard> entry : clipboards.entrySet()) {
            File previous = sourceByFilename.putIfAbsent(
                    entry.getKey().getName(),
                    entry.getKey());
            if (previous != null && !previous.equals(entry.getKey())) {
                throw new IllegalStateException(
                        "Duplicate schematic filename '" + entry.getKey().getName()
                                + "' exists at both " + previous.getPath() + " and "
                                + entry.getKey().getPath()
                                + "; configuration lookup by filename would be ambiguous.");
            }
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

    private static final String SCHEMATIC_CACHE_FOLDER = ".schematic_cache";

    /**
     * Loads a schematic, going through an on-disk cache of already-converted copies.
     * <p>
     * Shipped .schem files carry the Minecraft DataVersion they were saved at, which is older than
     * the running server. WorldEdit therefore hands every block entity in every file to Mojang's
     * DataFixerUpper on the way in. That was ~8s of a full-content start, and it does not
     * parallelise: thread dumps show workers parked on CompletableFutures inside DFU while it
     * builds its shared rewrite rules, so the cost is largely fixed per JVM no matter how many
     * threads read files. It also grows with every Minecraft release, as the conversion chain from
     * the shipped version to current gets longer.
     * <p>
     * So the converted result is written back out at the current DataVersion and reused. Entries are
     * keyed by the source file's contents, the Minecraft data version and the WorldEdit version, so
     * editing a schematic, updating the server or updating WorldEdit all miss and reconvert. Each
     * entry is also proven equal to its source before it is published (see writeCache). A miss, an
     * unreadable entry, or any failure to write one falls back to reading the original — the cache
     * is only ever an optimisation, never the source of truth.
     */
    private static Clipboard loadCached(File schematicFile, File cacheFolder, Map<String, File> cacheIndex, Map<File, String> computedKeys) {
        String key = cacheKey(schematicFile);
        if (key == null) return Schematic.load(schematicFile);
        computedKeys.put(schematicFile, key);

        File cacheFile = cacheIndex.get(key);
        if (cacheFile != null) {
            Clipboard cached = readVerifiedCacheEntry(cacheFile);
            if (cached != null) return cached;
            //Unreadable, or its contents no longer match the name it is filed under. Either way it
            //cannot be trusted, so drop it and convert from the original.
            cacheFile.delete();
        }

        Clipboard clipboard = Schematic.load(schematicFile);
        if (clipboard != null && cacheFolder.isDirectory()) writeCache(clipboard, cacheFolder, key, schematicFile);
        return clipboard;
    }

    /**
     * Reads a cache entry, but only after confirming its contents still hash to the value recorded
     * in its filename.
     * <p>
     * Entries are verified when written, so the only way one can be wrong afterwards is if the file
     * changed on disk. Damage usually announces itself — the format is gzipped, so a corrupted
     * stream fails its own checksum and simply will not parse. What that does not cover is an entry
     * being replaced by a different, perfectly valid schematic, which would otherwise load happily
     * and generate the wrong structure with nothing logged. Comparing the contents against the hash
     * in the name closes that off, and costs one hash of a file that is about to be read anyway.
     *
     * @return the clipboard, or null if the entry is missing, damaged, or not what it claims to be
     */
    private static Clipboard readVerifiedCacheEntry(File cacheFile) {
        try {
            byte[] contents = Files.readAllBytes(cacheFile.toPath());
            String name = cacheFile.getName();
            int separator = name.lastIndexOf('-');
            if (separator < 0) return null;
            String recordedHash = name.substring(separator + 1, name.length() - ".schem".length());
            if (!recordedHash.equals(hashOf(contents))) return null;

            try (ClipboardReader reader =
                         BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC
                                 .getReader(new ByteArrayInputStream(contents))) {
                return reader.read();
            }
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Indexes the cache folder once per load, mapping each entry's key to its file.
     * <p>
     * Filenames carry the key and a hash of the contents, so the file for a given key cannot be
     * named without knowing what is in it. Listing the folder a single time avoids re-listing it
     * once per schematic.
     */
    private static Map<String, File> indexCacheFolder(File cacheFolder) {
        Map<String, File> index = new HashMap<>();
        File[] cachedFiles = cacheFolder.listFiles();
        if (cachedFiles == null) return index;
        for (File cachedFile : cachedFiles) {
            String name = cachedFile.getName();
            if (!name.endsWith(".schem")) continue;
            int separator = name.lastIndexOf('-');
            if (separator < 0) continue;
            index.put(name.substring(0, separator), cachedFile);
        }
        return index;
    }

    private static String hashOf(byte[] contents) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(contents);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    /**
     * Identifies a cache entry by everything that can change what a converted schematic should look
     * like: the source file's contents, the Minecraft version being converted to, and the WorldEdit
     * version doing the converting. Change any of the three and the entry is a miss and gets rebuilt.
     *
     * @return the key, or null if the file could not be hashed
     */
    private static String cacheKey(File schematicFile) {
        try {
            return hashOf(Files.readAllBytes(schematicFile.toPath())) + "-" + Bukkit.getUnsafe().getDataVersion() + "-" + worldEditVersion();
        } catch (Exception exception) {
            return null;
        }
    }

    private static String worldEditVersion() {
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) return "none";
        //Only used as a cache key component, so any characters that would be awkward in a filename
        //are flattened rather than escaped.
        return worldEdit.getDescription().getVersion().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Only reports whether the two clipboards differ, never how much, so it can stop at the first
     * discrepancy.
     *
     * @return a description of the first difference, or null when the two are identical
     */
    private static String firstDifference(Clipboard original, Clipboard candidate) {
        BlockVector3 originalDimensions = original.getDimensions();
        if (!originalDimensions.equals(candidate.getDimensions()))
            return "size changed, was " + originalDimensions + " and the copy has " + candidate.getDimensions();

        BlockVector3 originalMinimum = original.getMinimumPoint();
        BlockVector3 candidateMinimum = candidate.getMinimumPoint();
        BlockVector3 originalPasteOffset = originalMinimum.subtract(original.getOrigin());
        BlockVector3 candidatePasteOffset = candidateMinimum.subtract(candidate.getOrigin());
        if (!originalPasteOffset.equals(candidatePasteOffset)) {
            return "paste origin changed, minimum-to-origin offset was "
                    + originalPasteOffset + " and the copy has "
                    + candidatePasteOffset;
        }

        for (int x = 0; x < originalDimensions.x(); x++)
            for (int y = 0; y < originalDimensions.y(); y++)
                for (int z = 0; z < originalDimensions.z(); z++) {
                    BlockVector3 offset = BlockVector3.at(x, y, z);
                    //getFullBlock rather than getBlock: it carries the block entity data, which is
                    //what chests, spawner signs and boss signs are made of, and therefore the part
                    //whose loss would otherwise be invisible.
                    BaseBlock originalBlock = original.getFullBlock(offset.add(originalMinimum));
                    BaseBlock candidateBlock = candidate.getFullBlock(offset.add(candidateMinimum));
                    if (!originalBlock.equals(candidateBlock))
                        return "block at " + x + "," + y + "," + z + " changed, was "
                                + originalBlock + " and the copy has " + candidateBlock;
                }

        List<String> originalEntities = entitySignatures(original);
        List<String> candidateEntities = entitySignatures(candidate);
        if (!originalEntities.equals(candidateEntities)) {
            return "entities changed, source has " + originalEntities.size()
                    + " semantic entries and the copy has "
                    + candidateEntities.size();
        }
        return null;
    }

    /**
     * Captures the entity state that affects a paste while ignoring list order
     * and the clipboard's absolute coordinate frame. Sponge round trips may
     * normalize absolute coordinates, but entity position relative to the
     * paste origin, rotation, type, and NBT must remain identical.
     */
    private static List<String> entitySignatures(Clipboard clipboard) {
        List<String> signatures = new ArrayList<>();
        for (com.sk89q.worldedit.entity.Entity entity : clipboard.getEntities()) {
            com.sk89q.worldedit.util.Location location = entity.getLocation();
            com.sk89q.worldedit.entity.BaseEntity state = entity.getState();
            String type = state == null || state.getType() == null
                    ? "unknown"
                    : state.getType().id();
            String nbt = state == null || !state.hasNbtData()
                    ? ""
                    : String.valueOf(state.getNbtData().toLinTag());
            signatures.add(
                    type + "|"
                            + Double.doubleToLongBits(
                                    location.getX() - clipboard.getOrigin().x()) + "|"
                            + Double.doubleToLongBits(
                                    location.getY() - clipboard.getOrigin().y()) + "|"
                            + Double.doubleToLongBits(
                                    location.getZ() - clipboard.getOrigin().z()) + "|"
                            + Float.floatToIntBits(location.getYaw()) + "|"
                            + Float.floatToIntBits(location.getPitch()) + "|"
                            + nbt);
        }
        signatures.sort(String::compareTo);
        return signatures;
    }

    private static final AtomicBoolean warnedAboutUnusableCache = new AtomicBoolean(false);

    /**
     * Writes a converted schematic to the cache, but only after proving the written copy reads back
     * identical to what was just loaded.
     * <p>
     * The point of the cache is to skip re-running Minecraft's data conversion on every start, which
     * means every later start trusts these files instead of the originals. If a copy were ever
     * subtly wrong — a lost sign, a dropped chest — structures would keep generating and nothing
     * would report an error. Rather than leave that to a check somebody has to remember to run, an
     * entry only comes into existence if it has already been shown to match its source, so a broken
     * round trip degrades to "slow but correct" on its own.
     * <p>
     * The copy is written to a temporary file first, so an interrupted start cannot leave a
     * truncated file that a later start would mistake for a verified entry.
     */
    private static void writeCache(Clipboard clipboard, File cacheFolder, String key, File sourceFile) {
        //Unique per attempt, not per key: duplicate schematics shipped in different folders hash to
        //the same key, so keying the temporary file on that alone had two threads writing and moving
        //one shared path.
        File temporaryFile = new File(cacheFolder, key + "-" + UUID.randomUUID() + ".tmp");
        try {
            //Serialised into memory, and verified from memory, so the candidate copy is never opened
            //as a file before it is accepted. Reading it back from disk instead left the reader's
            //handle open on Windows, which then blocked the move into place, so entries silently
            //failed to publish and left .tmp files behind.
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ClipboardWriter writer =
                         BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(buffer)) {
                writer.write(clipboard);
            }
            byte[] written = buffer.toByteArray();

            Clipboard writtenBack;
            try (ClipboardReader reader =
                         BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC
                                 .getReader(new ByteArrayInputStream(written))) {
                writtenBack = reader.read();
            }

            String difference = writtenBack == null
                    ? "it could not be read back"
                    : firstDifference(clipboard, writtenBack);
            if (difference != null) {
                reportUnusableCache(sourceFile, difference);
                return;
            }

            //The contents hash goes in the filename so a later start can confirm the entry still
            //holds what it held when it was verified.
            File cacheFile = new File(cacheFolder, key + "-" + hashOf(written) + ".schem");

            //Written to a temporary name and moved, so an interrupted start cannot leave a
            //half-written file that a later start would mistake for a verified entry.
            Files.write(temporaryFile.toPath(), written);
            try {
                Files.move(temporaryFile.toPath(), cacheFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                //Content packs sometimes ship the same schematic in more than one folder. Identical
                //contents produce an identical key, so two loader threads can be publishing the very
                //same entry at the same time and one of them loses the race on the destination. The
                //entry it wanted is already there and byte-identical, so that is a success, not a
                //failure worth warning about.
                Files.deleteIfExists(temporaryFile.toPath());
                if (!cacheFile.isFile()) throw moveFailure;
            }
        } catch (Exception exception) {
            temporaryFile.delete();
            //Structures are unaffected — the schematic is already loaded from its original file.
            //Still reported, because a cache that silently never populates would look like an
            //unexplained slow start forever.
            reportUnusableCache(sourceFile, exception.toString());
        }
    }

    private static void reportUnusableCache(File sourceFile, String reason) {
        if (!warnedAboutUnusableCache.compareAndSet(false, true)) return;
        Logger.warn("Schematic caching is not working: a cached copy of " + sourceFile.getName()
                + " was rejected (" + reason + ").");
        Logger.warn("Structures are unaffected — they are being read from the original files instead, "
                + "which only makes startup slower. Please report this along with your WorldEdit version.");
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

            File cacheFolder =
                    new File(MetadataHandler.PLUGIN.getDataFolder(), SCHEMATIC_CACHE_FOLDER);
            if (!cacheFolder.isDirectory() && !cacheFolder.mkdirs())
                Logger.warn("Could not create the schematic cache folder; schematics will be re-converted every start.");

            Map<String, File> cacheIndex = indexCacheFolder(cacheFolder);
            Map<File, String> computedKeys = new ConcurrentHashMap<>();
            AtomicInteger filesRead = new AtomicInteger();

            try (SchematicConversionLog.Session conversionLog =
                         SchematicConversionLog.capture()) {
                File warmupFile = schematicFiles.get(0);
                Clipboard warmupClipboard;
                try {
                    warmupClipboard = activeLoad.submit(
                            () -> loadClipboard(
                                    warmupFile,
                                    cacheFolder,
                                    cacheIndex,
                                    computedKeys,
                                    filesRead,
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
                                cacheFolder,
                                cacheIndex,
                                computedKeys,
                                filesRead,
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
            if (filesRead.get() > 0 || forgotten > 0) {
                pruneStaleCacheEntries(schematicFiles, cacheFolder, computedKeys, activeLoad);
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
     * data converter — only runs for files the in-memory cache cannot vouch for. Files that were
     * genuinely read are counted, because that is what decides whether the on-disk cache needs
     * pruning afterwards.
     */
    private static Clipboard loadClipboard(File schematicFile,
                                           File cacheFolder,
                                           Map<String, File> cacheIndex,
                                           Map<File, String> computedKeys,
                                           AtomicInteger filesRead,
                                           SchematicLoadCoordinator.ActiveLoad activeLoad) {
        activeLoad.checkRunning();
        Clipboard cached = clipboardCache.get(schematicFile);
        activeLoad.checkRunning();
        if (cached != null) {
            return cached;
        }

        Clipboard clipboard = loadCached(schematicFile, cacheFolder, cacheIndex, computedKeys);
        activeLoad.checkRunning();
        if (clipboard != null) {
            clipboardCache.put(schematicFile, clipboard);
            filesRead.incrementAndGet();
            activeLoad.checkRunning();
        }
        return clipboard;
    }

    /**
     * Deletes cache entries that no schematic currently maps to.
     * <p>
     * Entries are keyed by schematic contents, Minecraft data version and WorldEdit version, so
     * updating any of the three leaves the previous generation of entries behind — as does deleting
     * a content pack. Without this, the cache folder keeps a full copy of every schematic set the
     * server has ever run.
     */
    private static void pruneStaleCacheEntries(
            List<File> schematicFiles,
            File cacheFolder,
            Map<File, String> computedKeys,
            SchematicLoadCoordinator.ActiveLoad activeLoad) {
        File[] cachedFiles = cacheFolder.listFiles();
        if (cachedFiles == null) return;

        Set<String> currentKeys = new HashSet<>();
        for (File schematicFile : schematicFiles) {
            activeLoad.checkRunning();
            String key = computedKeys.get(schematicFile);
            if (key == null) key = cacheKey(schematicFile);
            if (key != null) currentKeys.add(key);
        }
        //An empty set means every key failed to compute; deleting the whole cache off the back of
        //that would be destructive for no reason.
        if (currentKeys.isEmpty()) return;

        int pruned = 0;
        for (File cachedFile : cachedFiles) {
            activeLoad.checkRunning();
            String name = cachedFile.getName();
            //Matched on key rather than whole filename: the trailing contents hash is not knowable
            //from the source alone, so only the key part can be compared.
            if (name.endsWith(".schem")) {
                int separator = name.lastIndexOf('-');
                if (separator >= 0 && currentKeys.contains(name.substring(0, separator))) continue;
            } else if (!name.endsWith(".tmp")) {
                continue;
            }
            if (cachedFile.delete()) pruned++;
        }
        if (pruned > 0) Logger.info("Removed " + pruned + " outdated schematic cache entries.");
    }

    public static SchematicConfigField getSchematicConfiguration(String filename) {
        return schematicConfigurations.get(filename);
    }
}
