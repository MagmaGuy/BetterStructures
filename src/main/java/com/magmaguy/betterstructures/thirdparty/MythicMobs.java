package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.betterstructures.util.ChatColorConverter;
import com.magmaguy.betterstructures.util.WarningMessage;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Support for MythicMobs, configuration format is "MobID[:level]"
 *
 * @author CarmJos
 */
public class MythicMobs {

    public static boolean Spawn(Location location, String filename) {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("betterstructures.*")) {
                    player.sendMessage(ChatColorConverter.convert(
                            "&8[BetterStructures] &cOne of your packs uses the MythicMobs plugin &4but MythicMobs is not currently installed on your server&c!" +
                                    " &2You can download it here: &9https://www.spigotmc.org/resources/%E2%9A%94-mythicmobs-free-version-%E2%96%BAthe-1-custom-mob-creator%E2%97%84.5702/"));
                }
            }
            return false;
        }

        String[] args = filename.split(":");


        MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(args[0]).orElse(null);
        if (mob == null) {
            new WarningMessage("Failed to spawn regional boss " + args[0] + "! The filename for this boss probably does not match the mob that should be in ~/plugins/MythicMobs/Mobs/");
            return false;
        }

        double level;
        try {
            level = Double.parseDouble(args[1]);
        } catch (Exception e) {
            new WarningMessage("Failed to parse level for mob " + filename + "!");
            return false;
        }
        mob.spawn(BukkitAdapter.adapt(location), Math.max(1, level));
        return true;

    }

}
