package com.magmaguy.betterstructures.content;

import com.magmaguy.betterstructures.BetterStructures;
import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.nightbreak.NightbreakContentRefresher;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public class BSPackageRefresher {
    private static final Duration REFRESH_COOLDOWN = Duration.ofMinutes(5);
    private static final String CATALOG_KEY = "nightbreak-packages";

    private BSPackageRefresher() {
    }

    public static void refreshContentAndAccess() {
        NightbreakContentRefresher.refreshAsyncIfDue(
                (JavaPlugin) MetadataHandler.PLUGIN,
                CATALOG_KEY,
                REFRESH_COOLDOWN,
                BetterStructures::availablePackages,
                bspPackage -> true,
                outdated -> {
                });
    }

    public static void reset() {
        NightbreakContentRefresher.resetRefreshCooldown(
                (JavaPlugin) MetadataHandler.PLUGIN, CATALOG_KEY);
    }
}
