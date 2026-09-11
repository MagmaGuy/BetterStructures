package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An on-disk cache of schematics already converted to the running server's data version. One
 * instance covers one load pass over one loader's schematic set.
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
public final class SchematicDiskCache {
    private static final String CACHE_FOLDER_NAME = ".schematic_cache";

    private final File cacheFolder;
    private final Map<String, File> cacheIndex;
    private final Map<File, String> computedKeys = new ConcurrentHashMap<>();
    private final AtomicInteger filesRead = new AtomicInteger();

    /**
     * The cache folder for the structure schematic loader.
     * <p>
     * Each loader passes its own folder and owns it exclusively: {@link #pruneStaleEntries}
     * deletes every entry its loader's file set does not account for, so two loaders sharing a
     * folder would prune each other's entries on every load. Subfolders are safe — indexing and
     * pruning ignore directory entries. Structures keep the root folder name that shipped before
     * the cache machinery was shared, so entries already on servers survive the upgrade.
     */
    public static File structureCacheFolder() {
        return new File(MetadataHandler.PLUGIN.getDataFolder(), CACHE_FOLDER_NAME);
    }

    public static File moduleCacheFolder() {
        return new File(structureCacheFolder(), "modules");
    }

    public static File componentCacheFolder() {
        return new File(structureCacheFolder(), "components");
    }

    public SchematicDiskCache(File cacheFolder) {
        this.cacheFolder = cacheFolder;
        if (!cacheFolder.isDirectory() && !cacheFolder.mkdirs())
            Logger.warn("Could not create the schematic cache folder; schematics will be re-converted every start.");
        this.cacheIndex = indexCacheFolder(cacheFolder);
    }

    /**
     * Loads a schematic, going through the on-disk cache of already-converted copies.
     */
    public Clipboard load(File schematicFile) {
        Clipboard clipboard = loadThroughCache(schematicFile);
        if (clipboard != null) filesRead.incrementAndGet();
        return clipboard;
    }

    /**
     * How many files this instance actually read, whether from a cache entry or the original —
     * that is, files the caller's in-memory clipboard cache could not vouch for. The on-disk
     * cache can only have gained entries for files that were actually read, so a pass where
     * nothing was read has nothing to prune.
     */
    public int filesRead() {
        return filesRead.get();
    }

    private Clipboard loadThroughCache(File schematicFile) {
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
        if (clipboard != null && cacheFolder.isDirectory()) writeCache(clipboard, key, schematicFile);
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
                    if (!blocksSemanticallyEqual(originalBlock, candidateBlock))
                        return "block at " + x + "," + y + "," + z + " changed, was "
                                + originalBlock + " and the copy has " + candidateBlock;
                }

