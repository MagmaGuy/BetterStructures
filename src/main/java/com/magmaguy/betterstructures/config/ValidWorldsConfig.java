package com.magmaguy.betterstructures.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ValidWorldsConfig extends ConfigurationFile {
    @Getter
    private static HashMap<World, Boolean> validWorlds = new HashMap<>();
    @Getter
    private static boolean whitelistNewWorlds;
    private static ValidWorldsConfig instance;

    public ValidWorldsConfig() {
        super("ValidWorlds.yml");
        instance = this;
    }

    public static void registerNewWorld(World world) {
        if (instance.fileConfiguration.getKeys(true).contains("Valid worlds." + world.getName())) {
            validWorlds.put(world, instance.fileConfiguration.getBoolean("Valid worlds." + world.getName()));
            return;
        }

        ConfigurationEngine.setBoolean(instance.fileConfiguration, "Valid worlds." + world.getName(), whitelistNewWorlds);
        ConfigurationEngine.fileSaverOnlyDefaults(instance.fileConfiguration, instance.file);
        validWorlds.put(world, whitelistNewWorlds);
    }

    public static boolean isValidWorld(World world) {
        if (validWorlds.get(world) != null)
            return validWorlds.get(world);
        return false;
    }

    @Override
    public void initializeValues() {
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

    public static class ValidWorldsConfigEvents implements Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onWorldLoad(WorldLoadEvent event) {
            registerNewWorld(event.getWorld());
        }
    }
}
