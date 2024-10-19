package com.magmaguy.betterstructures;

import com.magmaguy.betterstructures.commands.*;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.modules.ModulesConfig;
import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.betterstructures.listeners.FirstTimeSetupWarner;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.VersionChecker;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.util.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class BetterStructures extends JavaPlugin {

    @Override
    public void onEnable() {
        MetadataHandler.PLUGIN = this;
        Bukkit.getLogger().info("    ____       __  __            _____ __                  __                      ");
        Bukkit.getLogger().info("   / __ )___  / /_/ /____  _____/ ___// /________  _______/ /___  __________  _____");
        Bukkit.getLogger().info("  / __  / _ \\/ __/ __/ _ \\/ ___/\\__ \\/ __/ ___/ / / / ___/ __/ / / / ___/ _ \\/ ___/");
        Bukkit.getLogger().info(" / /_/ /  __/ /_/ /_/  __/ /   ___/ / /_/ /  / /_/ / /__/ /_/ /_/ / /  /  __(__  ) ");
        Bukkit.getLogger().info("/_____/\\___/\\__/\\__/\\___/_/   /____/\\__/_/   \\__,_/\\___/\\__/\\__,_/_/   \\___/____/");
        // Plugin startup logic
        Bukkit.getLogger().info("[BetterStructures] Initialized version " + this.getDescription().getVersion() + "!");
        Bukkit.getPluginManager().registerEvents(new NewChunkLoadEvent(), this);
        Bukkit.getPluginManager().registerEvents(new FirstTimeSetupWarner(), this);
        Bukkit.getPluginManager().registerEvents(new ValidWorldsConfig.ValidWorldsConfigEvents(), this);
        Bukkit.getPluginManager().registerEvents(new VersionChecker.VersionCheckerEvents(), this);
        try {
            this.getConfig().save("config.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new DefaultConfig();
        new ValidWorldsConfig();
        //Creates import folder if one doesn't exist, imports any content inside
        MagmaCore.initializeImporter();
        MagmaCore.onEnable();

        new TreasureConfig();
        new GeneratorConfig();
        new SchematicConfig();
        new ModulesConfig();
        new ContentPackageConfig();
        ModulesContainer.initializeSpecialModules();
        CommandManager commandManager = new CommandManager(this, "betterstructures");
        commandManager.registerCommand(new LootifyCommand());
        commandManager.registerCommand(new PlaceCommand());
        commandManager.registerCommand(new ReloadCommand());
        commandManager.registerCommand(new SilentCommand());
        commandManager.registerCommand(new TeleportCommand());
        commandManager.registerCommand(new VersionCommand());
        commandManager.registerCommand(new SetupCommand());
        commandManager.registerCommand(new FirstTimeSetupCommand());
        commandManager.registerCommand(new GenerateModulesInstantlyCommand());
        commandManager.registerCommand(new GenerateModulesSlowlyCommand());

        VersionChecker.checkPluginVersion();
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                Bukkit.getPluginManager().getPlugin("EliteMobs") != null)
            Bukkit.getPluginManager().registerEvents(new WorldGuard(), this);
        new Metrics(this, 19523);
    }

    @Override
    public void onLoad() {
        MagmaCore.createInstance(this);
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                    Bukkit.getPluginManager().getPlugin("EliteMobs") != null)
                WorldGuard.initializeFlag();
            else
                Logger.info("WorldGuard is not enabled! WorldGuard is recommended when using the EliteMobs integration.");
        } catch (Exception ex) {
            Logger.info("WorldGuard could not be detected! Some BetterStructures features use WorldGuard, and they will not work until it is installed.");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SchematicContainer.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        MagmaCore.shutdown();
        HandlerList.unregisterAll(MetadataHandler.PLUGIN);
        BSPackage.shutdown();
        ModulesContainer.shutdown();
        WaveFunctionCollapseGenerator.shutdown();
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }
}
