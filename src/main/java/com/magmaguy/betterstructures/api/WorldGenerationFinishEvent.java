package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.config.modules.ModularWorld;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.concurrent.ThreadLocalRandom;

public class WorldGenerationFinishEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final ModularWorld modularWorld;


    public WorldGenerationFinishEvent(ModularWorld modularWorld) {
        this.modularWorld = modularWorld;
        Vector3i vector3i = modularWorld.getSpawnLocations().get(ThreadLocalRandom.current().nextInt(0, modularWorld.getSpawnLocations().size()));
        modularWorld.getModularGenerationStatus().getGeneratingPlayer().teleport(new Location(modularWorld.getWorld(), vector3i.x, vector3i.y, vector3i.z));
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}