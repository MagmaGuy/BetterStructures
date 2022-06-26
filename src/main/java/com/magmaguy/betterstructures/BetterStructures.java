package com.magmaguy.betterstructures;

import com.magmaguy.betterstructures.commands.CommandHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
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
        try {
            this.getConfig().save("config.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DefaultConfig.initializeConfig();
        ValidWorldsConfig.initializeConfig();
        new GeneratorConfig();
        new SchematicConfig();
        new CommandHandler();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }
}
