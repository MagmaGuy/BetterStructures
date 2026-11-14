package com.magmaguy.betterstructures.thirdparty;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Support for MMOItems treasure entries, configuration format is "TYPE@ID".
 * Callers must check {@link #isEnabled()} first: this class is the only place that touches the MMOItems API,
 * so servers without MMOItems never load its classes.
 */
public class MMOItems {

    private MMOItems() {
    }

    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    /**
     * @return the built item, or null when the type or id does not match an MMOItems item
     */
    public static ItemStack resolve(String entry) {
        String[] args = entry.split("@");
        net.Indyuce.mmoitems.MMOItems mmo = net.Indyuce.mmoitems.MMOItems.plugin;
        MMOItem mmoitem = mmo.getMMOItem(mmo.getTypes().get(args[0]), args[1]);
        if (mmoitem == null) return null;
        return mmoitem.newBuilder().build();
    }
}
