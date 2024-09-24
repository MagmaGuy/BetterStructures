package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class SilentCommand extends AdvancedCommand {
    public SilentCommand() {
        super(List.of("silent"));
        setUsage("/betterstructures silent");
        setDescription("Silences the warnings about structures appearing for admins.");
    }

    @Override
    public void execute(CommandData commandData) {
        DefaultConfig.toggleWarnings();
        Logger.sendMessage(commandData.getCommandSender(), "&2Toggled build warnings to " + DefaultConfig.isNewBuildingWarn() + "!");
    }
}
