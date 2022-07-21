package com.magmaguy.betterstructures.util;

import org.bukkit.Bukkit;

public class InfoMessage {
    public InfoMessage(String message) {
        Bukkit.getLogger().info("[EliteMobs] " + message);
    }
}
