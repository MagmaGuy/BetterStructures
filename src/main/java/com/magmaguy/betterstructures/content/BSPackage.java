package com.magmaguy.betterstructures.content;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.commands.ReloadCommand;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.magmacore.menus.ContentPackage;
import com.magmaguy.magmacore.nightbreak.NightbreakAccount;
import com.magmaguy.magmacore.nightbreak.NightbreakContentManager;
import com.magmaguy.magmacore.nightbreak.NightbreakManagedContent;
import com.magmaguy.magmacore.nightbreak.NightbreakSetupMenuHelper;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BSPackage extends ContentPackage implements NightbreakManagedContent {

    @Getter
    private static final Map<String, BSPackage> bsPackages = new HashMap<>();
    @Getter
    private final ContentPackageConfigFields contentPackageConfigFields;
    @Getter
    @Setter
    private boolean outOfDate = false;
    @Getter
    @Setter
    private NightbreakAccount.AccessInfo cachedAccessInfo = null;

    public BSPackage(ContentPackageConfigFields contentPackageConfigFields) {
        super();
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

        toggleContentState(true);
        ReloadCommand.reload(player);
        Logger.sendMessage(player, "Installed " + contentPackageConfigFields.getName());
    }

    @Override
    public void doUninstall(Player player) {
        player.closeInventory();
        File folder = getSpecificContentFolder();
        if (!folder.exists()) {
            Logger.sendMessage(player, "Failed to find directory " + folder.getAbsolutePath());
            return;
        }

        toggleContentState(false);
        ReloadCommand.reload(player);
        Logger.sendMessage(player, "Uninstalled " + contentPackageConfigFields.getName());
    }

    @Override
    public void doDownload(Player player) {
        player.closeInventory();
        String slug = getNightbreakSlug();
        if (slug == null || slug.isEmpty()) {
            NightbreakSetupMenuHelper.sendManualDownloadMessage(player, contentPackageConfigFields.getDownloadLink(),
                    "BetterStructures imports", "/bs reload");
            return;
        }

        if (!NightbreakAccount.hasToken()) {
            NightbreakSetupMenuHelper.sendNoTokenPrompt(player, "BetterStructures", "https://nightbreak.io/plugin/betterstructures/");
            return;
        }

        Logger.sendSimpleMessage(player, "&aChecking Nightbreak access for &2" + contentPackageConfigFields.getName() + "&a...");
        JavaPlugin plugin = (JavaPlugin) MetadataHandler.PLUGIN;
        NightbreakContentManager.checkAccessAsync(plugin, slug, accessInfo -> {
            cachedAccessInfo = accessInfo;
            if (!player.isOnline()) return;

            if (accessInfo == null) {
                Logger.sendSimpleMessage(player, "&cFailed to contact Nightbreak for access information.");
                return;
            }

            if (!accessInfo.hasAccess) {
                doShowAccessInfo(player);
                return;
            }

            File importsFolder = new File(plugin.getDataFolder(), "imports");
            if (!importsFolder.exists()) importsFolder.mkdirs();

            NightbreakContentManager.downloadAsync(plugin, slug, importsFolder, player, success -> {
                if (!player.isOnline()) return;
                if (!success) {
                    return;
                }

                Logger.sendSimpleMessage(player, "&aReloading BetterStructures so the new content is picked up...");
                ReloadCommand.reload(player);
            });
        });
    }

    @Override
    protected void doShowAccessInfo(Player player) {
        NightbreakSetupMenuHelper.sendAccessInfo(player, contentPackageConfigFields.getName(), cachedAccessInfo,
                "https://nightbreak.io/plugin/betterstructures/");
    }

    @Override
    protected ItemStack getInstalledItemStack() {
        return NightbreakSetupMenuHelper.createInstalledItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription());
    }

    @Override
    protected ItemStack getPartiallyInstalledItemStack() {
        return NightbreakSetupMenuHelper.createPartiallyInstalledItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription());
    }

    @Override
    protected ItemStack getNotInstalledItemStack() {
        return NightbreakSetupMenuHelper.createNotInstalledItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription());
    }

    @Override
    protected ItemStack getNotDownloadedItemStack() {
        return NightbreakSetupMenuHelper.createNotDownloadedItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription(),
                getNightbreakSlug(),
                cachedAccessInfo);
    }

    @Override
    protected ItemStack getNeedsAccessItemStack() {
        return NightbreakSetupMenuHelper.createNeedsAccessItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription(),
                cachedAccessInfo);
    }

    @Override
    protected ItemStack getOutOfDateUpdatableItemStack() {
        return NightbreakSetupMenuHelper.createOutOfDateUpdatableItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription(),
                getNightbreakSlug());
    }

    @Override
    protected ItemStack getOutOfDateNoAccessItemStack() {
        return NightbreakSetupMenuHelper.createOutOfDateNoAccessItem(contentPackageConfigFields.getName(),
                contentPackageConfigFields.getDescription());
    }

    @Override
    protected ContentState getContentState() {
        boolean downloaded = isDownloaded();
        boolean installed = isInstalled();
        boolean hasNightbreakSlug = getNightbreakSlug() != null && !getNightbreakSlug().isEmpty();

        if (installed && outOfDate) {
            if (hasNightbreakSlug && NightbreakAccount.hasToken() && cachedAccessInfo != null && !cachedAccessInfo.hasAccess) {
                return ContentState.OUT_OF_DATE_NO_ACCESS;
            }
            return ContentState.OUT_OF_DATE_UPDATABLE;
        }

        if (installed) return ContentState.INSTALLED;
        if (downloaded) return ContentState.NOT_INSTALLED;

        if (hasNightbreakSlug && NightbreakAccount.hasToken() && cachedAccessInfo != null && !cachedAccessInfo.hasAccess) {
            return ContentState.NEEDS_ACCESS;
        }

        return ContentState.NOT_DOWNLOADED;
    }

    private void toggleContentState(boolean enabled) {
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

        contentPackageConfigFields.setEnabledAndSave(enabled);
    }

    private File getSpecificContentFolder() {
        String baseFolder = contentPackageConfigFields.getContentPackageType() == ContentPackageConfigFields.ContentPackageType.MODULAR
                ? "modules"
                : "schematics";
        return new File(MetadataHandler.PLUGIN.getDataFolder(), baseFolder + File.separatorChar + contentPackageConfigFields.getFolderName());
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
    public boolean isInstalled() {
        return isDownloaded() && contentPackageConfigFields.isEnabled();
    }

    @Override
    public boolean isDownloaded() {
        return getSpecificContentFolder().exists();
    }
}
