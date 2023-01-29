package com.magmaguy.betterstructures.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ValidWorldsConfig {
    @Getter
    private static HashMap<World, Boolean> validWorlds = new HashMap<>();
    @Getter
    private static FileConfiguration fileConfiguration;
    @Getter
    private static boolean whitelistNewWorlds;
    private static File file;

    private ValidWorldsConfig() {
    }

    public static void initializeConfig() {
        file = ConfigurationEngine.fileCreator("ValidWorlds.yml");
        fileConfiguration = ConfigurationEngine.fileConfigurationCreator(file);

        whitelistNewWorlds = ConfigurationEngine.setBoolean(fileConfiguration, "New worlds spawn structures", true);

        for (World world : Bukkit.getWorlds())
            ConfigurationEngine.setBoolean(fileConfiguration, "Valid worlds." + world.getName(), true);

        ConfigurationSection validWorldsSection = fileConfiguration.getConfigurationSection("Valid worlds");

        List<String> enabledWorlds = new ArrayList<>();

        for (String key : validWorldsSection.getKeys(false))
            if (validWorldsSection.getBoolean(key))
                enabledWorlds.add(key);

        for (World world : Bukkit.getWorlds())
            validWorlds.put(world, enabledWorlds.contains(world.getName()));

        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
    }

    public static void registerNewWorld(World world) {
        if (fileConfiguration.getKeys(true).contains("Valid worlds." + world.getName())) {
            validWorlds.put(world, fileConfiguration.getBoolean("Valid worlds." + world.getName()));
            return;
        }

        ConfigurationEngine.setBoolean(fileConfiguration, "Valid worlds." + world.getName(), whitelistNewWorlds);
        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
        validWorlds.put(world, whitelistNewWorlds);
    }

    public static boolean isValidWorld(World world) {
        if (validWorlds.get(world) != null)
            return validWorlds.get(world);
        return false;
    }

    public static class ValidWorldsConfigEvents implements Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onWorldLoad(WorldLoadEvent event) {
            registerNewWorld(event.getWorld());
        }
    }
}
