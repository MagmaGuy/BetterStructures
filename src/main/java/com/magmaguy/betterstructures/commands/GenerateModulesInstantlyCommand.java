package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class GenerateModulesInstantlyCommand extends AdvancedCommand {
    public GenerateModulesInstantlyCommand() {
        super(List.of("generateModulesInstantly"));
        addArgument("worldName", new ArrayList<>());
        addArgument("radius", new ArrayList<>());
        addArgument("debug" , new ArrayList<>(List.of("true", "false")));
        addArgument("startingModule", ModulesContainer.getModulesContainers().keySet().stream().toList());
        setUsage("/bs generateModulesInstantly <worldName> <radius> <debug> <startingModule>");
        setPermission("betterstructures.generatemodules.instantly");
        setDescription("Generates modular builds in a dedicated world, instantly.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        if (commandData.getIntegerArgument("radius") > 80 && Runtime.getRuntime().maxMemory() <= 4L * 1024 * 1024 * 1024) {
            Logger.sendMessage(commandData.getCommandSender(),
                    "You do not have enough RAM for a radius above 80, you will definitely want more than 4GB of RAM for that. Consider pregenerating it locally on a computer that has more RAM and then putting the world in your server!");
            return;
        }
        new WaveFunctionCollapseGenerator(
                commandData.getStringArgument("worldName"),
                commandData.getIntegerArgument("radius"),
                Boolean.valueOf(commandData.getStringArgument("debug")),
                commandData.getPlayerSender(),
                commandData.getStringArgument("startingModule"));
    }
}
