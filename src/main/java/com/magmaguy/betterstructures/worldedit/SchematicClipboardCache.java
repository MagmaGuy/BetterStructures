package com.magmaguy.betterstructures.worldedit;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Keeps parsed schematics in memory so a content reload does not re-parse files that did not change.
 * <p>
 * {@code /betterstructures reload} exists to pick up edited configuration, and config.yml is not part
 * of the schematic set — but every reload used to re-read and re-convert every .schem anyway. On a
 * full content install that is 681 files, and acceptance runs measured ~8s of the ~15s reload going
 * into nothing but producing clipboards byte-identical to the ones already in memory.
 * <p>
 * A file is considered unchanged when its path, size, last-modified timestamp, and content digest
 * all match what they were when it was parsed. Timestamps come from {@link BasicFileAttributes} rather than
 * {@link File#lastModified()} so they carry the full resolution the filesystem records (100ns on
 * NTFS) instead of being truncated to milliseconds, which closes the window where a file could be
 * rewritten to the same size within the same millisecond and still look unchanged.
 * <p>
 * Deleting a schematic is handled by {@link #retainOnly(Collection)}: the caller re-walks the folder
 * every reload and only files it actually found stay cached. Adding one is a plain miss, because
 * nothing was ever cached under its path.
 * <p>
 * Nothing here is a source of truth. Every lookup that cannot be proven current is a miss and falls
 * through to a real read, so the worst a stale or unreadable entry can do is cost the reload the
 * time it used to spend anyway.
 */
public final class SchematicClipboardCache {
    //Registered so a plugin shutdown can drop every cached clipboard without each cache's owner
    //having to expose its own teardown hook.
    private static final Set<SchematicClipboardCache> caches = ConcurrentHashMap.newKeySet();

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public SchematicClipboardCache() {
        caches.add(this);
    }

    /**
     * @return the clipboard parsed from this exact file, or null when it was never cached, has been
     * modified since, or can no longer be read
     */
    public Clipboard get(File schematicFile) {
        Entry entry = entries.get(pathKey(schematicFile));
        if (entry == null) return null;
        Identity identity = identify(schematicFile);
        if (identity == null || !identity.equals(entry.identity())) return null;
        return entry.clipboard();
    }

    public void put(File schematicFile, Clipboard clipboard) {
        Identity identity = identify(schematicFile);
        //A file that cannot be stated now cannot be proven unchanged later, so it is simply never
        //cached rather than cached under an identity that can never match.
        if (identity == null) return;
        entries.put(pathKey(schematicFile), new Entry(identity, clipboard));
    }

    /**
     * Drops every entry whose file is no longer part of the schematic set.
     *
     * @return how many entries were dropped, which tells the caller whether the set changed at all
     */
    public int retainOnly(Collection<File> schematicFiles) {
        Set<String> present = new HashSet<>();
        for (File schematicFile : schematicFiles) present.add(pathKey(schematicFile));
        int before = entries.size();
        entries.keySet().removeIf(key -> !present.contains(key));
        return before - entries.size();
    }

    /**
     * Releases every cached clipboard. Called when the plugin is disabled, for the same reason the
     * other static content registries are cleared there: the schematic set is large, and a disabled
     * plugin should not be the reason it stays resident.
     */
    public static void shutdown() {
        for (SchematicClipboardCache cache : caches) cache.entries.clear();
    }

    private static String pathKey(File schematicFile) {
        return schematicFile.getAbsoluteFile().getPath();
    }

    /**
     * @return the file's size, modification time, and content digest, or null if it could not be read
     */
    private static Identity identify(File schematicFile) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                    schematicFile.toPath(),
                    BasicFileAttributes.class);
            return new Identity(
                    attributes.size(),
                    attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS),
                    sha256(schematicFile));
        } catch (IOException | RuntimeException | NoSuchAlgorithmException exception) {
            return null;
        }
    }

    private static String sha256(File schematicFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(schematicFile.toPath())) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private record Identity(long size, long lastModifiedNanos, String sha256) {
    }

    private record Entry(Identity identity, Clipboard clipboard) {
    }
}
