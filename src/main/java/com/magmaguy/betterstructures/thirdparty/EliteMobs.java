package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.elitemobs.commands.ReloadCommand;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class EliteMobs {
    /**
     * Spawns a 1-time regional boss at the set location
     *
     * @param location Location where the boss should spawn
     * @param filename Filename of the boss, as set in the EliteMobs custombosses configuration folder
     */
    public static boolean Spawn(Location location, String filename) {
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            RegionalBossEntity regionalBossEntity = RegionalBossEntity.SpawnRegionalBoss(filename, location);
            if (regionalBossEntity == null) {
                Logger.warn("Failed to spawn regional boss " + filename + "! The filename for this boss probably does not match the filename that should be in ~/plugins/EliteMobs/custombosses/");
                return false;
            } else {
                regionalBossEntity.spawn(false);
                return true;
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.hasPermission("betterstructures.*"))
                    Logger.sendMessage(player, "&cOne of your packs uses the EliteMobs plugin &4but EliteMobs is not currently installed on your server&c!" +
                            " &2You can download it here: &9https://nightbreak.io/plugin/elitemobs/");
            return false;
        }
    }

    /**
     * Reloads EliteMobs after a content import deposited files into its data folder.
     * EliteMobs only reads its config folders on boot/reload, so without this the
     * imported shrine bosses stay unregistered until a manual /em reload and shrines
     * paste without bosses.
     */
    public static void reloadAfterContentImport() {
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) return;
        Bukkit.getScheduler().runTask(MetadataHandler.PLUGIN, () -> {
            if (!Bukkit.getPluginManager().isPluginEnabled("EliteMobs")) return;
            Logger.info("EliteMobs content was imported - reloading EliteMobs so the new content registers.");
            ReloadCommand.reload(Bukkit.getConsoleSender());
        });
    }
}
