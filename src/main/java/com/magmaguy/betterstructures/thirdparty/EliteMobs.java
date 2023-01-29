package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.betterstructures.util.ChatColorConverter;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.magmaguy.elitemobs.commands.admin.ReloadCommand;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
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
                new WarningMessage("Failed to spawn regional boss " + filename + "! The filename for this boss probably does not match the filename that should be in ~/plugins/EliteMobs/custombosses/");
                return false;
            } else {
                regionalBossEntity.spawn(false);
                return true;
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.hasPermission("betterstructures.*"))
                    player.sendMessage(ChatColorConverter.convert(
                            "&8[BetterStructures] &cOne of your packs uses the EliteMobs plugin &4but EliteMobs is not currently installed on your server&c!" +
                                    " &2You can download it here: &9https://www.spigotmc.org/resources/%E2%9A%94elitemobs%E2%9A%94.40090/"));
            return false;
        }
    }

    public static void Reload() {
        ReloadCommand.reload(Bukkit.getConsoleSender());
    }
}
