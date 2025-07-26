package com.magmaguy.betterstructures.modules;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

public class WorldInitializer {

    public static World generateWorld(String worldName, Player player) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(World.Environment.NORMAL);
        worldCreator.keepSpawnInMemory(false);
        worldCreator.generator(new VoidGenerator());
        World world = worldCreator.createWorld();
        world.setAutoSave(false);
//        player.teleport(new Location(world, 8, 16, 8));
        player.setGameMode(GameMode.SPECTATOR);
        return world;
    }

    private static class VoidGenerator extends ChunkGenerator {
    }
}
