package com.magmaguy.betterstructures.menus;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.commands.ReloadCommand;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.schematics.SchematicConfig;
import com.magmaguy.magmacore.menus.FirstTimeSetupMenu;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BetterStructuresFirstTimeSetupMenu {
    public static void createMenu(Player player) {
        new FirstTimeSetupMenu(
                (JavaPlugin) MetadataHandler.PLUGIN,
                player,
                "&2BetterStructures",
                "&6Guided content setup",
                createInfoItem(),
                List.of(createRecommendedItem(), createManualItem(), createSkipItem()));
    }

    private static MenuButton createInfoItem() {
        return new MenuButton(ItemStackGenerator.generateSkullItemStack(
                "magmaguy",
                "&2Welcome to BetterStructures!",
                List.of(
                        "&7Connect this server,",
                        "&7open the setup menu,",
                        "&7and start generating structures quickly."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                sendLink(player, "&2Setup guide: ", "&9&nhttps://nightbreak.io/plugin/betterstructures/#setup",
                        "&7Click to open the BetterStructures setup guide.",
                        "https://nightbreak.io/plugin/betterstructures/#setup");
                sendLink(player, "&2Account token: ", "&9&nhttps://nightbreak.io/account/",
                        "&7Click to open the account token page.",
                        "https://nightbreak.io/account/");
                sendCommand(player, "&2Setup menu: ", "&a/bs setup",
                        "&7Click to open the BetterStructures setup menu.",
                        "/bs setup");
                sendCommand(player, "&2Recommended plugins: ", "&a/bs recommendedplugins",
                        "&7Click to see plugins that work well with BetterStructures.",
                        "/bs recommendedplugins");
                sendLink(player, "&2Support Discord: ", "&9&nhttps://discord.gg/eSxvPbWYy4",
                        "&7Click to open the BetterStructures support Discord.",
                        "https://discord.gg/eSxvPbWYy4");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

    private static MenuButton createRecommendedItem() {
        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.GREEN_STAINED_GLASS_PANE,
                "&2Recommended Setup",
                List.of("&aMarks setup complete.", "&aGuides you to setup and recommended plugins."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&aBetterStructures setup is now marked as complete.");
                Logger.sendSimpleMessage(player, "&7Connect this server so BetterStructures can install content and download plugin updates from in-game.");
                sendLink(player, "&7Step 1: get your account token at ",
                        "&9&nhttps://nightbreak.io/account/",
                        "&7Click to open the account token page.",
                        "https://nightbreak.io/account/");
                sendCommand(player, "&7Step 2: link it in-game with ", "&a/nightbreaklogin <token>",
                        "&7Click to prepare the token command.",
                        "/nightbreaklogin ");
                player.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&7Step 3: open the setup menu with "),
                        SpigotMessage.commandHoverMessage("&a/bs setup",
                                "&7Click to open the BetterStructures setup menu.",
                                "/bs setup"));
                sendCommand(player, "&7Recommended plugins: ", "&a/bs recommendedplugins",
                        "&7Click to see plugins that work well with BetterStructures.",
                        "/bs recommendedplugins");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

    private static MenuButton createManualItem() {
        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.YELLOW_STAINED_GLASS_PANE,
                "&6Manual Setup",
                List.of("&eMarks setup complete.", "&eLeaves content management up to you."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&6Setup complete. You can manage content manually through &a/bs setup&6.");
                Logger.sendSimpleMessage(player, "&7If you already dropped new files into &8plugins/BetterStructures/imports&7, run &a/bs reload&7.");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

    private static MenuButton createSkipItem() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit") &&
                !Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            return new MenuButton(ItemStackGenerator.generateItemStack(
                    Material.RED_STAINED_GLASS_PANE,
                    "&cWorldEdit not installed",
                    List.of("&cBetterStructures needs WorldEdit or FAWE installed."))) {
                @Override
                public void onClick(Player player) {
                    player.closeInventory();
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    Logger.sendSimpleMessage(player, "&cInstall WorldEdit or FAWE before using BetterStructures.");
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                }
            };
        }

        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.RED_STAINED_GLASS_PANE,
                "&cUse Current Content",
                List.of("&cDismisses the setup prompt and keeps your current content state."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                if (SchematicConfig.getSchematicConfigurations().isEmpty()) {
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    Logger.sendSimpleMessage(player, "&eSetup dismissed, but no BetterStructures content is currently installed.");
                    Logger.sendSimpleMessage(player, "&7Use &a/bs setup &7when you're ready to install content.");
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                } else {
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    Logger.sendSimpleMessage(player, "&aSetup complete. BetterStructures will keep using your current installed content.");
                    Logger.sendSimpleMessage(player, "&7Run &a/bs reload &7if you import new content later.");
                    Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                    ReloadCommand.reload(player);
                }
            }
        };
    }

    private static void sendLink(Player player, String prefix, String display, String hover, String url) {
        player.spigot().sendMessage(
                SpigotMessage.simpleMessage(prefix),
                SpigotMessage.hoverLinkMessage(display, hover, url));
    }

    private static void sendCommand(Player player, String prefix, String display, String hover, String command) {
        player.spigot().sendMessage(
                SpigotMessage.simpleMessage(prefix),
                SpigotMessage.commandHoverMessage(display, hover, command));
    }
}
