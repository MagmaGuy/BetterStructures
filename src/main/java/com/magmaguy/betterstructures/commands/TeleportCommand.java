package com.magmaguy.betterstructures.commands;

import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand extends AdvancedCommand {
    public TeleportCommand() {
        super(List.of("teleport", "tp"));
        addArgument("world", new ArrayList<>());
        addArgument("x", new ArrayList<>());
        addArgument("y", new ArrayList<>());
        addArgument("z", new ArrayList<>());
        setUsage("/teleport <worldname> <x> <y> <z>");
        setPermission("betterstructures.*");
        setDescription("Teleports a player to specific coordinates.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        try {
            World world = Bukkit.getWorld(commandData.getStringArgument("world"));
            double x = Double.parseDouble(commandData.getStringArgument("x"));
            double y = Double.parseDouble(commandData.getStringArgument("y"));
            double z = Double.parseDouble(commandData.getStringArgument("z"));
            commandData.getPlayerSender().teleport(new Location(world, x, y, z));
        } catch (Exception ex) {
            Logger.sendMessage(commandData.getCommandSender(), "Failed to teleport to location because the location wasn't valid!");
        }
    }
}
