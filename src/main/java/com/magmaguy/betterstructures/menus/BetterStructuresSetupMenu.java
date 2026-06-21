package com.magmaguy.betterstructures.menus;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.BetterStructures;
import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;
import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.betterstructures.content.BSPackageRefresher;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.menus.SetupMenuBuilder;
import com.magmaguy.magmacore.nightbreak.DownloadAllContentPackage;
import com.magmaguy.magmacore.nightbreak.NightbreakAccount;
import com.magmaguy.magmacore.nightbreak.NightbreakSetupControls;
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

        MenuButton infoButton = NightbreakSetupControls.setupInfoButton(
                BetterStructures.NIGHTBREAK_PLUGIN_SPEC,
                "https://nightbreak.io/plugin/betterstructures/#setup");

        SetupMenuBuilder builder = new SetupMenuBuilder((JavaPlugin) MetadataHandler.PLUGIN, player)
                .title("Setup menu")
                .infoButton(infoButton)
                .packages(bsPackages)
                .appendPackage(new DownloadAllContentPackage<>(() -> new ArrayList<>(BSPackage.getBsPackages().values()),
                        "BetterStructures",
                        "https://nightbreak.io/plugin/betterstructures/",
                        "bs downloadall"))
                .addFilter(Material.GRASS_BLOCK, "Structure Packs",
                        (Predicate<BSPackage>) BetterStructuresSetupMenu::filterStructures)
                .addFilter(Material.DEEPSLATE_BRICKS, "Module Packs",
                        (Predicate<BSPackage>) BetterStructuresSetupMenu::filterModules);
        NightbreakSetupControls.prependStandardControls(builder, (JavaPlugin) MetadataHandler.PLUGIN, BetterStructures.NIGHTBREAK_PLUGIN_SPEC)
                .open();
    }

    private static boolean filterStructures(BSPackage bsPackage) {
        return bsPackage.getContentPackageConfigFields().getContentPackageType() ==
                ContentPackageConfigFields.ContentPackageType.STRUCTURE;
    }

    private static boolean filterModules(BSPackage bsPackage) {
        return bsPackage.getContentPackageConfigFields().getContentPackageType() ==
                ContentPackageConfigFields.ContentPackageType.MODULAR;
    }
}
