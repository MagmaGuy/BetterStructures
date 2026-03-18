package com.magmaguy.betterstructures.content;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.commands.ReloadCommand;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.magmacore.nightbreak.AbstractNightbreakContentPackage;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BSPackage extends AbstractNightbreakContentPackage {
    @Getter
    private static final Map<String, BSPackage> bsPackages = new HashMap<>();
    @Getter
    private final ContentPackageConfigFields contentPackageConfigFields;

    public BSPackage(ContentPackageConfigFields contentPackageConfigFields) {
        this.contentPackageConfigFields = contentPackageConfigFields;
        bsPackages.put(contentPackageConfigFields.getFilename(), this);
    }

    public static void shutdown() {
        bsPackages.clear();
    }

    @Override
    protected void doInstall(Player player) {
        player.closeInventory();
        File folder = getSpecificContentFolder();
        if (!folder.exists()) {
            Logger.sendMessage(player, "Failed to find directory " + folder.getAbsolutePath());
            return;
        }

        handleStateSave(player,
                toggleContentState(true),
                () -> {
                    ReloadCommand.reload(player);
                    if (player.isOnline()) {
                        Logger.sendMessage(player, "Installed " + contentPackageConfigFields.getName());
                    }
                },
                "&cFailed to update BetterStructures package state. Check the console.");
    }

    @Override
    public void doUninstall(Player player) {
        player.closeInventory();
        File folder = getSpecificContentFolder();
        if (!folder.exists()) {
            Logger.sendMessage(player, "Failed to find directory " + folder.getAbsolutePath());
            return;
        }

        handleStateSave(player,
                toggleContentState(false),
                () -> {
                    ReloadCommand.reload(player);
                    if (player.isOnline()) {
                        Logger.sendMessage(player, "Uninstalled " + contentPackageConfigFields.getName());
                    }
                },
                "&cFailed to update BetterStructures package state. Check the console.");
    }

    private CompletableFuture<Void> toggleContentState(boolean enabled) {
        if (contentPackageConfigFields.getContentPackageType() != ContentPackageConfigFields.ContentPackageType.MODULAR) {
            File folder = getSpecificContentFolder();
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().endsWith(".yml")) continue;
                    SchematicConfigField schematicConfigField = SchematicConfig.getSchematicConfiguration(file.getName());
                    if (schematicConfigField != null) schematicConfigField.toggleEnabled(enabled);
                }
            }
        }

        return contentPackageConfigFields.setEnabledAndSave(enabled);
    }

    private File getSpecificContentFolder() {
        String baseFolder = contentPackageConfigFields.getContentPackageType() == ContentPackageConfigFields.ContentPackageType.MODULAR
                ? "modules"
                : "schematics";
        return new File(MetadataHandler.PLUGIN.getDataFolder(), baseFolder + File.separatorChar + contentPackageConfigFields.getFolderName());
    }

    @Override
    protected JavaPlugin getOwnerPlugin() {
        return (JavaPlugin) MetadataHandler.PLUGIN;
    }

    @Override
    protected String getPluginDisplayName() {
        return "BetterStructures";
    }

    @Override
    protected String getContentPageUrl() {
        return "https://nightbreak.io/plugin/betterstructures/";
    }

    @Override
    protected List<String> getPackageDescription() {
        return contentPackageConfigFields.getDescription();
    }

    @Override
    protected String getManualImportsFolderName() {
        return "BetterStructures imports";
    }

    @Override
    protected String getManualReloadCommand() {
        return "/bs reload";
    }

    @Override
    protected void onDownloadStateSaved(Player player) {
        if (player.isOnline()) {
            Logger.sendSimpleMessage(player, "&aReloading BetterStructures so the new content is picked up...");
        }
        ReloadCommand.reload(player);
    }

    @Override
    public String getNightbreakSlug() {
        return contentPackageConfigFields.getNightbreakSlug();
    }

    @Override
    public String getDisplayName() {
        return contentPackageConfigFields.getName();
    }

    @Override
    public String getDownloadLink() {
        return contentPackageConfigFields.getDownloadLink();
    }

    @Override
    public int getLocalVersion() {
        return contentPackageConfigFields.getVersion();
    }

    @Override
    public CompletableFuture<Void> enableAfterDownload() {
        return contentPackageConfigFields.setEnabledAndSave(true);
    }

    @Override
    public boolean isInstalled() {
        return isDownloaded() && contentPackageConfigFields.isEnabled();
    }

    @Override
    public boolean isDownloaded() {
        return getSpecificContentFolder().exists();
    }
}
