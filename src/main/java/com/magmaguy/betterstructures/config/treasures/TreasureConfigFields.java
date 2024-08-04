package com.magmaguy.betterstructures.config.treasures;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureConfigFields extends CustomConfigFields {

    @Getter
    private final Map<Material, List<ConfigurationEnchantment>> enchantmentSettings = new HashMap<>();
    private final List<String> seenInvalidKeys = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, Object> rawLoot = new HashMap();
    @Setter
    private Map<String, Object> rawEnchantmentSettings = new HashMap<>();
    @Getter
    @Setter
    private ChestContents chestContents = null;
    @Getter
    @Setter
    private double mean = 4;
    @Getter
    @Setter
    private double standardDeviation = 3;

    public TreasureConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.rawLoot = processMapWithKey("items", rawLoot);
        this.rawEnchantmentSettings = processMapWithKey("procedurallyGeneratedItemSettings", DefaultChestContents.generateProcedurallyGeneratedItems());
        this.mean = processDouble("mean", mean, 4, true);
        this.standardDeviation = processDouble("standardDeviation", standardDeviation, 3, true);
        chestContents = new ChestContents(this);
        parseEnchantmentSettings();
    }

    private void parseEnchantmentSettings() {
        for (Map.Entry<String, Object> stringObjectEntry : rawEnchantmentSettings.entrySet()) {
            Material material = Material.matchMaterial(stringObjectEntry.getKey());
            if (material == null) {
                Logger.warn("Incorrect material entry for enchantment settings of the configuration file " + filename);
                continue;
            }
            List<ConfigurationEnchantment> configurationEnchantments = new ArrayList<>();
            Map<String, Object> enchantments = ((MemorySection) stringObjectEntry.getValue()).getValues(false);
            for (Map.Entry<String, Object> enchantmentsEntry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentsEntry.getKey()));
                if (enchantment == null && !seenInvalidKeys.contains(enchantmentsEntry.getKey())) {
                    Logger.info("Failed to get valid enchantment from key " + enchantmentsEntry.getKey() + " in configuration file " + filename + " ! This is almost certainly because another plugin " + "is using enchantments that are pretending to be vanilla Minecraft enchantments, when they aren't, " + "and doing so in a way that doesn't allow items to be enchanted via normal means. This enchantment " + "will be ignored for generating items, you can ignore this warning if you didn't plan to use this " + "enchantment in the first place. Warnings about this specific enchantment will now be suppressed.");
                    seenInvalidKeys.add(enchantmentsEntry.getKey());
                    continue;
                }
                int minLevel = 1;
                int maxLevel = 1;
                double chance = 0;
                for (Map.Entry<String, Object> enchantmentValue : ((ConfigurationSection) (enchantmentsEntry.getValue())).getValues(false).entrySet()) {
                    switch (enchantmentValue.getKey().toLowerCase(Locale.ROOT)) {
                        case "minlevel":
                            minLevel = Integer.parseInt(enchantmentValue.getValue().toString());
                            break;
                        case "maxlevel":
                            maxLevel = Integer.parseInt(enchantmentValue.getValue().toString());
                            break;
                        case "chance":
                            chance = Double.parseDouble(enchantmentValue.getValue().toString());
                            break;
                        default:
                            Logger.warn("Invalid key for setting " + enchantmentValue.getKey() + " in file " + filename);
                    }
                }
                configurationEnchantments.add(new ConfigurationEnchantment(enchantment, minLevel, maxLevel, chance));
            }
            enchantmentSettings.put(material, configurationEnchantments);
        }
    }

    public void addChestEntry(Map<String, Object> entry, String rarity, Player player) {
        List<Map<?, ?>> mapList = ((ConfigurationSection) rawLoot.get(rarity)).getMapList("items");
        mapList.add(entry);
        fileConfiguration.set("items." + rarity, Map.of("weight", ((ConfigurationSection) rawLoot.get(rarity)).getDouble("weight"), "items", mapList));
        try {
            fileConfiguration.save(file);
        } catch (Exception ex) {
            player.sendMessage("[BetterStructures] Failed to save entry to file! Report this to the developer.");
            return;
        }
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        player.sendMessage("[BetterStructures] Reloaded plugin to add chest entry! It should now be live.");
    }

    public class ConfigurationEnchantment {
        private final Enchantment enchantment;
        private final int minLevel;
        private final int maxLevel;
        private final double chance;

        public ConfigurationEnchantment(Enchantment enchantment, int minLevel, int maxLevel, double chance) {
            this.enchantment = enchantment;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.chance = chance;
        }

        public void rollEnchantment(ItemMeta itemMeta) {
            if (ThreadLocalRandom.current().nextDouble() >= chance) return;
            int level = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
            if (itemMeta != null && enchantment != null) itemMeta.addEnchant(enchantment, level, true);
        }
    }
}
