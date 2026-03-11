package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class FirstTimeSetupWarner implements Listener {
    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (DefaultConfig.isSetupDone()) return;
        if (!event.getPlayer().hasPermission("betterstructures.*")) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.getPlayer().isOnline()) return;
                Logger.sendSimpleMessage(event.getPlayer(), "&8&m----------------------------------------------------");
                Logger.sendMessage(event.getPlayer(), "&fInitial setup message:");
                Logger.sendSimpleMessage(event.getPlayer(), "&7Welcome to BetterStructures! &c&lIt looks like you have not set up BetterStructures yet!");
                event.getPlayer().spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2To install BetterStructures, click here: "),
                        SpigotMessage.commandHoverMessage("&a/betterstructures initialize",
                                "&7Click to open the BetterStructures first-time setup menu.",
                                "/betterstructures initialize"));
                event.getPlayer().spigot().sendMessage(
                        SpigotMessage.simpleMessage("&7You can get support over at "),
                        SpigotMessage.hoverLinkMessage("&9&nhttps://discord.gg/9f5QSka",
                                "&7Click to open the BetterStructures support Discord.",
                                "https://discord.gg/9f5QSka"));
                event.getPlayer().spigot().sendMessage(
                        SpigotMessage.simpleMessage("&cPick an option in "),
                        SpigotMessage.commandHoverMessage("&a/betterstructures setup",
                                "&7Click to open the BetterStructures setup menu.",
                                "/betterstructures setup"),
                        SpigotMessage.simpleMessage(" &cto permanently dismiss this message!"));
                Logger.sendSimpleMessage(event.getPlayer(), "&8&m----------------------------------------------------");
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 10);
    }
}
