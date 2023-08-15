package com.magmaguy.betterstructures.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultChestContents {
    public static double normalWeight = 6;
    public static double rareWeight = 3;
    public static double extraRareWeight = 1;

    public static Map<String, Object> overworldContents() {
        Map<String, Object> items = new HashMap<>();
        Map<String, Object> commonItems = new HashMap<>();
        Map<String, Object> rareItems = new HashMap<>();
        Map<String, Object> epicItems = new HashMap<>();
        List<Map<String, Object>> commonList = new ArrayList<>();
        List<Map<String, Object>> rareList = new ArrayList<>();
        List<Map<String, Object>> epicList = new ArrayList<>();

        commonList.add(generateEntry(Material.ARROW, 16, 32, normalWeight));
        commonList.add(generateEntry(Material.BAKED_POTATO, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.BAMBOO, 2, 7, normalWeight));
        commonList.add(generateEntry(Material.BAMBOO_SAPLING, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.BEEF, 4, 8, normalWeight));
        commonList.add(generateEntry(Material.BELL, 1, 1, rareWeight));
        commonList.add(generateEntry(Material.BLACK_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.BLACK_WOOL, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.BLUE_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.BLUE_WOOL, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.BONE, 2, 5, normalWeight));
        commonList.add(generateEntry(Material.BONE_BLOCK, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.BOOK, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.BOW, 1, 1, normalWeight, true));
        commonList.add(generateEntry(Material.BREAD, 8, 16, normalWeight));
        if (!VersionChecker.serverVersionOlderThan(1, 20))
            commonList.add(generateEntry(Material.BRUSH, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.BROWN_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.BROWN_WOOL, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.CANDLE, 1, 5, normalWeight));
        commonList.add(generateEntry(Material.CARROT, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.CARROT_ON_A_STICK, 1, 1, rareWeight));
        commonList.add(generateEntry(Material.CHAIN, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.CHEST, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.CLAY_BALL, 1, 16, normalWeight));
        commonList.add(generateEntry(Material.CLOCK, 1, 1, rareWeight));
        commonList.add(generateEntry(Material.COAL, 2, 5, normalWeight));
        commonList.add(generateEntry(Material.COPPER_INGOT, 3, 9, extraRareWeight));
        commonList.add(generateEntry(Material.COCOA_BEANS, 4, 12, normalWeight));
        commonList.add(generateEntry(Material.COOKED_BEEF, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_CHICKEN, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_COD, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_MUTTON, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_PORKCHOP, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_RABBIT, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_SALMON, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKIE, 32, 64, extraRareWeight));
        commonList.add(generateEntry(Material.COMPASS, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.CYAN_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.CYAN_WOOL, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.DRIED_KELP, 8, 48, normalWeight));
        commonList.add(generateEntry(Material.EMERALD, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.EXPERIENCE_BOTTLE, 1, 2, normalWeight));
        commonList.add(generateEntry(Material.FEATHER, 2, 16, normalWeight));
        commonList.add(generateEntry(Material.FISHING_ROD, 1, 1, normalWeight, true));
        commonList.add(generateEntry(Material.FLINT, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.FLINT_AND_STEEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.FLOWER_POT, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.ORANGE_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.GLISTERING_MELON_SLICE, 1, 4, extraRareWeight));
        commonList.add(generateEntry(Material.GRAY_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.GREEN_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.PINK_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.PURPLE_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_BOOTS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_BOOTS, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_HORSE_ARMOR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LIME_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.MAGENTA_DYE, 1, 3, normalWeight));
        if (!VersionChecker.serverVersionOlderThan(1, 19))
            commonList.add(generateEntry(Material.MUSIC_DISC_5, 1, 1, extraRareWeight)); //1.19
        commonList.add(generateEntry(Material.MUSIC_DISC_11, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_13, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_BLOCKS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CAT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CHIRP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_FAR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MALL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MELLOHI, 1, 1, extraRareWeight));
        if (!VersionChecker.serverVersionOlderThan(1, 18))
            commonList.add(generateEntry(Material.MUSIC_DISC_OTHERSIDE, 1, 1, extraRareWeight)); //1.18
        commonList.add(generateEntry(Material.MUSIC_DISC_PIGSTEP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STAL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STRAD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WAIT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WARD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.YELLOW_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.SADDLE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEAD, 3, 9, normalWeight));
        commonList.add(generateEntry(Material.SHEARS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.NAME_TAG, 3, 9, normalWeight));
        commonList.add(generateEntry(Material.HEART_OF_THE_SEA, 1, 1, rareWeight));
        commonList.add(generateEntry(Material.HONEYCOMB, 2, 5, rareWeight));
        commonList.add(generateEntry(Material.WHITE_DYE, 1, 3, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.BOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.ANVIL, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.BEETROOT, 1, 6, normalWeight));
        rareList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.BEETROOT_SEEDS, 1, 6, normalWeight));
        rareList.add(generateEntry(Material.BONE_MEAL, 2, 12, normalWeight));
        rareList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.DIAMOND_HORSE_ARMOR, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HORSE_ARMOR, 1, 1, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_INGOT, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.SHIELD, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.GLASS_BOTTLE, 2, 8, rareWeight));
        rareList.add(generateEntry(Material.GLOW_BERRIES, 1, 3, normalWeight));
        rareList.add(generateEntry(Material.GOLDEN_APPLE, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.GOLDEN_CARROT, 3, 9, rareWeight));
        rareList.add(generateEntry(Material.SPYGLASS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.MUSHROOM_STEW, 3, 9, normalWeight));
        rareList.add(generateEntry(Material.WRITABLE_BOOK, 1, 8, rareWeight));
        rareList.add(generateEntry(Material.BUNDLE, 1, 1, rareWeight));
        epicList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.SPECTRAL_ARROW, 8, 16, normalWeight, true));
        epicList.add(generateEntry(Material.SLIME_BALL, 16, 32, normalWeight, true));
        epicList.add(generateEntry(Material.MAP, 1, 6, normalWeight));
        epicList.add(generateEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 6, extraRareWeight));
        epicList.add(generateEntry(Material.PUMPKIN_PIE, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.RABBIT_STEW, 3, 9, extraRareWeight));
        epicList.add(generateEntry(Material.GLOW_INK_SAC, 8, 16, extraRareWeight));
        epicList.add(generateEntry(Material.TOTEM_OF_UNDYING, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.HONEY_BOTTLE, 1, 4, normalWeight));

        commonItems.put("weight", 60);
        commonItems.put("items", commonList);
        rareItems.put("weight", 30);
        rareItems.put("items", rareList);
        epicItems.put("weight", 10);
        epicItems.put("items", epicList);
        items.put("common", commonItems);
        items.put("rare", rareItems);
        items.put("epic", epicItems);
        return items;
    }

    public static Map<String, Object> overworldUndergroundContents() {
        //Clones the list from above ground
        Map<String, Object> items = new HashMap<>(overworldContents());
        Map<String, Object> commonItems = new HashMap<>();
        Map<String, Object> rareItems = new HashMap<>();
        Map<String, Object> epicItems = new HashMap<>();
        List<Map<String, Object>> commonList = new ArrayList<>();
        List<Map<String, Object>> rareList = new ArrayList<>();
        List<Map<String, Object>> epicList = new ArrayList<>();

        commonList.add(generateEntry(Material.RAIL, 2, 5, normalWeight));
        commonList.add(generateEntry(Material.PISTON, 3, 9, extraRareWeight));
        commonList.add(generateEntry(Material.TNT, 2, 6, extraRareWeight));
        commonList.add(generateEntry(Material.MINECART, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.MINECART, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.ACTIVATOR_RAIL, 1, 3, rareWeight));
        commonList.add(generateEntry(Material.POWERED_RAIL, 1, 5, rareWeight));
        commonList.add(generateEntry(Material.LAPIS_LAZULI, 1, 16, normalWeight));
        commonList.add(generateEntry(Material.IRON_BLOCK, 1, 3, rareWeight));
        commonList.add(generateEntry(Material.ARROW, 16, 32, normalWeight));
        commonList.add(generateEntry(Material.BREAD, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.CHAIN, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.CHEST, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.CLAY_BALL, 1, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_BEEF, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_PORKCHOP, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COMPASS, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.EMERALD, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.EXPERIENCE_BOTTLE, 1, 2, normalWeight));
        commonList.add(generateEntry(Material.FLINT, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.FLINT_AND_STEEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.GLISTERING_MELON_SLICE, 1, 4, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_BOOTS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_HORSE_ARMOR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.MUSIC_DISC_11, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_13, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_BLOCKS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CAT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CHIRP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_FAR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MALL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MELLOHI, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_PIGSTEP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STAL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STRAD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WAIT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WARD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.BOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.CHEST_MINECART, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        rareList.add(generateEntry(Material.LANTERN, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.BONE_MEAL, 2, 12, normalWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.DIAMOND_HORSE_ARMOR, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HORSE_ARMOR, 1, 1, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_INGOT, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.SHIELD, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.GLASS_BOTTLE, 2, 8, rareWeight));
        rareList.add(generateEntry(Material.GOLDEN_APPLE, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.GOLDEN_CARROT, 3, 9, rareWeight));
        rareList.add(generateEntry(Material.SPYGLASS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.MUSHROOM_STEW, 3, 9, normalWeight));
        rareList.add(generateEntry(Material.BUNDLE, 1, 1, rareWeight));
        epicList.add(generateEntry(Material.KNOWLEDGE_BOOK, 1, 1, extraRareWeight)); //Reveals all recipes
        epicList.add(generateEntry(Material.TNT_MINECART, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.SCULK, 1, 6, normalWeight)); //1.19
        epicList.add(generateEntry(Material.TOTEM_OF_UNDYING, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.SPECTRAL_ARROW, 8, 16, normalWeight, true));
        epicList.add(generateEntry(Material.SLIME_BALL, 16, 32, normalWeight, true));
        epicList.add(generateEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 6, extraRareWeight));
        epicList.add(generateEntry(Material.PUMPKIN_PIE, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.TOTEM_OF_UNDYING, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight, true));

        commonItems.put("weight", 60);
        commonItems.put("items", commonList);
        rareItems.put("weight", 30);
        rareItems.put("items", rareList);
        epicItems.put("weight", 10);
        epicItems.put("items", epicList);
        items.put("common", commonItems);
        items.put("rare", rareItems);
        items.put("epic", epicItems);
        return items;
    }

    public static Map<String, Object> netherContents() {
        Map<String, Object> items = new HashMap<>();
        Map<String, Object> commonItems = new HashMap<>();
        Map<String, Object> rareItems = new HashMap<>();
        Map<String, Object> epicItems = new HashMap<>();
        List<Map<String, Object>> commonList = new ArrayList<>();
        List<Map<String, Object>> rareList = new ArrayList<>();
        List<Map<String, Object>> epicList = new ArrayList<>();

        commonList.add(generateEntry(Material.GILDED_BLACKSTONE, 1, 6, rareWeight));
        commonList.add(generateEntry(Material.GHAST_TEAR, 1, 1, rareWeight));
        commonList.add(generateEntry(Material.NETHERITE_SCRAP, 1, 6, rareWeight));
        commonList.add(generateEntry(Material.IRON_NUGGET, 3, 9, extraRareWeight));
        commonList.add(generateEntry(Material.ANCIENT_DEBRIS, 1, 5, rareWeight));
        commonList.add(generateEntry(Material.MAGMA_CREAM, 1, 5, normalWeight));
        commonList.add(generateEntry(Material.GOLD_NUGGET, 3, 9, normalWeight));
        commonList.add(generateEntry(Material.GOLDEN_AXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_BOOTS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_CHESTPLATE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_HELMET, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_HOE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_LEGGINGS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_PICKAXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_SHOVEL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_SWORD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_AXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_BOOTS, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_CHESTPLATE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_HELMET, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_HOE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_HORSE_ARMOR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.GOLDEN_LEGGINGS, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_PICKAXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_SHOVEL, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.GOLDEN_SWORD, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.ARROW, 16, 32, normalWeight));
        commonList.add(generateEntry(Material.BREAD, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.CHAIN, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.CHEST, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.CLAY_BALL, 1, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_BEEF, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_PORKCHOP, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COMPASS, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.EMERALD, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.EXPERIENCE_BOTTLE, 1, 2, normalWeight));
        commonList.add(generateEntry(Material.FLINT, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.FLINT_AND_STEEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.GLISTERING_MELON_SLICE, 1, 4, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_BOOTS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_HORSE_ARMOR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.MUSIC_DISC_11, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_13, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_BLOCKS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CAT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CHIRP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_FAR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MALL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MELLOHI, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_PIGSTEP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STAL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STRAD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WAIT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WARD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.BOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.BLAZE_ROD, 3, 9, normalWeight));
        rareList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        rareList.add(generateEntry(Material.GOLD_INGOT, 1, 3, normalWeight));
        rareList.add(generateEntry(Material.GOLD_INGOT, 6, 12, extraRareWeight));
        rareList.add(generateEntry(Material.FIRE_CHARGE, 1, 3, extraRareWeight));
        rareList.add(generateEntry(Material.NETHER_QUARTZ_ORE, 6, 27, rareWeight));
        rareList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.BONE_MEAL, 2, 12, normalWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.DIAMOND_HORSE_ARMOR, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HORSE_ARMOR, 1, 1, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_INGOT, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.SHIELD, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.GLASS_BOTTLE, 2, 8, rareWeight));
        rareList.add(generateEntry(Material.GOLDEN_APPLE, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.GOLDEN_CARROT, 3, 9, rareWeight));
        rareList.add(generateEntry(Material.SPYGLASS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.MUSHROOM_STEW, 3, 9, normalWeight));
        rareList.add(generateEntry(Material.BUNDLE, 1, 1, rareWeight));
        epicList.add(generateEntry(Material.BLAZE_POWDER, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.GOLD_BLOCK, 2, 4, extraRareWeight));
        epicList.add(generateEntry(Material.NETHERITE_AXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_BOOTS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_CHESTPLATE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_HELMET, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_HOE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_LEGGINGS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_PICKAXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_SHOVEL, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_SWORD, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_AXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_BOOTS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_CHESTPLATE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_HELMET, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_HOE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_LEGGINGS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_INGOT, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.NETHERITE_PICKAXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_SHOVEL, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.NETHERITE_SWORD, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.POISONOUS_POTATO, 32, 64, extraRareWeight));
        epicList.add(generateEntry(Material.NETHER_STAR, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BLOCK, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.SPECTRAL_ARROW, 8, 16, normalWeight, true));
        epicList.add(generateEntry(Material.SLIME_BALL, 16, 32, normalWeight, true));
        epicList.add(generateEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 6, extraRareWeight));
        epicList.add(generateEntry(Material.PUMPKIN_PIE, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.TOTEM_OF_UNDYING, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight, true));

        commonItems.put("weight", 60);
        commonItems.put("items", commonList);
        rareItems.put("weight", 30);
        rareItems.put("items", rareList);
        epicItems.put("weight", 10);
        epicItems.put("items", epicList);
        items.put("common", commonItems);
        items.put("rare", rareItems);
        items.put("epic", epicItems);
        return items;
    }

    public static Map<String, Object> endContents() {
        Map<String, Object> items = new HashMap<>();
        Map<String, Object> commonItems = new HashMap<>();
        Map<String, Object> rareItems = new HashMap<>();
        Map<String, Object> epicItems = new HashMap<>();
        List<Map<String, Object>> commonList = new ArrayList<>();
        List<Map<String, Object>> rareList = new ArrayList<>();
        List<Map<String, Object>> epicList = new ArrayList<>();

        commonList.add(generateEntry(Material.CHORUS_FRUIT, 1, 16, rareWeight));
        commonList.add(generateEntry(Material.SOUL_LANTERN, 1, 3, extraRareWeight));
        commonList.add(generateEntry(Material.CRYING_OBSIDIAN, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.ARROW, 16, 32, normalWeight));
        commonList.add(generateEntry(Material.BREAD, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.CHAIN, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.CHEST, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.CLAY_BALL, 1, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_BEEF, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COOKED_PORKCHOP, 8, 16, normalWeight));
        commonList.add(generateEntry(Material.COMPASS, 1, 4, normalWeight));
        commonList.add(generateEntry(Material.EMERALD, 1, 6, normalWeight));
        commonList.add(generateEntry(Material.EXPERIENCE_BOTTLE, 1, 2, normalWeight));
        commonList.add(generateEntry(Material.FLINT, 1, 8, normalWeight));
        commonList.add(generateEntry(Material.FLINT_AND_STEEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.GLISTERING_MELON_SLICE, 1, 4, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_BOOTS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.LEATHER_HELMET, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_HORSE_ARMOR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.LEATHER_LEGGINGS, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.LEATHER_CHESTPLATE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.MUSIC_DISC_11, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_13, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_BLOCKS, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CAT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_CHIRP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_FAR, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MALL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_MELLOHI, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_PIGSTEP, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STAL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_STRAD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WAIT, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.MUSIC_DISC_WARD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight));
        commonList.add(generateEntry(Material.STONE_AXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_HOE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_PICKAXE, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SHOVEL, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.STONE_SWORD, 1, 1, extraRareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.WOODEN_AXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_HOE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_PICKAXE, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SHOVEL, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.WOODEN_SWORD, 1, 1, rareWeight, true));
        commonList.add(generateEntry(Material.BOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight));
        commonList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.DIAMOND, 2, 10, rareWeight));
        rareList.add(generateEntry(Material.ENDER_CHEST, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.TURTLE_HELMET, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.CROSSBOW, 1, 1, normalWeight, true));
        rareList.add(generateEntry(Material.BONE_MEAL, 2, 12, normalWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.CHAINMAIL_BOOTS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_CHESTPLATE, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_HELMET, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.CHAINMAIL_LEGGINGS, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.DIAMOND_HORSE_ARMOR, 1, 1, rareWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.IRON_AXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_BOOTS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_CHESTPLATE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HELMET, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HOE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_HORSE_ARMOR, 1, 1, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_INGOT, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.IRON_LEGGINGS, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_PICKAXE, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SHOVEL, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.IRON_SWORD, 1, 1, extraRareWeight, true));
        rareList.add(generateEntry(Material.SHIELD, 1, 1, rareWeight, true));
        rareList.add(generateEntry(Material.GLASS_BOTTLE, 2, 8, rareWeight));
        rareList.add(generateEntry(Material.GOLDEN_APPLE, 3, 9, extraRareWeight));
        rareList.add(generateEntry(Material.GOLDEN_CARROT, 3, 9, rareWeight));
        rareList.add(generateEntry(Material.SPYGLASS, 1, 1, normalWeight));
        rareList.add(generateEntry(Material.MUSHROOM_STEW, 3, 9, normalWeight));
        rareList.add(generateEntry(Material.BUNDLE, 1, 1, rareWeight));
        epicList.add(generateEntry(Material.ELYTRA, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.ENDER_PEARL, 2, 4, rareWeight));
        epicList.add(generateEntry(Material.GRAY_SHULKER_BOX, 1, 1, rareWeight));
        epicList.add(generateEntry(Material.ENDER_EYE, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight));
        epicList.add(generateEntry(Material.DIAMOND_BLOCK, 1, 2, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND, 2, 10, extraRareWeight));
        epicList.add(generateEntry(Material.DIAMOND_AXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_BOOTS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HELMET, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_HOE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_LEGGINGS, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SWORD, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_PICKAXE, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.DIAMOND_SHOVEL, 1, 1, normalWeight, true));
        epicList.add(generateEntry(Material.SPECTRAL_ARROW, 8, 16, normalWeight, true));
        epicList.add(generateEntry(Material.SLIME_BALL, 16, 32, normalWeight, true));
        epicList.add(generateEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 6, extraRareWeight));
        epicList.add(generateEntry(Material.PUMPKIN_PIE, 3, 9, normalWeight));
        epicList.add(generateEntry(Material.TOTEM_OF_UNDYING, 1, 1, extraRareWeight));
        epicList.add(generateEntry(Material.TRIDENT, 1, 1, normalWeight, true));

        commonItems.put("weight", 60);
        commonItems.put("items", commonList);
        rareItems.put("weight", 30);
        rareItems.put("items", rareList);
        epicItems.put("weight", 10);
        epicItems.put("items", epicList);
        items.put("common", commonItems);
        items.put("rare", rareItems);
        items.put("epic", epicItems);
        return items;
    }


    public static Map<String, Object> generateEntry(Material material, int minAmount, int maxAmount, double weight) {
        return generateEntry(material, minAmount, maxAmount, weight, false);
    }

    public static Map<String, Object> generateEntry(Material material, int minAmount, int maxAmount, double weight, boolean procedurallyGeneratedEnchantments) {
        HashMap<String, Object> entry = new HashMap<>();
        entry.put("material", material.toString());
        entry.put("amount", minAmount + "-" + maxAmount);
        entry.put("weight", weight);
        if (procedurallyGeneratedEnchantments)
            entry.put("procedurallyGenerateEnchantments", true);
        return entry;
    }

    public static Map<String, Object> generateProcedurallyGeneratedItems() {
        /*This is the top level.
        procedurallyGeneratedItems: <- you are here
          WOODEN_SWORD:
            SHARPNESS:
	          minLevel: 1
	          maxLevel: 5
	          chance: .1
         */
        Map<String, Object> procedurallyGeneratedEnchantments = new HashMap<>();
        List<Material> enchantableItems = List.of(
                Material.FISHING_ROD,
                Material.SHIELD,
                Material.TRIDENT,
                Material.CROSSBOW,
                Material.BOW,
                Material.TURTLE_HELMET,
                Material.WOODEN_AXE,
                Material.WOODEN_PICKAXE,
                Material.WOODEN_HOE,
                Material.WOODEN_SHOVEL,
                Material.WOODEN_SWORD,
                Material.STONE_AXE,
                Material.STONE_PICKAXE,
                Material.STONE_HOE,
                Material.STONE_SHOVEL,
                Material.STONE_SWORD,
                Material.GOLDEN_AXE,
                Material.GOLDEN_PICKAXE,
                Material.GOLDEN_HOE,
                Material.GOLDEN_SHOVEL,
                Material.GOLDEN_SWORD,
                Material.GOLDEN_HELMET,
                Material.GOLDEN_CHESTPLATE,
                Material.GOLDEN_LEGGINGS,
                Material.GOLDEN_BOOTS,
                Material.CHAINMAIL_HELMET,
                Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS,
                Material.CHAINMAIL_BOOTS,
                Material.LEATHER_HELMET,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS,
                Material.LEATHER_BOOTS,
                Material.IRON_AXE,
                Material.IRON_PICKAXE,
                Material.IRON_HOE,
                Material.IRON_SHOVEL,
                Material.IRON_SWORD,
                Material.IRON_HELMET,
                Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS,
                Material.IRON_BOOTS,
                Material.DIAMOND_AXE,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_HOE,
                Material.DIAMOND_SHOVEL,
                Material.DIAMOND_SWORD,
                Material.DIAMOND_HELMET,
                Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS,
                Material.DIAMOND_BOOTS,
                Material.NETHERITE_AXE,
                Material.NETHERITE_PICKAXE,
                Material.NETHERITE_HOE,
                Material.NETHERITE_SHOVEL,
                Material.NETHERITE_SWORD,
                Material.NETHERITE_HELMET,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS,
                Material.NETHERITE_BOOTS);

        for (Material enchantableItem : enchantableItems) {
            Map<String, Map<String, Object>> enchantmentMap = new HashMap<>();
            for (Enchantment enchantment : Enchantment.values()) {
                if (!enchantment.canEnchantItem(new ItemStack(enchantableItem))) continue;
                Map<String, Object> enchantmentSettingsMap = new HashMap<>();
                int minLevel = enchantment.getStartLevel();
                int maxLevel = enchantment.getMaxLevel();
                double chance = 0.2;
                enchantmentSettingsMap.put("minLevel", minLevel);
                enchantmentSettingsMap.put("maxLevel", maxLevel);
                enchantmentSettingsMap.put("chance", chance);
                enchantmentMap.put(enchantment.getKey().getKey(), enchantmentSettingsMap);
            }
            procedurallyGeneratedEnchantments.put(enchantableItem.getKey().getKey(), enchantmentMap);
        }
        return procedurallyGeneratedEnchantments;
    }

}
