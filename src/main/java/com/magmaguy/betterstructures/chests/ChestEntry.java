package com.magmaguy.betterstructures.chests;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class ChestEntry {
    private Material material;
    private double chance;
    private int minAmount;
    private int maxAmount;
    private ItemStack itemStack;

    public ChestEntry(Material material, double chance, int minAmount, int maxAmount, ItemStack itemStack) {
        this.material = material;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.itemStack = itemStack;
    }

    public ItemStack rollEntry() {
        if (ThreadLocalRandom.current().nextDouble() > chance) return null;
        int amount;
        if (minAmount != maxAmount) amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        else amount = minAmount;
        if (material != null)
            return new ItemStack(material, amount);
        ItemStack finalItemStack = itemStack.clone();
        finalItemStack.setAmount(amount);
        return finalItemStack;
    }
}
