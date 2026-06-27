package com.magmaguy.betterstructures.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultChestContentsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void defaultTreasureTablesExposeValidWeightedRarityBuckets() {
        assertTreasureTableSchema(DefaultChestContents.overworldContents());
        assertTreasureTableSchema(DefaultChestContents.barrelFoodContents());
        assertTreasureTableSchema(DefaultChestContents.overworldUndergroundContents());
        assertTreasureTableSchema(DefaultChestContents.netherContents());
        assertTreasureTableSchema(DefaultChestContents.endContents());
    }

    @Test
    void generatedProceduralEnchantmentSchemaUsesOnlyApplicableVanillaEnchantments() {
        Map<String, Object> generatedItems = DefaultChestContents.generateProcedurallyGeneratedItems();

        assertFalse(generatedItems.isEmpty());
        assertTrue(generatedItems.containsKey("diamond_sword"));

        int checkedEnchantments = 0;
        for (Map.Entry<String, Object> materialEntry : generatedItems.entrySet()) {
            Material material = Material.matchMaterial(materialEntry.getKey());
            assertNotNull(material, "Unknown generated material key: " + materialEntry.getKey());

            Map<?, ?> enchantments = assertInstanceOf(Map.class, materialEntry.getValue());
            for (Map.Entry<?, ?> enchantmentEntry : enchantments.entrySet()) {
                String enchantmentName = assertInstanceOf(String.class, enchantmentEntry.getKey());
                assertFalse(enchantmentName.contains(":"), "Generated defaults should store vanilla enchantment keys without namespace");

                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName));
                assertNotNull(enchantment, "Unknown generated enchantment key: " + enchantmentName);
                assertTrue(enchantment.canEnchantItem(new ItemStack(material)),
                        enchantmentName + " should be applicable to " + material);

                Map<?, ?> settings = assertInstanceOf(Map.class, enchantmentEntry.getValue());
                int minLevel = assertNumber(settings.get("minLevel")).intValue();
                int maxLevel = assertNumber(settings.get("maxLevel")).intValue();
                double chance = assertNumber(settings.get("chance")).doubleValue();
                assertTrue(minLevel >= 1);
                assertTrue(maxLevel >= minLevel);
                assertTrue(chance >= 0D && chance <= 1D);
                checkedEnchantments++;
            }
        }

        assertTrue(checkedEnchantments > 0);
    }

    @Test
    void generatedEntryOnlyMarksProceduralEnchantmentsWhenRequested() {
        Map<String, Object> plainEntry = DefaultChestContents.generateEntry(Material.DIAMOND, 2, 4, 6D);
        Map<String, Object> enchantedEntry = DefaultChestContents.generateEntry(Material.DIAMOND_SWORD, 1, 1, 3D, true);

        assertEquals("DIAMOND", plainEntry.get("material"));
        assertEquals("2-4", plainEntry.get("amount"));
        assertEquals(6D, assertNumber(plainEntry.get("weight")).doubleValue());
        assertFalse(plainEntry.containsKey("procedurallyGenerateEnchantments"));

        assertEquals("DIAMOND_SWORD", enchantedEntry.get("material"));
        assertEquals("1-1", enchantedEntry.get("amount"));
        assertEquals(3D, assertNumber(enchantedEntry.get("weight")).doubleValue());
        assertEquals(true, enchantedEntry.get("procedurallyGenerateEnchantments"));
    }

    private static void assertTreasureTableSchema(Map<String, Object> treasureTable) {
        assertRarityBucket(treasureTable, "common");
        assertRarityBucket(treasureTable, "rare");
        assertRarityBucket(treasureTable, "epic");
    }

    private static void assertRarityBucket(Map<String, Object> treasureTable, String rarity) {
        Map<?, ?> bucket = assertInstanceOf(Map.class, treasureTable.get(rarity), "Missing rarity bucket " + rarity);
        assertTrue(assertNumber(bucket.get("weight")).doubleValue() > 0D);

        List<?> items = assertInstanceOf(List.class, bucket.get("items"));
        assertFalse(items.isEmpty());

        for (Object itemObject : items) {
            Map<?, ?> item = assertInstanceOf(Map.class, itemObject);
            Material material = Material.matchMaterial(assertInstanceOf(String.class, item.get("material")));
            assertNotNull(material);
            assertAmountRange(assertInstanceOf(String.class, item.get("amount")));
            assertTrue(assertNumber(item.get("weight")).doubleValue() > 0D);
            if (item.containsKey("procedurallyGenerateEnchantments")) {
                assertEquals(true, item.get("procedurallyGenerateEnchantments"));
            }
        }
    }

    private static void assertAmountRange(String range) {
        String[] parts = range.split("-");
        assertEquals(2, parts.length);
        int min = Integer.parseInt(parts[0]);
        int max = Integer.parseInt(parts[1]);
        assertTrue(min >= 1);
        assertTrue(max >= min);
    }

    private static Number assertNumber(Object value) {
        return assertInstanceOf(Number.class, value);
    }
}
