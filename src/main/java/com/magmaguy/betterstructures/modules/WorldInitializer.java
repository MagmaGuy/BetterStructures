package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.TemporaryWorldManager;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

public class WorldInitializer {

    public static World generateWorld(String worldName, Player player) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(World.Environment.NORMAL);
        worldCreator.keepSpawnInMemory(false);
        // MagmaCore's void generator lives outside this plugin's class loader, so a world that
        // outlasts a restart-free update does not keep the replaced copy of BetterStructures loaded.
        worldCreator.generator(TemporaryWorldManager.voidGenerator());
        World world = worldCreator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Bukkit failed to create world " + worldName);
        }
        world.setAutoSave(false);
        if (player != null) player.setGameMode(GameMode.SPECTATOR);
        return world;
    }
}
