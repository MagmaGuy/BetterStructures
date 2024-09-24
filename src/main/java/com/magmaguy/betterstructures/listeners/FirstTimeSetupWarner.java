package com.magmaguy.betterstructures.listeners;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.util.Logger;
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
                Logger.sendSimpleMessage(event.getPlayer(), "&7Welcome to BetterStructures!" +
                        " &c&lIt looks like have not have set up BetterStructures yet! &2To install BetterStructures, do &a/betterstructures initialize &2!");
                Logger.sendSimpleMessage(event.getPlayer(), "&7You can get support over at &9&nhttps://discord.gg/9f5QSka");
                Logger.sendSimpleMessage(event.getPlayer(), "&cPick an option in /betterstructures setup to permanently dismiss this message!");
                Logger.sendSimpleMessage(event.getPlayer(), "&8&m----------------------------------------------------");
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 10);
    }
}
