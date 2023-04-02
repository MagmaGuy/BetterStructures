package com.magmaguy.betterstructures.chests;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.betterstructures.util.WarningMessage;
import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestContents {

    @Getter
    private List<ChestEntry> chestEntries = new ArrayList<>();

    /*
    Entry format: - material=MATERIAL:amount=AMOUNT_MIN-AMOUNT_MAX:chance=CHANCE
     */
    public ChestContents(TreasureConfigFields treasureConfigFields) {
        if (treasureConfigFields.getRawLoot() == null) return;
        if (!processEntries(treasureConfigFields.getRawLoot()))
            new WarningMessage("Treasure file " + treasureConfigFields.getFilename() + " has invalid treasure entries!");
    }

    private boolean processEntries(List<String> rawChestEntries) {
        boolean noProblems = true;
        for (String string : rawChestEntries) {
            String[] sections = string.split(":");
            Material material = null;
            ItemStack itemStack = null;
            int amountMin = 1;
            int amountMax = 1;
            double chance = 1;
            for (String section : sections) {
                String[] subsection = section.split("=");
                switch (subsection[0].toLowerCase()) {
                    case "material":
                        try {
                            material = Material.getMaterial(subsection[1]);
                        } catch (Exception exception) {
                            new WarningMessage("Invalid material detected! Problematic entry: " + subsection[0]);
                            noProblems = false;
                        }
                        break;
                    case "serialized":
                        try {
                            itemStack = ItemStackSerialization.itemStackArrayFromBase64(section.replace("serialized=", ""));
                        } catch (Exception ex) {
                            new WarningMessage("Invalid serialized value detected! Problematic entry: " + subsection[0]);
                            ex.printStackTrace();
                            noProblems = false;
                        }
                        break;
                    case "mmoitem":  // carm start - Support for MMOItems
                    case "mmoitems":
                        if (Bukkit.getPluginManager().getPlugin("MMOItems") == null) {
                            new WarningMessage("MMOItems plugin not found! Problematic entry: " + subsection[0]);
                            noProblems = false;
                            break;
                        }
                        try {
                            String[] args = subsection[1].split("@");
                            MMOItems mmo = MMOItems.plugin;
                            MMOItem mmoitem = mmo.getMMOItem(mmo.getTypes().get(args[0]), args[1]);
                            if (mmoitem == null) throw new NullPointerException("mmo item is null");
                            itemStack = mmoitem.newBuilder().build();
                        } catch (Exception ex) {
                            new WarningMessage("Invalid mmo item detected! Problematic entry: " + subsection[0]);
                            noProblems = false;
                        }
                        // carm end - Support for MMOItems
                    case "amount":
                        try {
                            if (subsection[1].contains("-")) {
                                String[] amounts = subsection[1].split("-");
                                amountMin = Integer.parseInt(amounts[0]);
                                amountMax = Integer.parseInt(amounts[1]);
                            } else {
                                amountMin = Integer.parseInt(subsection[1]);
                                amountMax = amountMin;
                            }
                        } catch (Exception exception) {
                            new WarningMessage("Invalid amount detected! Problematic entry: " + subsection[0]);
                            noProblems = false;
                        }
                        break;
                    case "chance":
                        try {
                            chance = Double.parseDouble(subsection[1]);
                        } catch (Exception exception) {
                            new WarningMessage("Invalid chance detected! Problematic entry: " + subsection[0]);
                            noProblems = false;
                        }
                        break;
                    case "info":
                        //This is purely visual, helps document serialized entries!
                        break;
                    default: {
                        new WarningMessage("Failed to parse chest entry! Problematic entry: " + subsection[0]);
                        noProblems = false;
                    }
                }
            }

            if (material != null || itemStack != null) {
                ChestEntry chestEntry = new ChestEntry(material, chance, amountMin, amountMax, itemStack);
                chestEntries.add(chestEntry);
            }
        }
        return noProblems;
    }

    public void rollChestContents(Container chest) {
        for (ChestEntry chestEntry : chestEntries) {
            ItemStack itemStack = chestEntry.rollEntry();
            if (itemStack == null) continue;
            chest.getSnapshotInventory().addItem(itemStack);
        }
    }
}
