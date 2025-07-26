package com.magmaguy.betterstructures.commands;

import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;

import java.util.ArrayList;

public class BetterStructuresCommand extends AdvancedCommand {
    public BetterStructuresCommand() {
        super(new ArrayList<>());
        setUsage("/bs");
        setPermission("betterstructures.*");
        setDescription("A basic help command for BetterStructures.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        Logger.sendMessage(commandData.getCommandSender(), "BetterStructures is a plugin that adds random structures to your Minecraft world!");
        Logger.sendMessage(commandData.getCommandSender(), "You can check the structures you have and download structures in the &2/betterstructures setup &fcommand.");
        Logger.sendMessage(commandData.getCommandSender(), "Once a pack is installed, structures will automatically generate in freshly generated chunks. You do not have to run any commands for this to happen.");
        Logger.sendMessage(commandData.getCommandSender(), "By default, OPs will get notified about new structures generating until they disable these messages.");
    }
}
