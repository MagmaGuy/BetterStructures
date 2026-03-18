package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginBootstrap;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand extends AdvancedCommand {
    public ReloadCommand() {
        super(List.of("reload"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures reload");
        setDescription("Reloads the plugin.");
    }

    @Override
    public void execute(CommandData commandData) {
        reload(commandData.getCommandSender());
    }

    public static void reload(CommandSender commandSender) {
        NightbreakPluginBootstrap.setPendingReloadSender((org.bukkit.plugin.java.JavaPlugin) MetadataHandler.PLUGIN, commandSender);
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
    }
}
