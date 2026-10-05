package com.magmaguy.betterstructures.util;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only copies of the Bukkit registries that content loading reads.
 * <p>
 * Content configs load on a MagmaCore worker thread, but CraftRegistry resolves entries lazily into a plain
 * HashMap. A worker lookup that misses that cache writes to it while the server thread or another plugin may
 * be writing too, which throws ConcurrentModificationException and drops the config file being parsed. On
 * Paper 26.3 that silently emptied every barrel because treasure_barrel_food.yml failed to load.
 * <p>
 * {@link #capture()} runs on the server thread before the async phase. Workers then read these immutable maps
 * instead of the registry. Iterating each registry also resolves every entry into CraftRegistry's cache, so
 * lookups a worker still makes indirectly, such as ItemStack deserialization, find existing entries instead of
 * inserting new ones.
 */
public final class RegistrySnapshot {
    private static volatile Map<NamespacedKey, Enchantment> enchantments = null;
    private static volatile Map<NamespacedKey, Biome> biomes = null;

    private RegistrySnapshot() {
    }

    /**
     * Copies the registries. Registries are frozen once the server has started, so one capture per plugin
     * enable covers every later content reload.
     */
    public static void capture() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Registry snapshots must be captured on the server thread.");
        enchantments = copy(Registry.ENCHANTMENT);
        biomes = copy(Registry.BIOME);
    }

    private static <T extends Keyed> Map<NamespacedKey, T> copy(Registry<T> registry) {
        Map<NamespacedKey, T> copy = new HashMap<>();
        for (T entry : registry) {
            try {
                copy.put(entry.getKey(), entry);
            } catch (RuntimeException keyless) {
                // Legacy placeholders such as Biome.CUSTOM have no key and can't be named in configs.
            }
        }
        return Map.copyOf(copy);
    }

    @Nullable
    public static Enchantment enchantment(NamespacedKey key) {
        Map<NamespacedKey, Enchantment> snapshot = enchantments;
        return snapshot != null ? snapshot.get(key) : Registry.ENCHANTMENT.get(key);
    }

    /**
     * Thread-safe replacement for {@link Biome#valueOf(String)}: accepts a vanilla biome name such as PLAINS, or a
     * namespaced key, in any case.
     *
     * @throws IllegalArgumentException when no biome has that name
     */
    public static Biome biome(String name) {
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        if (key == null) throw new IllegalArgumentException("No biome found with the name " + name);
        Map<NamespacedKey, Biome> snapshot = biomes;
        Biome biome = snapshot != null ? snapshot.get(key) : Registry.BIOME.get(key);
        if (biome == null) throw new IllegalArgumentException("No biome found with the name " + name);
        return biome;
    }
}
