package com.magmaguy.betterstructures.util;

import com.magmaguy.betterstructures.MetadataHandler;
import org.bukkit.Bukkit;

public class WarningMessage {
    public WarningMessage(String message) {
        Bukkit.getLogger().warning("[BetterStructures] " + message);
    }

    public WarningMessage(String message, boolean stackTrace) {
        Bukkit.getLogger().warning("[BetterStructures] " + message);
        if (stackTrace) {
            Bukkit.getLogger().info("BetterStructures version: " + MetadataHandler.PLUGIN.getDescription().getVersion() + " | Server version: " + Bukkit.getServer().getVersion());
            for (StackTraceElement element : Thread.currentThread().getStackTrace())
                Bukkit.getLogger().info(element.toString());
        }
    }
}
