package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.menus.BetterStructuresSetupMenu;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;

import java.util.List;

public class SetupCommand extends AdvancedCommand {
    public SetupCommand() {
        super(List.of("setup"));
        setPermission("betterstructures.setup");
        setSenderType(SenderType.PLAYER);
        setDescription("The main command for setting up BetterStructures!");
        setUsage("/bs setup");
    }

    @Override
    public void execute(CommandData commandData) {
        BetterStructuresSetupMenu.createMenu(commandData.getPlayerSender());
    }
}
