package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.betterstructures.modules.ModulesContainer;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;

import java.util.ArrayList;
import java.util.List;

public class GenerateInfiniteCommand extends AdvancedCommand {
    public GenerateInfiniteCommand() {
        super(List.of("generateInfinite"));
        addArgument("worldName", new ArrayList<>());
        addArgument("radius", new ArrayList<>());
        addArgument("speed", new ArrayList<>());
        addArgument("startingModule", ModulesContainer.getModulesContainers().keySet().stream().toList());
        setUsage("/bs generateInfinite <worldName> <radius> <speed> <startingModule>");
        setPermission("betterstructures.generateInfinite");
        setDescription("Generates builds.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        new WaveFunctionCollapseGenerator(commandData.getStringArgument("worldName"), commandData.getIntegerArgument("radius"), commandData.getIntegerArgument("speed"), commandData.getPlayerSender(), commandData.getStringArgument("startingModule"));
    }
}
