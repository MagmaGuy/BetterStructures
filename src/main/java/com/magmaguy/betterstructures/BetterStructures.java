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
import com.magmaguy.betterstructures.content.BSPackageRefresher;
import com.magmaguy.betterstructures.listeners.FirstTimeSetupWarner;
import com.magmaguy.betterstructures.listeners.NewChunkLoadEvent;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.betterstructures.modules.WFCGenerator;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.ChunkPregenerator;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.dlc.ConfigurationImporter;
import com.magmaguy.magmacore.initialization.PluginInitializationConfig;
import com.magmaguy.magmacore.initialization.PluginInitializationContext;
import com.magmaguy.magmacore.initialization.PluginInitializationState;
import com.magmaguy.magmacore.nightbreak.NightbreakDownloadContentCommand;
import com.magmaguy.magmacore.nightbreak.NightbreakDownloadEverythingCommand;
import com.magmaguy.magmacore.nightbreak.NightbreakDownloadPluginUpdateCommand;
import com.magmaguy.magmacore.nightbreak.NightbreakForceReinstallContentCommand;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginSpec;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginUpdater;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginStateRegistry;
import com.magmaguy.magmacore.nightbreak.NightbreakRecommendedPluginsCommand;
import com.magmaguy.magmacore.util.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BetterStructures extends JavaPlugin {
    private volatile boolean contentReloadInProgress;
    private final List<CommandSender> activeReloadSenders = new ArrayList<>();
    private final List<CommandSender> queuedReloadSenders = new ArrayList<>();

    public static final NightbreakPluginSpec NIGHTBREAK_PLUGIN_SPEC = new NightbreakPluginSpec(
            "BetterStructures",
            "bs",
            "betterstructures.*",
            "betterstructures.setup",
            "betterstructures.initialize",
            "https://nightbreak.io/plugin/betterstructures/",
            "Reloaded BetterStructures.");

    @Override
    public void onEnable() {
        MetadataHandler.PLUGIN = this;
        SchematicConfig.prepareForEnable();
        Bukkit.getLogger().info("    ____       __  __            _____ __                  __                      ");
        Bukkit.getLogger().info("   / __ )___  / /_/ /____  _____/ ___// /________  _______/ /___  __________  _____");
        Bukkit.getLogger().info("  / __  / _ \\/ __/ __/ _ \\/ ___/\\__ \\/ __/ ___/ / / / ___/ __/ / / / ___/ _ \\/ ___/");
        Bukkit.getLogger().info(" / /_/ /  __/ /_/ /_/  __/ /   ___/ / /_/ /  / /_/ / /__/ /_/ /_/ / /  /  __(__  ) ");
        Bukkit.getLogger().info("/_____/\\___/\\__/\\__/\\___/_/   /____/\\__/_/   \\__,_/\\___/\\__/\\__,_/_/   \\___/____/");
        // Plugin startup logic
        Bukkit.getLogger().info("[BetterStructures] Initialized version " + this.getDescription().getVersion() + "!");
        MagmaCore.onEnable(this);
        MagmaCore.exportSharedAssets(this);
        SchematicContainer.updateOptionalPluginAvailability(
                Bukkit.getPluginManager().isPluginEnabled("EliteMobs"),
                Bukkit.getPluginManager().isPluginEnabled("MythicMobs"));
        MagmaCore.startInitialization(this,
                new PluginInitializationConfig("BetterStructures", "betterstructures.*", 17, resolveInitializationDependencies()),
                this::asyncInitialization,
                this::syncInitialization,
                () -> {
                    Logger.info("BetterStructures fully initialized!");
                    NightbreakPluginUpdater.autoDownloadPluginUpdateIfEnabled(this, NIGHTBREAK_PLUGIN_SPEC);
                    CommandSender pendingReloadSender = NightbreakPluginStateRegistry.consumePendingReloadSender(this);
                    if (pendingReloadSender == null) {
                        pendingReloadSender = MetadataHandler.pendingReloadSender;
                    }
                    if (pendingReloadSender != null) {
                        Logger.sendMessage(pendingReloadSender, NIGHTBREAK_PLUGIN_SPEC.reloadSuccessMessage());
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
        contentReloadInProgress = false;
        activeReloadSenders.clear();
        queuedReloadSenders.clear();
        boolean shutdownDuringInitialization =
                MagmaCore.getInitializationState(this.getName())
                        == PluginInitializationState.INITIALIZING;
        MagmaCore.requestInitializationShutdown(this);
        if (!SchematicConfig.shutdownLoading()) {
            Bukkit.getLogger().severe(
                    "[BetterStructures] Timed out waiting for schematic loading to stop; "
                            + "shutdown will continue after the bounded wait.");
        }
        Schematic.shutdown();
        SchematicClipboardCache.shutdown();
        SchematicContainer.shutdown();
        NewChunkLoadEvent.shutdown();
        ChunkPregenerator.shutdown();
        BSPackage.shutdown();
        ModulesContainer.shutdown();
        WFCGenerator.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        HandlerList.unregisterAll(MetadataHandler.PLUGIN);
        if (shutdownDuringInitialization) {
            MagmaCore.shutdown(this);
            Bukkit.getLogger().info("[BetterStructures] Shutdown during initialization.");
            return;
        }
        MagmaCore.shutdown(this);
        Bukkit.getLogger().info("[BetterStructures] Shutdown!");
    }

    /**
     * Decides whether initialization has to queue behind the plugins listed in plugin.yml softdepend.
     * <p>
     * By default MagmaCore holds a plugin's whole async phase until every present softdepend has
     * finished initializing. For BetterStructures that meant its schematic loading ran strictly
     * after EliteMobs finished, serialising two long, unrelated workloads back to back.
     * <p>
     * Nothing in the async phase actually needs EliteMobs to be *initialized* — schematic parsing
     * only needs to know whether EliteMobs is *installed*, which is already resolved in onEnable
     * via SchematicContainer.updateOptionalPluginAvailability. The one exception is the content
     * importer: when it deposits files into another plugin's data folder, that plugin has to be
     * told to reload, and reloading a plugin mid-initialization is not safe. So the wait is kept
     * only when there is actually something to import, which is rare (a fresh DLC zip in imports).
     *
     * @return null to use the plugin.yml softdepend list, or an empty list to wait for nobody
     */
    private List<String> resolveInitializationDependencies() {
        File importsFolder = new File(getDataFolder(), "imports");
        String[] pendingImports = importsFolder.list();
        boolean hasPendingImports = pendingImports != null && pendingImports.length > 0;
        if (hasPendingImports) {
            Logger.info("Content is pending import; waiting for other plugins before initializing.");
            return null;
        }
        return List.of();
    }

    private void asyncInitialization(PluginInitializationContext initializationContext) {
        initializationContext.step("Base Configs");
        new DefaultConfig();

        initializationContext.step("Content Importer");
        ConfigurationImporter importer = MagmaCore.initializeImporter(this);
        if (importer != null && importer.isEliteMobsContentImported())
            EliteMobs.reloadAfterContentImport();

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
        initializationContext.step("Valid Worlds Config");
        new ValidWorldsConfig();

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
        commandManager.registerCommand(new NightbreakRecommendedPluginsCommand(this, NIGHTBREAK_PLUGIN_SPEC));
        commandManager.registerCommand(new NightbreakDownloadPluginUpdateCommand(this, NIGHTBREAK_PLUGIN_SPEC));
        commandManager.registerCommand(new NightbreakDownloadEverythingCommand<>(this,
                NIGHTBREAK_PLUGIN_SPEC,
                BetterStructures::availablePackages,
                ReloadCommand::reload));
        commandManager.registerCommand(new NightbreakDownloadContentCommand<>(this,
                NIGHTBREAK_PLUGIN_SPEC,
                BetterStructures::availablePackages,
                ReloadCommand::reload,
                false));
        commandManager.registerCommand(new NightbreakDownloadContentCommand<>(this,
                NIGHTBREAK_PLUGIN_SPEC,
                BetterStructures::availablePackages,
                ReloadCommand::reload,
                true));
        commandManager.registerCommand(new NightbreakForceReinstallContentCommand<>(this,
                NIGHTBREAK_PLUGIN_SPEC,
                BetterStructures::availablePackages,
                ReloadCommand::reload));
        commandManager.registerCommand(new GenerateModulesCommand());
        commandManager.registerCommand(new BetterStructuresCommand());

        initializationContext.step("Version Check");
        MagmaCore.checkVersionUpdate("103241", "https://nightbreak.io/plugin/betterstructures/");

        initializationContext.step("WorldGuard Integration");
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard") &&
                Bukkit.getPluginManager().isPluginEnabled("EliteMobs")) {
            Bukkit.getPluginManager().registerEvents(new WorldGuard(), this);
        }

        initializationContext.step("Metrics");
        new Metrics(this, 19523);
    }

    public void reloadImportedContent(CommandSender commandSender) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, () -> reloadImportedContent(commandSender));
            return;
        }
        if (contentReloadInProgress) {
            if (commandSender != null) {
                queuedReloadSenders.add(commandSender);
                Logger.sendMessage(commandSender, "A BetterStructures content reload is already running. Your reload was queued.");
            }
            return;
        }

        activeReloadSenders.clear();
        if (commandSender != null) activeReloadSenders.add(commandSender);
        startImportedContentReload();
    }

    private void startImportedContentReload() {
        contentReloadInProgress = true;

        // `/betterstructures reload` is presented as the plugin reload command,
        // so runtime settings must not remain pinned to their startup values.
        // Reload these synchronously on the server thread before asynchronous
        // content parsing starts; ValidWorldsConfig also reads the live Bukkit
        // world list and therefore must not be constructed by the worker.
        new DefaultConfig();
        new ValidWorldsConfig();
        SchematicContainer.updateOptionalPluginAvailability(
                Bukkit.getPluginManager().isPluginEnabled("EliteMobs"),
                Bukkit.getPluginManager().isPluginEnabled("MythicMobs"));
        SchematicContainer.shutdown();
        Schematic.shutdown();
        NewChunkLoadEvent.prepareForContentReload();
        ChunkPregenerator.shutdown();
        Bukkit.getServer().getScheduler().cancelTasks(MetadataHandler.PLUGIN);
        BSPackage.shutdown();
        ModulesContainer.shutdown();
        WFCGenerator.shutdown();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                ConfigurationImporter importer = MagmaCore.initializeImporter(this);
                if (importer != null && importer.isEliteMobsContentImported())
                    EliteMobs.reloadAfterContentImport();
                new TreasureConfig();
                new GeneratorConfig();
                new ModuleGeneratorsConfig();
                new SpawnPoolsConfig();
                new SchematicConfig();
                new ModulesConfig();
                new ContentPackageConfig();
                BSPackageRefresher.reset();
                ComponentsConfigFolder.initialize();

                Bukkit.getScheduler().runTask(this, () -> finishImportedContentReload(true));
            } catch (Exception exception) {
                Logger.warn("Failed to reload BetterStructures content asynchronously.");
                exception.printStackTrace();
                Bukkit.getScheduler().runTask(this, () -> finishImportedContentReload(false));
            }
        });
    }

    private void finishImportedContentReload(boolean successful) {
        String resultMessage = successful
                ? "Reloaded BetterStructures content."
                : "&cFailed to reload BetterStructures content. Check the console.";
        for (CommandSender sender : activeReloadSenders) {
            Logger.sendMessage(sender, resultMessage);
        }
        activeReloadSenders.clear();
        contentReloadInProgress = false;

        if (!successful) {
            queuedReloadSenders.clear();
            NewChunkLoadEvent.discardDeferredNewChunks();
            // Every content registry is intentionally cleared before the
            // worker rebuilds it. Continuing to run after a failed rebuild
            // would leave a deceptively enabled plugin with partial or empty
            // content, so fail closed and make the defect visible.
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (queuedReloadSenders.isEmpty()) {
            NewChunkLoadEvent.replayDeferredNewChunks();
            return;
        }

        // A command arriving during a reload can accompany a file change made
        // after the first worker started. Run one more complete reload instead
        // of merely attaching its sender to the first result. All overlapping
        // requests are coalesced into that one follow-up, keeping the static
        // registries single-writer while still observing the newest files.
        activeReloadSenders.addAll(queuedReloadSenders);
        queuedReloadSenders.clear();
        startImportedContentReload();
    }

    public static boolean isReloading() {
        return MetadataHandler.PLUGIN instanceof BetterStructures plugin && plugin.contentReloadInProgress;
    }

    public static List<BSPackage> availablePackages() {
        if (isReloading()) return List.of();
        return new ArrayList<>(BSPackage.getBsPackages().values());
    }

    /**
     * Rejects commands that read or mutate content registries while the
     * asynchronous reload worker owns those registries.
     */
    public static boolean rejectContentCommandDuringReload(
            CommandSender sender) {
        if (!isReloading()) return false;
        if (sender != null) {
            Logger.sendMessage(
                    sender,
                    "&eBetterStructures content is reloading. Try that command again when the reload finishes.");
        }
        return true;
    }
}
