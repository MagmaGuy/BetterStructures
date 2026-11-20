package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.freeminecraftmodels.customentity.PropEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/** FreeMinecraftModels calls; touch {@link #spawnProp} only after {@link #isEnabled()} returned true. */
public final class FreeMinecraftModels {

    private FreeMinecraftModels() {
    }

    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels");
    }

    /** Places the model as a persistent static prop. False when FreeMinecraftModels refused it (unknown model, duplicate). */
    public static boolean spawnProp(String modelId, Location location) {
        return PropEntity.spawnPropEntity(modelId, location) != null;
    }
}
