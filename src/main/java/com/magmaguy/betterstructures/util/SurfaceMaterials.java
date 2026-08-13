package com.magmaguy.betterstructures.util;

import com.magmaguy.magmacore.util.VersionChecker;
import org.bukkit.Material;

import java.util.EnumSet;

public class SurfaceMaterials {

    //Built once: the previous per-call string switch re-ran the version check and NamespacedKey
    //lookup for every block on worldgen hot paths. Names that don't exist on the running server
    //resolve to null and are skipped, which matches the old switch exactly: an absent name can
    //never be the name of an input Material.
    private static final EnumSet<Material> IGNORABLE = EnumSet.noneOf(Material.class);

    static {
        String[] ignorableNames = {
                "ACACIA_LOG", "BIRCH_LOG", "DARK_OAK_LOG", "JUNGLE_LOG", "MANGROVE_LOG", "OAK_LOG", "SPRUCE_LOG",
                "STRIPPED_ACACIA_LOG", "STRIPPED_BIRCH_LOG", "STRIPPED_DARK_OAK_LOG", "STRIPPED_JUNGLE_LOG",
                "STRIPPED_MANGROVE_LOG", "STRIPPED_OAK_LOG", "STRIPPED_SPRUCE_LOG", "ACACIA_WOOD", "BIRCH_WOOD", "JUNGLE_WOOD",
                "DARK_OAK_WOOD", "OAK_WOOD", "MANGROVE_WOOD", "SPRUCE_WOOD", "STRIPPED_ACACIA_WOOD", "STRIPPED_BIRCH_WOOD",
                "STRIPPED_DARK_OAK_WOOD", "STRIPPED_JUNGLE_WOOD", "STRIPPED_MANGROVE_WOOD", "STRIPPED_OAK_WOOD",
                "STRIPPED_SPRUCE_WOOD", "MUSHROOM_STEM", "BROWN_MUSHROOM_BLOCK", "RED_MUSHROOM_BLOCK", "SUGAR_CANE", "BAMBOO",
                "TALL_GRASS", "WEEPING_VINES", "VINE", "TWISTING_VINES", "CAVE_VINES_PLANT", "CAVE_VINES", "WEEPING_VINES_PLANT",
                "TWISTING_VINES_PLANT", "FLOWERING_AZALEA", "CHORUS_FLOWER", "CORNFLOWER", "FLOWERING_AZALEA_LEAVES", "SUNFLOWER",
                "ACACIA_LEAVES", "AZALEA_LEAVES", "BIRCH_LEAVES", "DARK_OAK_LEAVES", "JUNGLE_LEAVES", "MANGROVE_LEAVES",
                "OAK_LEAVES", "SPRUCE_LEAVES", "DEAD_BUSH", "SWEET_BERRY_BUSH", "ROSE_BUSH", "POPPY", "DANDELION", "BLUE_ORCHID",
                "ALLIUM", "AZURE_BLUET", "ORANGE_TULIP", "PINK_TULIP", "RED_TULIP", "WHITE_TULIP", "OXEYE_DAISY", "LILY_OF_THE_VALLEY",
                "WITHER_ROSE", "PEONY", "COCOA", "COCOA_BEANS", "BAMBOO_SAPLING", "SPRUCE_SAPLING", "ACACIA_SAPLING", "BIRCH_SAPLING",
                "DARK_OAK_SAPLING", "JUNGLE_SAPLING", "OAK_SAPLING", "MELON", "POTATOES", "CARROTS", "BEETROOTS", "WHEAT", "AIR",
                "CAVE_AIR", "VOID_AIR", "ACACIA_FENCE", "ACACIA_FENCE_GATE", "BIRCH_FENCE_GATE", "BIRCH_FENCE", "CRIMSON_FENCE",
                "CRIMSON_FENCE_GATE", "DARK_OAK_FENCE", "DARK_OAK_FENCE_GATE", "JUNGLE_FENCE", "JUNGLE_FENCE_GATE",
                "MANGROVE_FENCE", "NETHER_BRICK_FENCE", "OAK_FENCE", "MANGROVE_FENCE_GATE", "OAK_FENCE_GATE", "SPRUCE_FENCE",
                "SPRUCE_FENCE_GATE", "WARPED_FENCE", "WARPED_FENCE_GATE", "FERN", "LILAC", "SNOW_BLOCK", "POWDER_SNOW", "SNOW",
                "CHORUS_PLANT", "CRIMSON_STEM", "STRIPPED_CRIMSON_STEM", "STRIPPED_CRIMSON_HYPHAE", "NETHER_WART_BLOCK",
                "NETHER_WART", "CRIMSON_ROOTS", "CRIMSON_FUNGUS", "SHROOMLIGHT", "FIRE", "WARPED_STEM", "WARPED_FUNGUS",
                "WARPED_HYPHAE", "WARPED_NYLIUM", "WARPED_ROOTS", "STRIPPED_WARPED_HYPHAE", "STRIPPED_WARPED_STEM",
                "NETHER_SPROUTS", "BONE_BLOCK", "LARGE_FERN"
        };
        for (String name : ignorableNames) {
            Material material = Material.getMaterial(name);
            if (material != null) IGNORABLE.add(material);
        }
        //Version-dependent entries, resolved once here exactly as the old default arm resolved them
        //per call: SHORT_GRASS only counts on 1.21+, and older servers expose the same plant as the
        //GRASS constant (key "grass").
        if (!VersionChecker.serverVersionOlderThan(21, 0)) {
            Material shortGrass = Material.getMaterial("SHORT_GRASS");
            if (shortGrass != null) IGNORABLE.add(shortGrass);
        }
        Material legacyGrass = Material.getMaterial("GRASS");
        if (legacyGrass != null) IGNORABLE.add(legacyGrass);
    }

    private SurfaceMaterials() {
    }

    public static boolean ignorable(Material material) {
        return IGNORABLE.contains(material);
    }
}
