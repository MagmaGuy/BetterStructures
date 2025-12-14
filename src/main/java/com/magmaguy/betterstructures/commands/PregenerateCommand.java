package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.util.ChunkPregenerator;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.IntegerCommandArgument;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public class PregenerateCommand extends AdvancedCommand {
    public PregenerateCommand() {
        super(List.of("pregenerate"));
        addArgument("center", new ListStringCommandArgument(List.of("HERE", "WORLD_CENTER", "WORLD_SPAWN"), "Center of the generation"));
        addArgument("shape", new ListStringCommandArgument(List.of("SQUARE", "CIRCLE"), "Shape of the generation"));
        addArgument("radius", new IntegerCommandArgument("Radius to generate"));
        addArgument("setWorldBorder", new ListStringCommandArgument(List.of("TRUE", "FALSE"), "Set a world border at the end?"));
        setUsage("/betterstructures pregenerate <centerType> <shape> <radius> <applyWorldBorder>");
        setPermission("betterstructures.*");
        setDescription("Pregenerates chunks from a center point outward in either a square or circle pattern up to the specified radius.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        String centerArg = commandData.getStringArgument("center");
        String shape = commandData.getStringArgument("shape");
        int radius = commandData.getIntegerArgument("radius");
        String setWorldBorderArg = commandData.getStringArgument("setWorldBorder");

        if (radius < 0) {
            Logger.sendMessage(commandData.getCommandSender(), "&cRadius must be 0 or greater.");
            return;
        }

        World world = commandData.getPlayerSender().getWorld();
        Location center;

        // Determine center location based on argument
        switch (centerArg.toUpperCase()) {
            case "HERE":
                center = commandData.getPlayerSender().getLocation();
                break;
            case "WORLD_CENTER":
                center = new Location(world, 0, world.getHighestBlockYAt(0, 0), 0);
                break;
            case "WORLD_SPAWN":
                center = world.getSpawnLocation();
                break;
            default:
                Logger.sendMessage(commandData.getCommandSender(), "&cInvalid center argument. Use HERE, WORLD_CENTER, or WORLD_SPAWN.");
                return;
        }

        boolean setWorldBorder = "TRUE".equalsIgnoreCase(setWorldBorderArg);

        if (!"SQUARE".equalsIgnoreCase(shape) && !"CIRCLE".equalsIgnoreCase(shape)) {
            Logger.sendMessage(commandData.getCommandSender(), "&cInvalid shape. Use SQUARE or CIRCLE.");
            return;
        }

        Logger.sendMessage(commandData.getCommandSender(), "&2Starting chunk pregeneration with shape: " + shape + ", center: " + centerArg + ", radius: " + radius);
        if (setWorldBorder) {
            Logger.sendMessage(commandData.getCommandSender(), "&2World border will be set to match the generated area.");
        }
        Logger.sendMessage(commandData.getCommandSender(), "&7Progress will be reported in the console every 30 seconds.");
        Logger.sendMessage(commandData.getCommandSender(), "&7Use &2/betterstructures cancelPregenerate &7to cancel if needed.");

        ChunkPregenerator pregenerator = new ChunkPregenerator(world, center, shape, radius, setWorldBorder);
        pregenerator.start();
    }
}

