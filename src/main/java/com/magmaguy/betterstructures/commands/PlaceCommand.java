package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceCommand extends AdvancedCommand {
    public PlaceCommand() {
        super(List.of("place"));
        ArrayList<String> loadedSchematics = new ArrayList<>();
        SchematicContainer.getSchematics().values().forEach(schematicContainer -> loadedSchematics.add(schematicContainer.getClipboardFilename()));
        addArgument("schematic", new ListStringCommandArgument(loadedSchematics, "<schematic>"));
        addArgument("type", new ListStringCommandArgument(List.of(GeneratorConfigFields.StructureType.SURFACE.toString(), GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW.toString(), GeneratorConfigFields.StructureType.UNDERGROUND_DEEP.toString(), GeneratorConfigFields.StructureType.SKY.toString(), GeneratorConfigFields.StructureType.LIQUID_SURFACE.toString()),"<type>"));
        setPermission("betterstructures.*");
        setDescription("Allows players to place structures.");
        setSenderType(SenderType.PLAYER);
        setUsage("/betterstructures place <schematic> <SURFACE/SKY/LIQUID_SURFACE/UNDERGROUND_DEEP/UNDERGROUND_SHALLOW>");
    }

    @Override
    public void execute(CommandData commandData) {
        placeSchematic(commandData.getStringArgument("schematic"), commandData.getStringArgument("type"), commandData.getPlayerSender());
    }

    private void placeSchematic(String schematicFile, String schematicType, Player player) {
        try {
            SchematicContainer commandSchematicContainer = null;
            for (SchematicContainer schematicContainer : SchematicContainer.getSchematics().values())
                if (schematicContainer.getClipboardFilename().equals(schematicFile)) {
                    commandSchematicContainer = schematicContainer;
                    break;
                }
            if (commandSchematicContainer == null) {
                player.sendMessage("[BetterStructures] Invalid schematic!");
                return;
            }
            GeneratorConfigFields.StructureType structureType;
            try {
                structureType = GeneratorConfigFields.StructureType.valueOf(schematicType);
            } catch (Exception exception) {
                player.sendMessage("[BetterStructures] Failed to get valid schematic type!");
                return;
            }
            FitAnything.commandBasedCreation(player.getLocation().getChunk(), structureType, commandSchematicContainer);
            player.sendMessage("[BetterStructures] Attempted to place " + schematicFile + " !");
        } catch (Exception ex) {
            player.sendMessage("[BetterStructures] Invalid schematic!");
        }
    }
}
