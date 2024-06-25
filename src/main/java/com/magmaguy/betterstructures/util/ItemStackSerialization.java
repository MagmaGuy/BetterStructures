package com.magmaguy.betterstructures.util;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemStackSerialization {
    private ItemStackSerialization() {
    }

    public static Map<String, Object> deserializeItem(ItemStack itemStack) throws IllegalStateException {
        return itemStack.serialize();
    }

    public static ItemStack serializeItem(Map<String, Object> deserializedItemStack) {
        try {
            return ItemStack.deserialize(deserializedItemStack);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
