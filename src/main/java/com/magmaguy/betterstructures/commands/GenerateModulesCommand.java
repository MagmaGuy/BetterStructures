package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.BetterStructures;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfig;
import com.magmaguy.betterstructures.config.modulegenerators.ModuleGeneratorsConfigFields;
import com.magmaguy.betterstructures.modules.WFCGenerator;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.DynamicListStringCommandArgument;
import com.magmaguy.magmacore.util.Logger;

import java.util.List;

public class GenerateModulesCommand extends AdvancedCommand {
    public GenerateModulesCommand() {
        super(List.of("generateModules"));
        setUsage("/bs generateModules <ModuleGeneratorsConfigFile.yml>");
        addArgument("moduleGeneratorsConfigFile", new DynamicListStringCommandArgument(
                () -> BetterStructures.isReloading()
                        ? List.of()
                        : ModuleGeneratorsConfig.getModuleGenerators().keySet().stream().toList(),
                "<module.yml>"));
        setPermission("betterstructures.generatemodules");
        setDescription("Generates modular builds in a dedicated world, based on the generator's configuration file.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        if (BetterStructures.rejectContentCommandDuringReload(
                commandData.getCommandSender())) return;
        ModuleGeneratorsConfigFields moduleGeneratorsConfigFields = ModuleGeneratorsConfig.getModuleGenerators().get(commandData.getStringArgument("moduleGeneratorsConfigFile"));
        if (moduleGeneratorsConfigFields == null) {
            Logger.sendMessage(commandData.getCommandSender(), "File " + commandData.getStringArgument("moduleGeneratorsConfigFile") + " not found! The world won't generate.");
            return;
        }
        WFCGenerator.generateFromConfig(
                moduleGeneratorsConfigFields,
                commandData.getPlayerSender());
    }
}
