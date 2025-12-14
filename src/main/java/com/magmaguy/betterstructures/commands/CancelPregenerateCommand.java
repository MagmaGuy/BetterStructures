package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.util.ChunkPregenerator;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.World;

import java.util.List;

public class CancelPregenerateCommand extends AdvancedCommand {
    public CancelPregenerateCommand() {
        super(List.of("cancelPregenerate", "cancelpregenerate"));
        setUsage("/betterstructures cancelPregenerate");
        setPermission("betterstructures.*");
        setDescription("Cancels active chunk pregeneration in your current world.");
        setSenderType(SenderType.PLAYER);
    }

    @Override
    public void execute(CommandData commandData) {
        World world = commandData.getPlayerSender().getWorld();
        
        // Find active pregenerators in the player's world
        List<ChunkPregenerator> worldPregenerators = ChunkPregenerator.activePregenerators.stream()
                .filter(p -> p.getWorld().equals(world))
                .toList();

        if (worldPregenerators.isEmpty()) {
            Logger.sendMessage(commandData.getCommandSender(), "&cNo active pregeneration found in world: " + world.getName());
            return;
        }

        // Cancel all active pregenerators in this world
        int cancelled = 0;
        for (ChunkPregenerator pregenerator : worldPregenerators) {
            pregenerator.cancel();
            cancelled++;
        }

        Logger.sendMessage(commandData.getCommandSender(), "&2Cancelled " + cancelled + " pregeneration process(es) in world: " + world.getName());
    }
}

