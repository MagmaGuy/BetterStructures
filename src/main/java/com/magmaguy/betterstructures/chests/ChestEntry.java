package com.magmaguy.betterstructures.chests;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChestEntry {
    private final Material material;
    @Getter
    private final double weight;
    private final int minAmount;
    private final int maxAmount;
    private final ItemStack itemStack;
    private final boolean procedurallyGeneratedEnchantments;
    private final TreasureConfigFields treasureConfigFields;

    public ChestEntry(Material material, double chance, int minAmount, int maxAmount, ItemStack itemStack, boolean procedurallyGeneratedEnchantments, TreasureConfigFields treasureConfigFields) {
        this.material = material;
        this.weight = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.itemStack = itemStack;
        this.procedurallyGeneratedEnchantments = procedurallyGeneratedEnchantments;
        this.treasureConfigFields = treasureConfigFields;
    }

    public ItemStack rollEntry() {
        int amount;
        if (minAmount != maxAmount) amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        else amount = minAmount;
        try {
            if (material != null) {
                ItemStack itemStack = new ItemStack(material, amount);
                if (!procedurallyGeneratedEnchantments)
                    return itemStack;
                List<TreasureConfigFields.ConfigurationEnchantment> configurationEnchantmentList = treasureConfigFields.getEnchantmentSettings().get(material);
                if (configurationEnchantmentList == null || configurationEnchantmentList.isEmpty()) return itemStack;
                ItemMeta itemMeta = itemStack.getItemMeta();
                for (TreasureConfigFields.ConfigurationEnchantment configurationEnchantment : configurationEnchantmentList) {
                    configurationEnchantment.rollEnchantment(itemMeta);
                }
                itemStack.setItemMeta(itemMeta);
                return itemStack;
            }
            ItemStack finalItemStack = itemStack.clone();
            finalItemStack.setAmount(amount);
            return finalItemStack;
        } catch (Exception e) {
            return  null;
            //some items won't work in later versions as the materials get renamed
        }
    }
}
