package com.magmaguy.betterstructures.util;

import com.magmaguy.betterstructures.MetadataHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class VersionChecker {
    private static boolean pluginIsUpToDate = true;

    public static void checkPluginVersion() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String currentVersion = MetadataHandler.PLUGIN.getDescription().getVersion();
                boolean snapshot = false;
                if (currentVersion.contains("SNAPSHOT")) {
                    snapshot = true;
                    currentVersion = currentVersion.split("-")[0];
                }
                String publicVersion = "";

                try {
                    Bukkit.getLogger().info("[BetterStructures] Latest public release is " + VersionChecker.readStringFromURL("https://api.spigotmc.org/legacy/update.php?resource=103241"));
                    Bukkit.getLogger().info("[BetterStructures] Your version is " + MetadataHandler.PLUGIN.getDescription().getVersion());
                    publicVersion = VersionChecker.readStringFromURL("https://api.spigotmc.org/legacy/update.php?resource=103241");
                } catch (IOException e) {
                    Bukkit.getLogger().warning("[BetterStructures] Couldn't check latest version");
                    return;
                }

                if (Double.parseDouble(currentVersion.split("\\.")[0]) < Double.parseDouble(publicVersion.split("\\.")[0])) {
                    outOfDateHandler();
                    return;
                }

                if (Double.parseDouble(currentVersion.split("\\.")[0]) == Double.parseDouble(publicVersion.split("\\.")[0])) {

                    if (Double.parseDouble(currentVersion.split("\\.")[1]) < Double.parseDouble(publicVersion.split("\\.")[1])) {
                        outOfDateHandler();
                        return;
                    }

                    if (Double.parseDouble(currentVersion.split("\\.")[1]) == Double.parseDouble(publicVersion.split("\\.")[1])) {
                        if (Double.parseDouble(currentVersion.split("\\.")[2]) < Double.parseDouble(publicVersion.split("\\.")[2])) {
                            outOfDateHandler();
                            return;
                        }
                    }
                }

                if (!snapshot)
                    Bukkit.getLogger().info("[BetterStructures] You are running the latest version!");
                else
                    new InfoMessage("You are running a snapshot version! You can check for updates in the #releases channel on the BetterStructures Discord!");

                pluginIsUpToDate = true;
            }
        }.runTaskAsynchronously(MetadataHandler.PLUGIN);
    }

    private static String readStringFromURL(String url) throws IOException {

        try (Scanner scanner = new Scanner(new URL(url).openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

    }

    private static void outOfDateHandler() {

        new WarningMessage("[BetterStructures] A newer version of this plugin is available for download!");
        pluginIsUpToDate = false;

    }

    public static class VersionCheckerEvents implements Listener {
        @EventHandler
        public void onPlayerLogin(PlayerJoinEvent event) {

            if (!event.getPlayer().hasPermission("betterstructures.versionnotification")) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!event.getPlayer().isOnline()) return;
                    if (!pluginIsUpToDate)
                        event.getPlayer().sendMessage(ChatColorConverter.convert("&a[BetterStructures] &cYour version of BetterStructures is outdated." +
                                " &aYou can download the latest version from &3&n&ohttps://www.spigotmc.org/resources/betterstructures.103241/"));
                }
            }.runTaskLater(MetadataHandler.PLUGIN, 20L * 3);

        }
    }

}
