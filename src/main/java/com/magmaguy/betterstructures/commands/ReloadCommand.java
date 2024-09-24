package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;

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
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        Logger.sendMessage(commandData.getCommandSender(), "Reload attempted. This may not 100% work. Restart instead if it didn't!");
    }
}
