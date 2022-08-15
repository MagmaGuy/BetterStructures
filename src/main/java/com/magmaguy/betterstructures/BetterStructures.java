package com.magmaguy.betterstructures;

import com.magmaguy.betterstructures.commands.CommandHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.VersionChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class BetterStructures extends JavaPlugin {

    @Override
    public void onEnable() {
        MetadataHandler.PLUGIN = this;
        // Plugin startup logic
        Bukkit.getLogger().info("[BetterStructures] Initialized!");
        Bukkit.getPluginManager().registerEvents(new NewChunkLoadEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ValidWorldsConfig.ValidWorldsConfigEvents(), this);
        Bukkit.getPluginManager().registerEvents(new VersionChecker.VersionCheckerEvents(), this);
        try {
            this.getConfig().save("config.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DefaultConfig.initializeConfig();
        ValidWorldsConfig.initializeConfig();
        new TreasureConfig();
        new GeneratorConfig();
        new SchematicConfig();
        new CommandHandler();
        VersionChecker.checkPluginVersion();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SchematicContainer.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }
}
