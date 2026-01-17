package com.magmaguy.betterstructures.chests;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.betterstructures.util.WeighedProbability;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChestContents {

    @Getter
    private final List<ChestRarity> chestRarities = new ArrayList<>();
    private final TreasureConfigFields treasureConfigFields;

    /*
    Entry format:
    - material: MATERIAL
      amount: min-max
      chance: chance
      mmoitem: mmoitem entry goes here
      serialized: serialized string goes here
      info: information about the item goes here. Useful for a human-readable explanation of what the entry is if serialized
     */
    public ChestContents(TreasureConfigFields treasureConfigFields) {
        this.treasureConfigFields = treasureConfigFields;
        if (treasureConfigFields.getRawLoot() == null) return;
        processRarities(treasureConfigFields.getRawLoot());
    }

    private Material getMaterial(String string) {
        try {
            return Material.getMaterial(string.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            Logger.warn("Invalid material detected! Problematic entry: " + string + " in configuration file " + treasureConfigFields.getFilename());
            return null;
        }
    }

    private double getWeight(String string) {
        try {
            return Double.parseDouble(string);
        } catch (Exception exception) {
            Logger.warn("Invalid double value detected! Problematic entry: " + string + " in configuration file " + treasureConfigFields.getFilename());
            return -1;
        }
    }

    private void processRarities(Map<String, Object> rawChestEntries) {
        for (Map.Entry<String, Object> entry : rawChestEntries.entrySet()) {
            double weight = -1;
            List<ChestEntry> chestEntries = null;
            for (Map.Entry<String, Object> innerEntry : ((MemorySection) entry.getValue()).getValues(false).entrySet()) {
                switch (innerEntry.getKey().toLowerCase(Locale.ROOT)) {
                    case "weight" -> weight = getWeight(innerEntry.getValue().toString());
                    case "items" -> chestEntries = processEntries((List<Map<String, ?>>) innerEntry.getValue());
                    default -> Logger.warn("Failed to read key " + innerEntry.getKey() + " for configuration file " + treasureConfigFields.getFilename());
                }
            }
            if (weight > 0 && chestEntries != null) chestRarities.add(new ChestRarity(weight, chestEntries));
        }
    }

    private ItemStack getSerializedItemStack(Map<String, Object> deserializedItemStack, String string) {
        try {
            return ItemStackSerialization.serializeItem(deserializedItemStack);
        } catch (Exception ex) {
            Logger.warn("Invalid serialized value detected! Problematic entry: " + string + " for configuration file " + treasureConfigFields.getFilename());
            ex.printStackTrace();
            return null;
        }
    }

    private boolean getProcedurallyGeneratedEnchantments(String string) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception ex) {
            Logger.warn("Invalid boolean value detected! Problematic entry: " + string + " for configuration file " + treasureConfigFields.getFilename());
            ex.printStackTrace();
            return false;
        }
    }

    private ItemStack getMMOItemsItemStack(String string) {
        try {
            String[] args = string.split("@");
            MMOItems mmo = MMOItems.plugin;
            MMOItem mmoitem = mmo.getMMOItem(mmo.getTypes().get(args[0]), args[1]);
            if (mmoitem == null) throw new NullPointerException("mmo item is null");
            return mmoitem.newBuilder().build();
        } catch (Exception ex) {
            Logger.warn("Invalid mmo item detected! Problematic entry: " + string + " in " + treasureConfigFields.getFilename());
            return null;
        }
    }

    private List<ChestEntry> processEntries(List<Map<String, ?>> rawChestEntries) {
        List<ChestEntry> chestEntries = new ArrayList<>();
        for (Map<String, ?> rawChestEntry : rawChestEntries) {
            Material material = null;
            int minAmount = -1;
            int maxAmount = -1;
            double weight = -1;
            boolean procedurallyGeneratedEnchantments = false;
            ItemStack itemStack = null;
            for (Map.Entry<String, ?> entry : rawChestEntry.entrySet()) {
                String value = entry.getValue().toString();
                switch (entry.getKey().toLowerCase(Locale.ROOT)) {
                    case "material" -> material = getMaterial(value);
                    case "amount" -> {
                        try {
                            if (value.contains("-")) {
                                String[] amounts = value.split("-");
                                minAmount = Integer.parseInt(amounts[0]);
                                maxAmount = Integer.parseInt(amounts[1]);
                            } else {
                                minAmount = Integer.parseInt(value);
                                maxAmount = minAmount;
                            }
                        } catch (Exception exception) {
                            Logger.warn("Invalid amount detected! Problematic entry: " + value + " in file " + treasureConfigFields.getFilename());
                        }
                    }
                    case "weight" -> weight = getWeight(value);
                    //Support for MMOItems - og code by Carm
                    case "mmoitem", "mmoitems" -> itemStack = getMMOItemsItemStack(value);
                    case "serialized" -> itemStack = getSerializedItemStack((Map<String, Object>) entry.getValue(), value);
                    case "procedurallygenerateenchantments" -> procedurallyGeneratedEnchantments = getProcedurallyGeneratedEnchantments(value);
                    case "info" -> {
                    }
                    default -> Logger.warn("Failed to read key " + entry.getKey() + " for configuration file " + treasureConfigFields.getFilename());
                }
            }
            if (material != null || itemStack != null) {
                ChestEntry chestEntry = new ChestEntry(material, weight, minAmount, maxAmount, itemStack, procedurallyGeneratedEnchantments, treasureConfigFields);
                chestEntries.add(chestEntry);
            }
        }
        return chestEntries;
    }

    public void rollChestContents(Container chest) {
        // Roll custom loot if available
        if (!chestRarities.isEmpty()) {
            rollCustomLoot(chest);
        }

        // Roll vanilla loot if available
        LootTables vanillaTreasure = treasureConfigFields.getVanillaTreasure();
        if (vanillaTreasure != null) {
            rollVanillaLoot(chest, vanillaTreasure);
        }
    }

    private void rollCustomLoot(Container chest) {
        int amount = (int) Math.max(Math.ceil(ThreadLocalRandom.current().nextGaussian(treasureConfigFields.getMean(), treasureConfigFields.getStandardDeviation())), 0);
        //Guarantee that at least one item will drop
        amount++;
        HashMap<Integer, Double> weightsMap = new HashMap<>();
        for (int i = chestRarities.size() - 1; i >= 0; i--)
            weightsMap.put(i, chestRarities.get(i).chestWeight);

        for (int i = 0; i < amount; i++) {
            ItemStack itemStack = chestRarities.get(WeighedProbability.pickWeightedProbability(weightsMap)).rollLoot();
            if (itemStack != null) {
                placeItemInChest(chest, itemStack);
            }
        }
    }

    private void rollVanillaLoot(Container chest, LootTables lootTable) {
        LootContext lootContext = new LootContext.Builder(chest.getLocation()).build();
        Collection<ItemStack> loot = lootTable.getLootTable().populateLoot(ThreadLocalRandom.current(), lootContext);
        for (ItemStack itemStack : loot) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                placeItemInChest(chest, itemStack);
            }
        }
    }

    private void placeItemInChest(Container chest, ItemStack itemStack) {
        int counter = 0;
        while (counter < 100) {
            int randomizedIndex = ThreadLocalRandom.current().nextInt(0, chest.getSnapshotInventory().getSize());
            if (chest.getSnapshotInventory().getItem(randomizedIndex) == null) {
                chest.getSnapshotInventory().setItem(randomizedIndex, itemStack);
                break;
            }
            counter++;
        }
    }

    private class ChestRarity {
        private final double chestWeight;
        private final List<ChestEntry> chestEntries;

        public ChestRarity(double chestWeight, List<ChestEntry> chestEntries) {
            this.chestEntries = chestEntries;
            this.chestWeight = chestWeight;
        }

        public ItemStack rollLoot() {
            HashMap<Integer, Double> weightsMap = new HashMap<>();
            for (int i = chestEntries.size() - 1; i >= 0; i--)
                weightsMap.put(i, chestEntries.get(i).getWeight());
            return chestEntries.get(WeighedProbability.pickWeightedProbability(weightsMap)).rollEntry();
        }
    }
}
