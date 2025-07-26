package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.config.modules.WaveFunctionCollapseGenerator;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class GenerateModulesCommand extends AdvancedCommand {
    public GenerateModulesCommand() {
        super(List.of("generateModules"));
        setUsage("/bs generateModules <ModuleGeneratorsConfigFile.yml>");
        addArgument("moduleGeneratorsConfigFile", new ListStringCommandArgument(ModuleGeneratorsConfig.getModuleGenerators().keySet().stream().toList(), "<module.yml>"));
        setPermission("betterstructures.generatemodules");
        setDescription("Generates modular builds in a dedicated world, based on the generator's configuration file.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
//        if (commandData.getIntegerArgument("radius") > 80 && Runtime.getRuntime().maxMemory() <= 4L * 1024 * 1024 * 1024) {
//            Logger.sendMessage(commandData.getCommandSender(),
//                    "You do not have enough RAM for a radius above 80, you will definitely want more than 4GB of RAM for that. Consider pregenerating it locally on a computer that has more RAM and then putting the world in your server!");
//            return;
//        }
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = ModuleGeneratorsConfig.getModuleGenerators().get(commandData.getStringArgument("moduleGeneratorsConfigFile"));
        if (moduleGeneratorsConfigFields == null) {
            Logger.sendMessage(commandData.getCommandSender(), "File " + commandData.getStringArgument("moduleGeneratorsConfigFile") + " not found! The world won't generate.");
            return;
        }
        WaveFunctionCollapseGenerator.generateFromConfig(
                moduleGeneratorsConfigFields,
                commandData.getPlayerSender());
    }
}
