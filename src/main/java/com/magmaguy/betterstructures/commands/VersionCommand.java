package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;

import java.util.List;

public class VersionCommand extends AdvancedCommand {
    public VersionCommand() {
        super(List.of("version"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures version");
        setDescription("Shows the version of the plugin");
    }

    @Override
    public void execute(CommandData commandData) {
        Logger.sendMessage(commandData.getCommandSender(), "&aVersion " +
                Bukkit.getPluginManager().getPlugin(
                        MetadataHandler.PLUGIN.getName()).getDescription().getVersion());
    }
}
