package com.magmaguy.betterstructures.config.treasures;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.CustomConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;
import com.magmaguy.betterstructures.util.WarningMessage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureConfigFields extends CustomConfigFields {

    @Getter
    private final Map<Material, List<ConfigurationEnchantment>> enchantmentSettings = new HashMap<>();
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
        this.rawLoot = processMap("items", rawLoot);
        this.rawEnchantmentSettings = processMap("procedurallyGeneratedItemSettings", DefaultChestContents.generateProcedurallyGeneratedItems());
        this.mean = processDouble("mean", mean, 4, true);
        this.standardDeviation = processDouble("standardDeviation", standardDeviation, 3, true);
        chestContents = new ChestContents(this);
        parseEnchantmentSettings();
    }

    private void parseEnchantmentSettings() {
        for (Map.Entry<String, Object> stringObjectEntry : rawEnchantmentSettings.entrySet()) {
            Material material = Material.matchMaterial(stringObjectEntry.getKey());
            if (material == null) {
                new WarningMessage("Incorrect material entry for enchantment settings of the configuration file " + filename);
                continue;
            }
            List<ConfigurationEnchantment> configurationEnchantments = new ArrayList<>();
            Map<String, Object> enchantments = ((MemorySection) stringObjectEntry.getValue()).getValues(false);
            for (Map.Entry<String, Object> enchantmentsEntry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentsEntry.getKey()));
                if (enchantment == null) {
                    new WarningMessage("Failed to get valid enchantment from key " + enchantmentsEntry.getKey() + " in configuration file " + filename);
                    continue;
                }
                int minLevel = 1;
                int maxLevel = 1;
                double chance = 0;
                for (Map.Entry<String, Object> enchantmentValue : ((ConfigurationSection) (enchantmentsEntry.getValue())).getValues(false).entrySet()) {
                    switch (enchantmentValue.getKey().toLowerCase()) {
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
                            new WarningMessage("Invalid key for setting " + enchantmentValue.getKey() + " in file " + filename);
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
        //fileConfiguration.set("items." + rarity + "items", mapList);
        HashMap map = new HashMap();
        map.put("items", mapList);
        fileConfiguration.createSection("items." + rarity, map);
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
            itemMeta.addEnchant(enchantment, level, true);
        }
    }
}
