package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;
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
        MetadataHandler.pendingReloadSender = commandSender;
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
    }
}