        List<String> originalEntities = entitySignatures(original);
        List<String> candidateEntities = entitySignatures(candidate);
        if (!originalEntities.equals(candidateEntities)) {
            //Name the first differing entry rather than the counts: the historical false
            //positive rejected with equal counts on both sides, which was unactionable.
            int max = Math.max(originalEntities.size(), candidateEntities.size());
            for (int index = 0; index < max; index++) {
                String originalEntry = index < originalEntities.size() ? originalEntities.get(index) : "<absent>";
                String candidateEntry = index < candidateEntities.size() ? candidateEntities.get(index) : "<absent>";
                if (!originalEntry.equals(candidateEntry))
                    return "entity entry " + index + " changed, was " + truncateSignature(originalEntry)
                            + " and the copy has " + truncateSignature(candidateEntry);
            }
            return "entities changed, source has " + originalEntities.size()
                    + " semantic entries and the copy has "
                    + candidateEntities.size();
        }
        return null;
    }

    private static String truncateSignature(String signature) {
        return signature.length() <= 200 ? signature : signature.substring(0, 200) + "…";
    }

    /**
     * Block equality that tolerates the block entity position keys. WorldEdit tracks a block
     * entity's position through the block it sits in, but FastAsyncWorldEdit's Sponge-v3 writer
     * additionally embeds x/y/z inside the block entity data, while data read from an
     * upstream-written file carries none — so a byte-faithful FAWE round trip still differs by
     * exactly those keys. They are rewritten to the paste position by any paster and therefore
     * carry no information a paste could lose.
     */
    private static boolean blocksSemanticallyEqual(BaseBlock original, BaseBlock candidate) {
        if (original.equals(candidate)) return true;
        if (!original.toImmutableState().equalsFuzzy(candidate.toImmutableState())) return false;
        org.enginehub.linbus.tree.LinCompoundTag originalNbt = original.getNbt();
        org.enginehub.linbus.tree.LinCompoundTag candidateNbt = candidate.getNbt();
        if (originalNbt == null || candidateNbt == null) return originalNbt == candidateNbt;
        return canonicalNbtWithoutPositionKeys(originalNbt).equals(canonicalNbtWithoutPositionKeys(candidateNbt));
    }

    private static String canonicalNbtWithoutPositionKeys(org.enginehub.linbus.tree.LinCompoundTag tag) {
        StringBuilder builder = new StringBuilder("{");
        tag.value().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey();
                    if ((key.equals("x") || key.equals("y") || key.equals("z"))
                            && entry.getValue() instanceof org.enginehub.linbus.tree.LinIntTag) return;
                    builder.append(key).append(':')
                            .append(canonicalNbt(entry.getValue())).append(',');
                });
        return builder.append('}').toString();
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
            org.enginehub.linbus.tree.LinCompoundTag nbtTag = state == null ? null : state.getNbt();
            String nbt = nbtTag == null ? "" : canonicalNbt(nbtTag);
            //Rotation comes from the entity's own NBT when it has one: FastAsyncWorldEdit's reader
            //does not carry rotation into the Location (which then reports its 0/90 default), so
            //the Location is only trustworthy where no NBT rotation exists to contradict it.
            float[] rotation = rotationFromNbt(nbtTag);
            float yaw = rotation != null ? rotation[0] : location.getYaw();
            float pitch = rotation != null ? rotation[1] : location.getPitch();
            signatures.add(
                    type + "|"
                            + Double.doubleToLongBits(
                                    location.getX() - clipboard.getOrigin().x()) + "|"
                            + Double.doubleToLongBits(
                                    location.getY() - clipboard.getOrigin().y()) + "|"
                            + Double.doubleToLongBits(
                                    location.getZ() - clipboard.getOrigin().z()) + "|"
                            + Float.floatToIntBits(yaw) + "|"
                            + Float.floatToIntBits(pitch) + "|"
                            + nbt);
        }
        signatures.sort(String::compareTo);
        return signatures;
    }

    /**
     * @return {yaw, pitch} from the entity NBT's Rotation list, or null when the entity does not
     * carry one in that shape
     */
    private static float[] rotationFromNbt(org.enginehub.linbus.tree.LinCompoundTag nbtTag) {
        if (nbtTag == null) return null;
        org.enginehub.linbus.tree.LinTag<?> rotationTag = nbtTag.value().get("Rotation");
        if (!(rotationTag instanceof org.enginehub.linbus.tree.LinListTag<?> rotationList)
                || rotationList.value().size() < 2) return null;
        if (!(rotationList.value().get(0) instanceof org.enginehub.linbus.tree.LinFloatTag yawTag)
                || !(rotationList.value().get(1) instanceof org.enginehub.linbus.tree.LinFloatTag pitchTag))
            return null;
        return new float[]{yawTag.value(), pitchTag.value()};
    }

    private static byte[] serializeForCache(Clipboard clipboard) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ClipboardWriter writer =
                     BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(buffer)) {
            writer.write(clipboard);
        }
        return buffer.toByteArray();
    }

    /**
     * The clipboard as the cache writer should see it: each entity's Location rotation restored
     * from its own NBT. FastAsyncWorldEdit's Sponge-v3 reader leaves rotation out of the WorldEdit
     * Location (which then reports its 0/90 default), and its writer discards the NBT Rotation tag
     * and re-encodes rotation from that Location — so writing a clipboard FAWE read would bake
     * 0/90 into every cached entity, corrupting armor stand rotations, and write-time verification
     * rejected the cache on every start. Upstream WorldEdit round trips rotation losslessly; there
     * no entity needs correcting and the clipboard itself is returned. The view is only ever
     * serialized — the clipboard used for pasting is never altered.
     */
    private static Clipboard rotationFaithfulView(Clipboard clipboard) {
        for (com.sk89q.worldedit.entity.Entity entity : clipboard.getEntities())
            if (correctedRotation(entity) != null) return new RotationFaithfulClipboard(clipboard);
        return clipboard;
    }

    /**
     * @return {yaw, pitch} the entity's Location should carry according to its NBT, or null when
     * the Location already agrees with the NBT or the NBT carries no rotation
     */
    private static float[] correctedRotation(com.sk89q.worldedit.entity.Entity entity) {
        com.sk89q.worldedit.entity.BaseEntity state = entity.getState();
        float[] rotation = rotationFromNbt(state == null ? null : state.getNbt());
        if (rotation == null) return null;
        com.sk89q.worldedit.util.Location location = entity.getLocation();
        if (Float.floatToIntBits(location.getYaw()) == Float.floatToIntBits(rotation[0])
                && Float.floatToIntBits(location.getPitch()) == Float.floatToIntBits(rotation[1]))
            return null;
        return rotation;
    }

    private static final class RotationFaithfulClipboard implements Clipboard {
        private final Clipboard delegate;

        private RotationFaithfulClipboard(Clipboard delegate) {
            this.delegate = delegate;
        }

        private List<com.sk89q.worldedit.entity.Entity> corrected(
                List<? extends com.sk89q.worldedit.entity.Entity> entities) {
            List<com.sk89q.worldedit.entity.Entity> wrapped = new ArrayList<>(entities.size());
            for (com.sk89q.worldedit.entity.Entity entity : entities) {
                float[] rotation = correctedRotation(entity);
                wrapped.add(rotation == null
                        ? entity
                        : new RotationCorrectedEntity(entity, rotation[0], rotation[1]));
            }
            return wrapped;
        }

        @Override
        public List<? extends com.sk89q.worldedit.entity.Entity> getEntities() {
            return corrected(delegate.getEntities());
        }

        @Override
        public List<? extends com.sk89q.worldedit.entity.Entity> getEntities(
                com.sk89q.worldedit.regions.Region region) {
            return corrected(delegate.getEntities(region));
        }

        @Override
        public com.sk89q.worldedit.entity.Entity createEntity(
                com.sk89q.worldedit.util.Location location,
                com.sk89q.worldedit.entity.BaseEntity entity) {
            return delegate.createEntity(location, entity);
        }

        @Override
        public com.sk89q.worldedit.regions.Region getRegion() {
            return delegate.getRegion();
        }

        @Override
        public BlockVector3 getDimensions() {
            return delegate.getDimensions();
        }

        @Override
        public BlockVector3 getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public void setOrigin(BlockVector3 origin) {
            delegate.setOrigin(origin);
        }

        @Override
        public boolean hasBiomes() {
            return delegate.hasBiomes();
        }

        @Override
        public BlockVector3 getMinimumPoint() {
            return delegate.getMinimumPoint();
        }

        @Override
        public BlockVector3 getMaximumPoint() {
            return delegate.getMaximumPoint();
        }

        @Override
        public com.sk89q.worldedit.world.block.BlockState getBlock(BlockVector3 position) {
            return delegate.getBlock(position);
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            return delegate.getFullBlock(position);
        }

        @Override
        public com.sk89q.worldedit.world.biome.BiomeType getBiome(BlockVector3 position) {
            return delegate.getBiome(position);
        }

        @Override
        public <T extends com.sk89q.worldedit.world.block.BlockStateHolder<T>> boolean setBlock(
                BlockVector3 position, T block) throws com.sk89q.worldedit.WorldEditException {
            return delegate.setBlock(position, block);
        }

        @Override
        public boolean setBiome(BlockVector3 position,
                                com.sk89q.worldedit.world.biome.BiomeType biome) {
            return delegate.setBiome(position, biome);
        }

        @Override
        public com.sk89q.worldedit.function.operation.Operation commit() {
            return delegate.commit();
        }
    }

    private static final class RotationCorrectedEntity implements com.sk89q.worldedit.entity.Entity {
        private final com.sk89q.worldedit.entity.Entity delegate;
        private final float yaw;
        private final float pitch;

        private RotationCorrectedEntity(com.sk89q.worldedit.entity.Entity delegate, float yaw, float pitch) {
            this.delegate = delegate;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public com.sk89q.worldedit.util.Location getLocation() {
            com.sk89q.worldedit.util.Location location = delegate.getLocation();
            return new com.sk89q.worldedit.util.Location(
                    location.getExtent(), location.toVector(), yaw, pitch);
        }

        @Override
        public boolean setLocation(com.sk89q.worldedit.util.Location location) {
            return delegate.setLocation(location);
        }

        @Override
        public com.sk89q.worldedit.entity.BaseEntity getState() {
            return delegate.getState();
        }

        @Override
        public com.sk89q.worldedit.extent.Extent getExtent() {
            return delegate.getExtent();
        }

        @Override
        public boolean remove() {
            return delegate.remove();
        }

        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return delegate.getFacet(cls);
        }
    }

    /**
     * Renders NBT with compound keys sorted recursively. Minecraft's data fixer
     * (which runs when a schematic's DataVersion is older than the server's)
     * rebuilds compounds in hash order, while the cache round trip preserves
     * write order — so a plain toString comparison rejected identical data
     * ("entities changed" with equal counts on both sides) on every start.
     */
    private static String canonicalNbt(org.enginehub.linbus.tree.LinTag<?> tag) {
        if (tag instanceof org.enginehub.linbus.tree.LinCompoundTag compound) {
            StringBuilder builder = new StringBuilder("{");
            compound.value().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> builder.append(entry.getKey()).append(':')
                            .append(canonicalNbt(entry.getValue())).append(','));
            return builder.append('}').toString();
        }
        if (tag instanceof org.enginehub.linbus.tree.LinListTag<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (Object element : list.value())
                builder.append(canonicalNbt((org.enginehub.linbus.tree.LinTag<?>) element)).append(',');
            return builder.append(']').toString();
        }
        return String.valueOf(tag);
    }

    //Once per JVM across every loader: the warning names a systemic round-trip problem, not a
    //per-folder one, and each loader hitting it would repeat the same message.
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
    private void writeCache(Clipboard clipboard, String key, File sourceFile) {
        //Unique per attempt, not per key: duplicate schematics shipped in different folders hash to
        //the same key, so keying the temporary file on that alone had two threads writing and moving
        //one shared path.
        File temporaryFile = new File(cacheFolder, key + "-" + UUID.randomUUID() + ".tmp");
        try {
            //Serialised into memory, and verified from memory, so the candidate copy is never opened
            //as a file before it is accepted. Reading it back from disk instead left the reader's
            //handle open on Windows, which then blocked the move into place, so entries silently
            //failed to publish and left .tmp files behind.
            byte[] written;
            Clipboard rotationFaithful = rotationFaithfulView(clipboard);
            try {
                written = serializeForCache(rotationFaithful);
            } catch (Throwable viewRejected) {
                if (rotationFaithful == clipboard) throw viewRejected;
                //The delegating view meets whichever WorldEdit fork is installed at runtime; if
                //that fork cannot write it, serialize the clipboard directly and let verification
                //decide whether the result is usable.
                written = serializeForCache(clipboard);
            }

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

    public void pruneStaleEntries(Collection<File> schematicFiles) {
        pruneStaleEntries(schematicFiles, () -> {
        });
    }

    /**
     * Deletes cache entries that no schematic currently maps to.
     * <p>
     * Entries are keyed by schematic contents, Minecraft data version and WorldEdit version, so
     * updating any of the three leaves the previous generation of entries behind — as does deleting
     * a content pack. Without this, the cache folder keeps a full copy of every schematic set the
     * server has ever run.
     *
     * @param schematicFiles    every schematic file this folder's loader currently owns, not just
     *                          the ones read this pass — anything unaccounted for is deleted
     * @param cancellationCheck run between files; may abort the prune by throwing
     */
    public void pruneStaleEntries(Collection<File> schematicFiles, Runnable cancellationCheck) {
        File[] cachedFiles = cacheFolder.listFiles();
        if (cachedFiles == null) return;

        Set<String> currentKeys = new HashSet<>();
        for (File schematicFile : schematicFiles) {
            cancellationCheck.run();
            String key = computedKeys.get(schematicFile);
            if (key == null) key = cacheKey(schematicFile);
            if (key != null) currentKeys.add(key);
        }
        //An empty set means every key failed to compute; deleting the whole cache off the back of
        //that would be destructive for no reason.
        if (currentKeys.isEmpty()) return;

        int pruned = 0;
        for (File cachedFile : cachedFiles) {
            cancellationCheck.run();
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
}
