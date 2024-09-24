package com.magmaguy.betterstructures.menus;

import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.menus.SetupMenu;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

        MenuButton infoButton = new MenuButton(ItemStackGenerator.generateSkullItemStack("magmaguy",
                "&2Installation instructions:",
                List.of(
                        "&2To setup the optional/recommended content for BetterStructures:",
                        "&61) &fDownload content from &9https://nightbreak.io/plugin/betterstructures",
                        "&62) &fPut the content in the &2imports &ffolder of BetterStructures",
                        "&63) &fDo &2/bs reload",
                        "&2That's it!",
                        "&6Click for more info and links!"))) {
            @Override
            public void onClick(Player p) {
                p.closeInventory();
                Logger.sendSimpleMessage(p, "&8&l&m&o---------------------------------------------");
                Logger.sendSimpleMessage(p, "&6&lBetterStructures installation resources:");
                Logger.sendSimpleMessage(p, "&2&lWiki page: &9&nhttps://magmaguy.com/wiki.html");
                Logger.sendSimpleMessage(p, "&2&lVideo setup guide: &9&nhttps://www.youtube.com/watch?v=1z47lSxmyq0");
                Logger.sendSimpleMessage(p, "&2&lContent download links: &9&nhttps://nightbreak.io/plugin/betterstructures/");
                Logger.sendSimpleMessage(p, "&2&lDiscord support: &9&nhttps://discord.gg/9f5QSka");
                Logger.sendSimpleMessage(p, "&8&l&m&o---------------------------------------------");
            }
        };

        new SetupMenu(player, infoButton, bsPackages, new ArrayList<>());
    }
}
