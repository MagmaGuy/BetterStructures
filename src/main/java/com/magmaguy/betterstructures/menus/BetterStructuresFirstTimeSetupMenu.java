package com.magmaguy.betterstructures.menus;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class BetterStructuresFirstTimeSetupMenu {
    public static void createMenu(Player player) {
        new com.magmaguy.magmacore.menus.FirstTimeSetupMenu(
                player,
                "&2BetterStructures",
                "&6Add custom structures to your server!",
                createInfoItem(),
                List.of(createGettingStartedItem()));
    }

    private static MenuButton createInfoItem() {
        return new MenuButton(ItemStackGenerator.generateSkullItemStack(
                "magmaguy",
                "&2Welcome to BetterStructures!",
                List.of(
                        "&9Click to get a link to the full setup guide!",
                        "&2You can find a basic checklist below to get started!"))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&2See the full setup here: &9&nhttps://nightbreak.io/plugin/betterstructures/#setup");
                Logger.sendSimpleMessage(player, "&2Check the available content through &6/bs setup &2!");
                Logger.sendSimpleMessage(player, "&2Support & discussion Discord: &9&nhttps://discord.gg/eSxvPbWYy4");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

    private static MenuButton createGettingStartedItem() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit") &&
                !Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            return new MenuButton(ItemStackGenerator.generateItemStack(
                    Material.RED_STAINED_GLASS_PANE,
                    "&cWorldEdit not installed!",
                    List.of("&cYou must install WorldEdit for",
                            "&cBetterStructures to work!"))) {
                @Override
                public void onClick(Player player) {
                    player.closeInventory();
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    Logger.sendSimpleMessage(player, "&c&lYou must install WorldEdit for BetterStructures to work!");
                    Logger.sendSimpleMessage(player, "&c&You can download it here: &9&nhttps://dev.bukkit.org/projects/worldedit");
                    Logger.sendSimpleMessage(player, "&4&lMake sure you get the right WorldEdit version for your Minecraft version!");
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                }
            };
        }

        if (SchematicConfig.getSchematicConfigurations().isEmpty()) {
            return new MenuButton(ItemStackGenerator.generateItemStack(
                    Material.YELLOW_STAINED_GLASS_PANE,
                    "&cNo content installed!",
                    List.of("&cCould not detect any structures installed",
                            "&cfor BetterStructures! Click for more",
                            "&cinformation!"))) {
                @Override
                public void onClick(Player player) {
                    player.closeInventory();
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    Logger.sendSimpleMessage(player, "&c&lBetterStructures requires either downloading or creating builds to work!");
                    Logger.sendSimpleMessage(player, "&cYou can download builds here: &9&nhttps://nightbreak.io/plugin/betterstructures/#content");
                    Logger.sendSimpleMessage(player, "&cOnce downloaded, just drag and drop it into the imports folder of BetterStructures and &4/bs reload&c. Setup video: &9&https://www.youtube.com/watch?v=1z47lSxmyq0");
                    Logger.sendSimpleMessage(player, "&4You can also just make your own content! Check the wiki for more information! &9&nhttps://magmaguy.com/wiki.html");
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                }
            };
        }

        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.GREEN_STAINED_GLASS_PANE,
                "&2Seems like everything is ready to go!",
                List.of("&aClick here to complete the first time setup!"))) {
            @Override
            public void onClick(Player player) {
                DefaultConfig.toggleSetupDone();
                player.closeInventory();
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&2Congratulations! Seems like your server is ready to start generating better structures!");
                Logger.sendSimpleMessage(player, "&aTo see the content currently installed, run the command &a/betterstructures setup");
                Logger.sendSimpleMessage(player, "&aTo generate structures, move to new chunks in your server! These must be completely new, never previously generated chunks. BetterStructures will never generate structures in already explored chunks!");
                Logger.sendSimpleMessage(player, "&aThat's it! Have fun exploring! The first time setup message will never show up again.");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

}
