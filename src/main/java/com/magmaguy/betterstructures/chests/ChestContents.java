package com.magmaguy.betterstructures.chests;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.magmaguy.betterstructures.util.WeighedProbability;
import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            return Material.getMaterial(string.toUpperCase());
        } catch (Exception exception) {
            new WarningMessage("Invalid material detected! Problematic entry: " + string + " in configuration file " + treasureConfigFields.getFilename());
            return null;
        }
    }

    private double getWeight(String string) {
        try {
            return Double.parseDouble(string);
        } catch (Exception exception) {
            new WarningMessage("Invalid double value detected! Problematic entry: " + string + " in configuration file " + treasureConfigFields.getFilename());
            return -1;
        }
    }

    private void processRarities(Map<String, Object> rawChestEntries) {
        for (Map.Entry<String, Object> entry : rawChestEntries.entrySet()) {
            double weight = -1;
            List<ChestEntry> chestEntries = null;
            for (Map.Entry<String, Object> innerEntry : ((MemorySection) entry.getValue()).getValues(false).entrySet()) {
                switch (innerEntry.getKey().toLowerCase()) {
                    case "weight" -> weight = getWeight(innerEntry.getValue().toString());
                    case "items" -> chestEntries = processEntries((List<Map<String, ?>>) innerEntry.getValue());
                    default -> new WarningMessage("Failed to read key " + innerEntry.getKey() + " for configuration file " + treasureConfigFields.getFilename());
                }
            }
            if (weight > 0 && chestEntries != null) chestRarities.add(new ChestRarity(weight, chestEntries));
        }
    }

    private ItemStack getSerializedItemStack(String string) {
        try {
            return ItemStackSerialization.itemStackArrayFromBase64(string.replace("serialized=", ""));
        } catch (Exception ex) {
            new WarningMessage("Invalid serialized value detected! Problematic entry: " + string + " for configuration file " + treasureConfigFields.getFilename());
            ex.printStackTrace();
            return null;
        }
    }

    private boolean getProcedurallyGeneratedEnchantments(String string) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception ex) {
            new WarningMessage("Invalid boolean value detected! Problematic entry: " + string + " for configuration file " + treasureConfigFields.getFilename());
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
            new WarningMessage("Invalid mmo item detected! Problematic entry: " + string + " in " + treasureConfigFields.getFilename());
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
                switch (entry.getKey().toLowerCase()) {
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
                            new WarningMessage("Invalid amount detected! Problematic entry: " + value + " in file " + treasureConfigFields.getFilename());
                        }
                    }
                    case "weight" -> weight = getWeight(value);
                    //Support for MMOItems - og code by Carm
                    case "mmoitem", "mmoitems" -> itemStack = getMMOItemsItemStack(value);
                    case "serialized" -> itemStack = getSerializedItemStack(value);
                    case "procedurallygenerateenchantments" -> procedurallyGeneratedEnchantments = getProcedurallyGeneratedEnchantments(value);
                    case "info" -> {
                    }
                    default -> new WarningMessage("Failed to read key " + entry.getKey() + " for configuration file " + treasureConfigFields.getFilename());
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
        int amount = (int) Math.max(Math.ceil(ThreadLocalRandom.current().nextGaussian(treasureConfigFields.getMean(), treasureConfigFields.getStandardDeviation())), 0);
        //Guarantee that at least one item will drop
        amount++;
        HashMap<Integer, Double> weightsMap = new HashMap<>();
        for (int i = chestRarities.size() - 1; i >= 0; i--)
            weightsMap.put(i, chestRarities.get(i).chestWeight);

        for (int i = 0; i < amount; i++) {
            ItemStack itemStack = chestRarities.get(WeighedProbability.pickWeighedProbability(weightsMap)).rollLoot();
            if (itemStack != null) chest.getSnapshotInventory().addItem(itemStack);
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
            return chestEntries.get(WeighedProbability.pickWeighedProbability(weightsMap)).rollEntry();
        }
    }
}
