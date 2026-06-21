package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.nightbreak.NightbreakAccount;
import com.magmaguy.magmacore.nightbreak.NightbreakContentManager;
import com.magmaguy.magmacore.nightbreak.NightbreakSetupMenuHelper;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadAllContentCommand extends AdvancedCommand {
    static final AtomicBoolean IS_BULK_DOWNLOADING = new AtomicBoolean(false);

    public DownloadAllContentCommand() {
        super(List.of("downloadallcontent"));
        setPermission("betterstructures.setup");
        setSenderType(SenderType.ANY);
        setDescription("Downloads all available BetterStructures content.");
        setUsage("/bs downloadallcontent");
    }

    @Override
    public void execute(CommandData commandData) {
        execute(commandData.getCommandSender(), false);
    }

    public static void execute(CommandSender sender, boolean updatesOnly) {
        if (!NightbreakAccount.hasToken()) {
            Logger.sendSimpleMessage(sender, "&cConnect this server first with &a/nightbreaklogin <token>&c.");
            return;
        }
        if (NightbreakAccount.hasAuthFailure()) {
            NightbreakSetupMenuHelper.sendTokenUpdatePrompt(sender, "BetterStructures");
            return;
        }

        if (!IS_BULK_DOWNLOADING.compareAndSet(false, true)) {
            Logger.sendSimpleMessage(sender, "&eA BetterStructures bulk content operation is already running.");
            return;
        }

        List<BSPackage> packagesToDownload = collectPackages(updatesOnly);

        if (packagesToDownload.isEmpty()) {
            IS_BULK_DOWNLOADING.set(false);
            Logger.sendSimpleMessage(sender, updatesOnly
                    ? "&aAll BetterStructures content is already up to date."
                    : "&aAll BetterStructures content is already downloaded and up to date.");
            return;
        }

        Logger.sendSimpleMessage(sender, (updatesOnly
                ? "&aFound &2" + packagesToDownload.size() + "&a BetterStructures package update(s)."
                : "&aFound &2" + packagesToDownload.size() + "&a BetterStructures content package(s) to download or update."));
        Player player = sender instanceof Player playerSender ? playerSender : null;
        File importsFolder = new File(MetadataHandler.PLUGIN.getDataFolder(), "imports");
        if (!importsFolder.exists()) importsFolder.mkdirs();

        downloadNext((JavaPlugin) MetadataHandler.PLUGIN, packagesToDownload, 0, importsFolder, sender, player,
                new AtomicInteger(), new AtomicInteger(), new ArrayList<>(), updatesOnly);
    }

    static List<BSPackage> collectPackages(boolean updatesOnly) {
        List<BSPackage> packagesToDownload = new ArrayList<>();
        Set<String> seenSlugs = new HashSet<>();
        for (BSPackage bsPackage : BSPackage.getBsPackages().values()) {
            String slug = bsPackage.getNightbreakSlug();
            if (slug == null || slug.isEmpty()) continue;
            if (!seenSlugs.add(slug)) continue;
            if (updatesOnly) {
                if (bsPackage.isOutOfDate()
                        && (bsPackage.getCachedAccessInfo() == null || bsPackage.getCachedAccessInfo().hasAccess)) {
                    packagesToDownload.add(bsPackage);
                }
            } else if ((!bsPackage.isDownloaded() || bsPackage.isOutOfDate())
                    && (bsPackage.getCachedAccessInfo() == null || bsPackage.getCachedAccessInfo().hasAccess)) {
                packagesToDownload.add(bsPackage);
            }
        }
        return packagesToDownload;
    }

    private static void downloadNext(JavaPlugin plugin,
                                     List<BSPackage> packages,
                                     int index,
                                     File importsFolder,
                                     CommandSender sender,
                                     Player player,
                                     AtomicInteger completed,
                                     AtomicInteger failed,
                                     List<String> failedNames,
                                     boolean updatesOnly) {
        if (player != null && !player.isOnline()) {
            if (index >= packages.size()) {
                IS_BULK_DOWNLOADING.set(false);
                if (completed.get() > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> ReloadCommand.reload(Bukkit.getConsoleSender()), 20L);
                }
                return;
            }
            BSPackage bsPackage = packages.get(index);
            NightbreakContentManager.downloadAsync(plugin, bsPackage.getNightbreakSlug(), importsFolder, null, success -> {
                if (success) {
                    completed.incrementAndGet();
                } else {
                    if (abortIfAuthFailure(sender, player)) return;
                    failed.incrementAndGet();
                    failedNames.add(bsPackage.getDisplayName());
                }
                downloadNext(plugin, packages, index + 1, importsFolder, sender, player, completed, failed, failedNames, updatesOnly);
            });
            return;
        }

        if (index >= packages.size()) {
            IS_BULK_DOWNLOADING.set(false);
            Logger.sendSimpleMessage(sender, (updatesOnly
                    ? "&aBetterStructures updates finished. Updated &2" + completed.get() + "&a, failed &c" + failed.get() + "&a."
                    : "&aBetterStructures bulk download finished. Downloaded &2" + completed.get() + "&a, failed &c" + failed.get() + "&a."));
            if (!failedNames.isEmpty()) {
                Logger.sendSimpleMessage(sender, "&cFailed packages: " + String.join(", ", failedNames));
            }
            if (completed.get() > 0) {
                Logger.sendSimpleMessage(sender, "&aReloading BetterStructures to pick up the new content...");
                Bukkit.getScheduler().runTaskLater(plugin, () -> ReloadCommand.reload(sender), 20L);
            }
            return;
        }

        BSPackage bsPackage = packages.get(index);
        Logger.sendSimpleMessage(sender, (updatesOnly
                ? "&2Updating (&a" + (index + 1) + "&2/&a" + packages.size() + "&2) &a" + bsPackage.getDisplayName() + "&2..."
                : "&2Downloading (&a" + (index + 1) + "&2/&a" + packages.size() + "&2) &a" + bsPackage.getDisplayName() + "&2..."));
        NightbreakContentManager.downloadAsync(plugin, bsPackage.getNightbreakSlug(), importsFolder, null, success -> {
            if (success) {
                completed.incrementAndGet();
                if (player == null || player.isOnline()) {
                    int remaining = packages.size() - (index + 1);
                    Logger.sendSimpleMessage(sender, remaining > 0
                            ? "&aDownloaded " + bsPackage.getDisplayName() + "&a. &2" + remaining + "&a package(s) remaining..."
                            : "&aDownloaded " + bsPackage.getDisplayName() + "&a.");
                }
            } else {
                if (abortIfAuthFailure(sender, player)) return;
                failed.incrementAndGet();
                failedNames.add(bsPackage.getDisplayName());
                if (player == null || player.isOnline()) {
                    Logger.sendSimpleMessage(sender, "&cFailed to download " + bsPackage.getDisplayName() + "&c.");
                }
            }
            downloadNext(plugin, packages, index + 1, importsFolder, sender, player, completed, failed, failedNames, updatesOnly);
        });
    }

    private static boolean abortIfAuthFailure(CommandSender sender, Player player) {
        if (!NightbreakAccount.hasAuthFailure()) return false;
        IS_BULK_DOWNLOADING.set(false);
        CommandSender target = player != null && !player.isOnline() ? Bukkit.getConsoleSender() : sender;
        NightbreakSetupMenuHelper.sendTokenUpdatePrompt(target, "BetterStructures");
        return true;
    }
}
