package com.magmaguy.betterstructures.worldedit;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Temporary regression coverage for the schematic reload/cache ticket. */
final class SchematicClipboardCacheTicketTest {
    @Test
    void unchangedFileHitsCacheAndChangedSameSizeFileMisses(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("airballoon_barren.schem");
        Files.write(source, new byte[] {1, 2, 3, 4});
        Clipboard clipboard = null;
        SchematicClipboardCache cache = new SchematicClipboardCache();

        cache.put(source.toFile(), clipboard);
        assertSame(clipboard, cache.get(source.toFile()), "an unchanged schematic should be reused");

        Files.write(source, new byte[] {4, 3, 2, 1});
        assertNull(cache.get(source.toFile()),
                "a same-size rewrite must miss even when the filesystem timestamp is coarse");
    }

    @Test
    void deletedSchematicIsRemovedByRetainOnly(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("deleted.schem");
        Files.write(source, new byte[] {7});
        SchematicClipboardCache cache = new SchematicClipboardCache();
        cache.put(source.toFile(), null);
        Files.delete(source);

        assertEquals(1, cache.retainOnly(List.of()), "removed files should be evicted");
        assertNull(cache.get(source.toFile()));
    }
}
