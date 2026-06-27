package com.magmaguy.betterstructures.chests;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestEntryTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void rollsMaterialEntriesWithConfiguredAmount() {
        ChestEntry entry = new ChestEntry(Material.DIAMOND, 1D, 3, 3, null, false, null);

        ItemStack itemStack = entry.rollEntry();

        assertNotNull(itemStack);
        assertEquals(Material.DIAMOND, itemStack.getType());
        assertEquals(3, itemStack.getAmount());
    }

    @Test
    void rollsClonedSerializedStyleItemStacksWithoutMutatingTemplate() {
        ItemStack template = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = template.getItemMeta();
        meta.setDisplayName("Dungeon Marker");
        template.setItemMeta(meta);
        ChestEntry entry = new ChestEntry(null, 1D, 4, 4, template, false, null);

        ItemStack itemStack = entry.rollEntry();

        assertNotNull(itemStack);
        assertNotSame(template, itemStack);
        assertEquals(Material.EMERALD, itemStack.getType());
        assertEquals(4, itemStack.getAmount());
        assertEquals(1, template.getAmount());
        assertEquals("Dungeon Marker", itemStack.getItemMeta().getDisplayName());
    }

    @Test
    void appliesProceduralEnchantmentsWhenConfigured() {
        Enchantment sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
        assertNotNull(sharpness);
        TreasureConfigFields fields = new TreasureConfigFields("test.yml", true);
        fields.getEnchantmentSettings().put(
                Material.DIAMOND_SWORD,
                List.of(fields.new ConfigurationEnchantment(sharpness, 2, 2, 1D)));
        ChestEntry entry = new ChestEntry(Material.DIAMOND_SWORD, 1D, 1, 1, null, true, fields);

        ItemStack itemStack = entry.rollEntry();

        assertNotNull(itemStack);
        assertTrue(itemStack.getItemMeta().hasEnchant(sharpness));
        assertEquals(2, itemStack.getItemMeta().getEnchantLevel(sharpness));
    }
}
