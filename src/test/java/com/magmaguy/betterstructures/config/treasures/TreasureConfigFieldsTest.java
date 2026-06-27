package com.magmaguy.betterstructures.config.treasures;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasureConfigFieldsTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void parsesOnlyValidVanillaEnchantmentSettings() throws Exception {
        Enchantment sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
        assertNotNull(sharpness);
        MemoryConfiguration configuration = new MemoryConfiguration();
        ConfigurationSection swordSettings = configuration.createSection("DIAMOND_SWORD");
        swordSettings.createSection("sharpness", Map.of("minLevel", 2, "maxLevel", 2, "chance", 1D));
        swordSettings.createSection("minecraft:not_real", Map.of("minLevel", 1, "maxLevel", 1, "chance", 1D));
        swordSettings.createSection("thirdparty:glow", Map.of("minLevel", 1, "maxLevel", 1, "chance", 1D));

        TreasureConfigFields fields = new TreasureConfigFields("test.yml", true);
        fields.setRawEnchantmentSettings(configuration.getValues(false));

        Method parseMethod = TreasureConfigFields.class.getDeclaredMethod("parseEnchantmentSettings");
        parseMethod.setAccessible(true);
        parseMethod.invoke(fields);

        assertTrue(fields.getEnchantmentSettings().containsKey(Material.DIAMOND_SWORD));
        assertEquals(1, fields.getEnchantmentSettings().get(Material.DIAMOND_SWORD).size());

        ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = itemStack.getItemMeta();
        fields.getEnchantmentSettings().get(Material.DIAMOND_SWORD).get(0).rollEnchantment(meta);

        assertTrue(meta.hasEnchant(sharpness));
        assertEquals(2, meta.getEnchantLevel(sharpness));
    }
}
