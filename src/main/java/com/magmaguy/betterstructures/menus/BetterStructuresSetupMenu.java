package com.magmaguy.betterstructures.menus;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.betterstructures.content.BSPackageRefresher;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.menus.SetupMenu;
import com.magmaguy.magmacore.nightbreak.NightbreakAccount;
import com.magmaguy.magmacore.nightbreak.DownloadAllContentPackage;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BetterStructuresSetupMenu {
    private BetterStructuresSetupMenu() {
    }

    public static void createMenu(Player player) {
        List<BSPackage> rawBsPackages = new ArrayList<>(BSPackage.getBsPackages().values());
        List<BSPackage> bsPackages = rawBsPackages.stream()
                .sorted(Comparator.comparing(pkg ->
                        ChatColor.stripColor(ChatColorConverter.convert(pkg.getContentPackageConfigFields().getName()))))
                .collect(Collectors.toList());
        BSPackageRefresher.refreshContentAndAccess();

        MenuButton infoButton = new MenuButton(ItemStackGenerator.generateSkullItemStack("magmaguy",
                "&2Installation instructions:",
                List.of(
                        "&2To setup the optional/recommended content for BetterStructures:",
                        "&61) &fLink your Nightbreak account: &a/nightbreaklogin",
                        "&62) &fDownload all content: &a/bs downloadall",
                        "&63) &fOr browse and manage content: &a/bs setup",
                        "&2That's it!",
                        "&6Click for more info and links!"))) {
            @Override
            public void onClick(Player p) {
                p.closeInventory();
                Logger.sendSimpleMessage(p, "<g:#8B0000:#CC4400:#DAA520>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</g>");
                Logger.sendSimpleMessage(p, "&6&lBetterStructures installation resources:");
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lNightbreak account: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://nightbreak.io/account/",
                                "&7Click to open the Nightbreak account page.",
                                "https://nightbreak.io/account/"));
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lWiki page: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://nightbreak.io/plugin/betterstructures/#setup",
                                "&7Click to open the BetterStructures setup page.",
                                "https://nightbreak.io/plugin/betterstructures/#setup"));
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lContent: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://nightbreak.io/plugin/betterstructures/",
                                "&7Click to browse BetterStructures content.",
                                "https://nightbreak.io/plugin/betterstructures/"));
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lDiscord support: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://discord.gg/9f5QSka",
                                "&7Click to open Discord.",
                                "https://discord.gg/9f5QSka"));
                if (NightbreakAccount.hasToken()) {
                    p.spigot().sendMessage(
                            SpigotMessage.commandHoverMessage("&2&lQuick install: &a/bs downloadall",
                                    "&7Click to run the bulk BetterStructures download.",
                                    "/bs downloadall"));
                    p.spigot().sendMessage(
                            SpigotMessage.commandHoverMessage("&2&lQuick update: &a/bs updatecontent",
                                    "&7Click to update all outdated BetterStructures content.",
                                    "/bs updatecontent"));
                }
                Logger.sendSimpleMessage(p, "<g:#8B0000:#CC4400:#DAA520>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</g>");
            }
        };

        List<com.magmaguy.magmacore.menus.ContentPackage> allPackages = new ArrayList<>(bsPackages);
        allPackages.add(new DownloadAllContentPackage<>(() -> new ArrayList<>(BSPackage.getBsPackages().values()),
                "BetterStructures",
                "https://nightbreak.io/plugin/betterstructures/",
                "bs downloadall"));

        new SetupMenu((JavaPlugin) MetadataHandler.PLUGIN, player, infoButton, allPackages,
                List.of(
                        createFilter(bsPackages, Material.GRASS_BLOCK, "Structure Packs", bspPackage ->
                                bspPackage.getContentPackageConfigFields().getContentPackageType() == com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields.ContentPackageType.STRUCTURE),
                        createFilter(bsPackages, Material.DEEPSLATE_BRICKS, "Module Packs", bspPackage ->
                                bspPackage.getContentPackageConfigFields().getContentPackageType() == com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields.ContentPackageType.MODULAR)),
                "Setup menu");
    }

    private static SetupMenu.SetupMenuFilter createFilter(List<BSPackage> orderedPackages,
                                                          Material material,
                                                          String name,
                                                          Predicate<BSPackage> predicate) {
        return new SetupMenu.SetupMenuFilter(
                ItemStackGenerator.generateItemStack(material, name),
                orderedPackages.stream().filter(predicate).toList());
    }
}
