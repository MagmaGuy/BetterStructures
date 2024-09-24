package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.menus.BetterStructuresFirstTimeSetupMenu;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;

import java.util.List;

public class FirstTimeSetupCommand extends AdvancedCommand {
    public FirstTimeSetupCommand() {
        super(List.of("initialize"));
        setUsage("/bs initialize");
        setPermission("betterstructures.initialize");
        setDescription("Does the first time setup of the plugin.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        BetterStructuresFirstTimeSetupMenu.createMenu(commandData.getPlayerSender());
    }
}
