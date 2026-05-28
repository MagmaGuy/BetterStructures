package com.magmaguy.betterstructures;

import com.magmaguy.betterstructures.commands.*;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.ValidWorldsConfig;
import com.magmaguy.betterstructures.config.components.ComponentsConfigFolder;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modules.ModulesConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.betterstructures.listeners.FirstTimeSetupWarner;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.modules.WFCGenerator;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.initialization.PluginInitializationConfig;
import com.magmaguy.magmacore.initialization.PluginInitializationContext;
import com.magmaguy.magmacore.initialization.PluginInitializationState;
import com.magmaguy.magmacore.util.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
        try {
            this.getConfig().save("config.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MagmaCore.onEnable(this);
        MagmaCore.exportSharedAssets(this);
        MagmaCore.startInitialization(this,
                new PluginInitializationConfig("BetterStructures", "betterstructures.*", 16),
                this::asyncInitialization,
                this::syncInitialization,
                () -> {
                    Logger.info("BetterStructures fully initialized!");
                    if (MetadataHandler.pendingReloadSender != null) {
                        Logger.sendMessage(MetadataHandler.pendingReloadSender, "Reloaded BetterStructures.");
                        MetadataHandler.pendingReloadSender = null;
                    }
                },
                throwable -> {
                    MetadataHandler.pendingReloadSender = null;
                    throwable.printStackTrace();
                });
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
        MagmaCore.requestInitializationShutdown(this);
        if (MagmaCore.getInitializationState(this.getName()) == PluginInitializationState.INITIALIZING) {
            Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
            MagmaCore.shutdown(this);
            Bukkit.getLogger().info("[BetterStructures] Shutdown during initialization.");
            return;
        }
        // Plugin shutdown logic
        SchematicContainer.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        MagmaCore.shutdown(this);
        HandlerList.unregisterAll(MetadataHandler.PLUGIN);
        BSPackage.shutdown();
        ModulesContainer.shutdown();
        WFCGenerator.shutdown();
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }

    private void asyncInitialization(PluginInitializationContext initializationContext) {
        initializationContext.step("Base Configs");
        new DefaultConfig();
        new ValidWorldsConfig();

        initializationContext.step("Content Importer");
        MagmaCore.initializeImporter(this);

        initializationContext.step("Treasure Config");
        new TreasureConfig();
        initializationContext.step("Generator Config");
        new GeneratorConfig();
        initializationContext.step("Module Generators");
        new ModuleGeneratorsConfig();
        initializationContext.step("Spawn Pools");
        new SpawnPoolsConfig();
        initializationContext.step("Schematics");
        new SchematicConfig();
        initializationContext.step("Modules");
        new ModulesConfig();
        initializationContext.step("Content Packages");
        new ContentPackageConfig();
    }

    private void syncInitialization(PluginInitializationContext initializationContext) {
        initializationContext.step("NMS Adapter");
        NMSManager.initializeAdapter(this);

        initializationContext.step("Components Folder");
        ComponentsConfigFolder.initialize();

        initializationContext.step("Event Listeners");
        Bukkit.getPluginManager().registerEvents(new NewChunkLoadEvent(), this);
        Bukkit.getPluginManager().registerEvents(new FirstTimeSetupWarner(), this);
        Bukkit.getPluginManager().registerEvents(new ValidWorldsConfig.ValidWorldsConfigEvents(), this);

        initializationContext.step("Commands");
        CommandManager commandManager = new CommandManager(this, "betterstructures");
        commandManager.registerCommand(new LootifyCommand());
        commandManager.registerCommand(new PlaceCommand());
        commandManager.registerCommand(new PregenerateCommand());
        commandManager.registerCommand(new CancelPregenerateCommand());
        commandManager.registerCommand(new ReloadCommand());
        commandManager.registerCommand(new SilentCommand());
        commandManager.registerCommand(new TeleportCommand());
        commandManager.registerCommand(new VersionCommand());
        commandManager.registerCommand(new SetupCommand());
        commandManager.registerCommand(new FirstTimeSetupCommand());
        commandManager.registerCommand(new DownloadAllContentCommand());
        commandManager.registerCommand(new UpdateContentCommand());
        commandManager.registerCommand(new GenerateModulesCommand());
        commandManager.registerCommand(new BetterStructuresCommand());

        initializationContext.step("Version Check");
        MagmaCore.checkVersionUpdate("103241", "https://nightbreak.io/plugin/betterstructures/");

        initializationContext.step("WorldGuard Integration");
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            Bukkit.getPluginManager().registerEvents(new WorldGuard(), this);
        }

        initializationContext.step("Metrics");
        new Metrics(this, 19523);
    }

    public void reloadImportedContent(CommandSender commandSender) {
        SchematicContainer.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        BSPackage.shutdown();
        ModulesContainer.shutdown();
        WFCGenerator.shutdown();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                MagmaCore.initializeImporter(this);
                new TreasureConfig();
                new GeneratorConfig();
                new ModuleGeneratorsConfig();
                new SpawnPoolsConfig();
                new SchematicConfig();
                new ModulesConfig();
                new ContentPackageConfig();
                ComponentsConfigFolder.initialize();

                Bukkit.getScheduler().runTask(this, () -> {
                    if (commandSender != null) {
                        Logger.sendMessage(commandSender, "Reloaded BetterStructures content.");
                    }
                });
            } catch (Exception exception) {
                Logger.warn("Failed to reload BetterStructures content asynchronously.");
                exception.printStackTrace();
                Bukkit.getScheduler().runTask(this, () -> {
                    if (commandSender != null) {
                        Logger.sendMessage(commandSender, "&cFailed to reload BetterStructures content. Check the console.");
                    }
                });
            }
        });
    }
}
